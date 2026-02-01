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
import java.util.Optional;

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
            ItemStack emptyBottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(emptyBottle)) {
                player.drop(emptyBottle, false);
            }
        }
        
        return stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack;
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
            // List effects
            for (CustomFluidDefinition.EffectEntry effect : definition.effects()) {
                String effectName = effect.effect().toString();
                int amplifier = effect.amplifier() + 1; // Display level (1-based)
                int durationSeconds = effect.duration() / 20;
                
                tooltip.add(Component.translatable("tooltip.hotbath.effect_entry",
                        effectName, amplifier, durationSeconds)
                        .withStyle(ChatFormatting.BLUE));
            }
            
            if (definition.effects().isEmpty()) {
                tooltip.add(Component.translatable("tooltip.hotbath.no_effects")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
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
