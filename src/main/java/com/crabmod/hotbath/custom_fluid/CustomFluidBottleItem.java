package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.compat.*;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A drinkable bottle that can contain any custom fluid defined in data packs.
 * Applies effects from the fluid definition when consumed.
 * 
 * <p>The fluid type is stored in the item's NBT tags, allowing a single
 * item registration to represent all custom fluid bottles.</p>
 */
public class CustomFluidBottleItem extends Item {
    
    private static final int DRINK_DURATION = 32; // Same as potions
    
    public CustomFluidBottleItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
            
            // Apply effects from the fluid definition
            CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
            if (definition != null) {
                List<MobEffectInstance> effects = definition.createEffectInstances();
                for (MobEffectInstance effect : effects) {
                    serverPlayer.addEffect(new MobEffectInstance(
                            effect.getEffect(),
                            effect.getDuration(),
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.isVisible(),
                            effect.showIcon()
                    ));
                }
                
                // Apply temperature effects for mod compatibility (only if hot)
                applyTemperatureEffects(serverPlayer, definition);
            }
        }
        
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }

        return stack;
    }
    
    /**
     * Applies temperature effects for compatible mods.
     * Only applies warming effects if the fluid is defined as hot.
     */
    private void applyTemperatureEffects(Player player, CustomFluidDefinition definition) {
        // Apply thirst effects for all fluids (not just hot ones)
        applyThirstEffects(player, definition);
        
        // Only apply temperature effects if the fluid is hot
        if (!definition.isHot()) {
            return;
        }
        
        // Apply ToughAsNails temperature effect (warming only)
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            BathWaterBottleTANModifier.applyWarmEffect(player);
        }
        
        // Apply Cold Sweat temperature effect
        if (ColdSweatIntegration.isColdSweatLoaded()) {
            BathWaterBottleColdSweatModifier.applyWarmEffect(player);
        }
        
        // Apply Legendary Survival Overhaul temperature effect
        if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            BathWaterBottleLSOModifier.applyWarmEffect(player);
        }
    }
    
    /**
     * Applies thirst restoration effects for compatible mods.
     * This applies to all custom fluids regardless of temperature.
     */
    private void applyThirstEffects(Player player, CustomFluidDefinition definition) {
        // Apply ToughAsNails thirst restoration with custom thirst value
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            ToughAsNailsThirstHelper.restoreThirst(player, definition);
        }
        
        // LSO thirst is handled via LSOThirstHandler event
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return DRINK_DURATION;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Check if this bottle has a valid fluid
        if (!CustomFluidNBTHelper.hasFluidData(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
        if (definition != null) {
            String translatedFluidName = definition.getTranslatedName();
            Component fluidName = Component.literal(translatedFluidName);
            return Component.translatable("item.hotbath.custom_fluid_bottle", fluidName);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, 
            @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        
        CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
        if (definition != null) {
            tooltip.add(Component.translatable("item.hotbath.custom_fluid_bottle.desc")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

            // Show effects
            List<MobEffectInstance> effects = definition.createEffectInstances();
            for (MobEffectInstance effect : effects) {
                String effectName = effect.getEffect().getDescriptionId();
                int amplifier = effect.getAmplifier();
                int durationSeconds = effect.getDuration() / 20;

                String level_str = amplifier > 0 ? " " + toRoman(amplifier + 1) : "";
                tooltip.add(Component.translatable(effectName)
                        .append(level_str)
                        .append(Component.translatable("tooltip.hotbath.duration", durationSeconds))
                        .withStyle(effect.getEffect().isBeneficial() ? ChatFormatting.BLUE : ChatFormatting.RED));
            }
        }
    }

    private static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }

    /**
     * Creates a bottle item stack filled with the specified custom fluid.
     * 
     * @param fluidId The fluid ID to fill the bottle with
     * @return A new bottle item stack with the fluid data set
     */
    public ItemStack createFilledBottle(ResourceLocation fluidId) {
        ItemStack stack = new ItemStack(this);
        CustomFluidNBTHelper.setFluidId(stack, fluidId);
        return stack;
    }
}
