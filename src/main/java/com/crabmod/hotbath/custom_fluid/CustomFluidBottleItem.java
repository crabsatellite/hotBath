package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.compat.*;
import com.crabmod.hotbath.items.BathWaterEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A dynamic bottle item that can hold any custom fluid defined in data packs.
 * When consumed, it applies the effects defined in the fluid's JSON configuration.
 * 
 * <p>This bottle integrates with temperature mods (Cold Sweat, ToughAsNails, LSO)
 * to apply warmth effects when drunk.</p>
 */
public class CustomFluidBottleItem extends Item {
    
    public CustomFluidBottleItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }

        if (!level.isClientSide) {
            // Apply custom fluid effects
            CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
            if (definition != null) {
                applyDrinkEffects(entity, definition);
            }
        }

        // Return glass bottle
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
     * Applies the drinking effects from the custom fluid.
     */
    private void applyDrinkEffects(LivingEntity entity, CustomFluidDefinition definition) {
        // Apply all effects from the fluid definition
        List<MobEffectInstance> effects = definition.createEffectInstances();
        for (MobEffectInstance effect : effects) {
            entity.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }

        // Apply temperature effects for mod compatibility
        if (entity instanceof Player player) {
            applyTemperatureEffects(player, definition);
        }
    }

    /**
     * Applies temperature effects for compatible mods.
     * Only applies warming effects if the fluid is defined as hot.
     */
    private void applyTemperatureEffects(Player player, CustomFluidDefinition definition) {
        // Only apply temperature effects if the fluid is hot
        if (!definition.isHot()) {
            return;
        }
        
        // Apply ToughAsNails temperature effect
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            BathWaterBottleTANModifier.applyWarmEffect(player);
            ToughAsNailsThirstHelper.restoreThirst(player);
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

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 32;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
        if (definition != null) {
            // Use the dynamic translation system to get the translated fluid name
            String translatedFluidName = definition.getTranslatedName();
            Component fluidName = Component.literal(translatedFluidName);
            return Component.translatable("item.hotbath.custom_fluid_bottle", fluidName);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
        if (definition != null) {
            tooltipComponents.add(Component.translatable("item.hotbath.custom_fluid_bottle.desc")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            
            // Show effects
            List<MobEffectInstance> effects = definition.createEffectInstances();
            for (MobEffectInstance effect : effects) {
                String effectName = effect.getEffect().value().getDescriptionId();
                int amplifier = effect.getAmplifier();
                int durationSeconds = effect.getDuration() / 20;
                
                String level = amplifier > 0 ? " " + toRoman(amplifier + 1) : "";
                tooltipComponents.add(Component.translatable(effectName)
                        .append(level)
                        .append(" (" + durationSeconds + "s)")
                        .withStyle(effect.getEffect().value().isBeneficial() ? ChatFormatting.BLUE : ChatFormatting.RED));
            }
        }
    }

    /**
     * Converts a number to Roman numerals.
     */
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
     * Gets the custom fluid color for item rendering.
     */
    public static int getItemColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 0) { // Fluid layer
            int color = CustomFluidDataComponents.getFluidColor(stack);
            if (color != -1) {
                return color;
            }
        }
        return 0xFFFFFFFF;
    }
}
