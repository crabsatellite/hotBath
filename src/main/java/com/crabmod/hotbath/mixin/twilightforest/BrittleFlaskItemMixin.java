package com.crabmod.hotbath.mixin.twilightforest;

import com.crabmod.hotbath.compat.*;
import com.crabmod.hotbath.compat.twilightforest.TFFlaskColorHelper;
import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidBottleItem;
import com.crabmod.hotbath.custom_fluid.CustomFluidDataComponents;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.items.BathWaterBottleItem;
import com.crabmod.hotbath.registers.ItemRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.components.item.PotionFlaskComponent;
import twilightforest.init.TFDataComponents;
import twilightforest.init.TFSounds;
import twilightforest.item.BrittleFlaskItem;

import java.util.List;
import java.util.Optional;

/**
 * Mixin to allow HotBath custom fluid bottles and bath water bottles to be put into
 * Twilight Forest's Brittle Flask and Greater Flask.
 * 
 * This enables players to store multiple doses of HotBath liquids in the flasks,
 * similar to how vanilla potions work with the flask.
 * 
 * Supported bottle types:
 * - Custom Fluid Bottles (data-driven custom fluids)
 * - Legacy Bath Water Bottles (hot water, honey bath, milk bath, herbal bath, peony bath, rose bath)
 */
@Mixin(BrittleFlaskItem.class)
public class BrittleFlaskItemMixin {

    @Unique
    private static final int FLASK_MAX_DOSES = 3;

    /**
     * Inject at HEAD to intercept when a HotBath bottle is right-clicked onto a flask.
     * This allows custom fluid bottles and legacy bath water bottles to be added to the flask.
     */
    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void hotbath$handleHotBathBottles(ItemStack stack, ItemStack other, Slot slot, 
            ClickAction action, Player player, SlotAccess access, CallbackInfoReturnable<Boolean> cir) {
        
        if (action != ClickAction.SECONDARY) return;
        
        try {
            // Handle custom fluid bottles (data-driven)
            if (other.getItem() instanceof CustomFluidBottleItem) {
                if (hotbath$handleCustomFluidBottle(stack, other, player)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
            
            // Handle legacy bath water bottles
            if (other.getItem() instanceof BathWaterBottleItem) {
                if (hotbath$handleLegacyBathBottle(stack, other, player)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        } catch (Throwable e) {
            // Report error but don't crash - just let vanilla behavior take over
            CompatManager.reportRuntimeError("twilightforest", "BrittleFlaskItemMixin.handleHotBathBottles", e);
        }
    }

    /**
     * Override isBarVisible to also show bar for HotBath fluids (which use customColor instead of potion).
     */
    @Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
    private void hotbath$isBarVisibleForHotBath(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            PotionFlaskComponent flaskContents = stack.getOrDefault(
                    TFDataComponents.POTION_FLASK_CONTENTS, PotionFlaskComponent.EMPTY);
            
            // Show bar if flask has customColor (HotBath fluid) even without base potion
            if (flaskContents.potion().customColor().isPresent() && flaskContents.doses() > 0) {
                cir.setReturnValue(true);
            }
        } catch (Throwable e) {
            CompatManager.reportRuntimeError("twilightforest", "BrittleFlaskItemMixin.isBarVisible", e);
        }
    }

    /**
     * Override getBarColor to use customColor for HotBath fluids.
     */
    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void hotbath$getBarColorForHotBath(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        try {
            PotionFlaskComponent flaskContents = stack.getOrDefault(
                    TFDataComponents.POTION_FLASK_CONTENTS, PotionFlaskComponent.EMPTY);
            
            // Use customColor if present (HotBath fluid)
            if (flaskContents.potion().customColor().isPresent()) {
                // Return the color with full opacity
                int color = flaskContents.potion().customColor().get();
                cir.setReturnValue(color | 0xFF000000);
            }
        } catch (Throwable e) {
            CompatManager.reportRuntimeError("twilightforest", "BrittleFlaskItemMixin.getBarColor", e);
        }
    }

    /**
     * Inject into finishUsingItem to apply HotBath effects.
     * Directly calls BathWaterEffects methods to ensure identical behavior to drinking bottles directly.
     * For custom fluids, applies temperature effects if the fluid is hot.
     */
    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void hotbath$applyHotBathEffects(ItemStack stack, net.minecraft.world.level.Level level, 
            net.minecraft.world.entity.LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide()) return;
        
        try {
            PotionFlaskComponent flaskContents = stack.getOrDefault(
                    TFDataComponents.POTION_FLASK_CONTENTS, PotionFlaskComponent.EMPTY);
            
            // Check if this is a HotBath fluid (has customColor but no base potion)
            if (flaskContents.potion().customColor().isPresent() && flaskContents.potion().potion().isEmpty()) {
                int color = flaskContents.potion().customColor().get();
                
                // First try to apply legacy bath effect based on color
                // This calls the exact same methods as drinking the bottle directly
                boolean isLegacyBath = TFFlaskColorHelper.applyEffectByColor(entity, color);
                
                // If not a legacy bath, check if it's a custom fluid and apply temperature effects
                if (!isLegacyBath && entity instanceof Player player) {
                    Optional<CustomFluidDefinition> definitionOpt = CustomFluidAPI.getDefinitionByColor(color);
                    if (definitionOpt.isPresent()) {
                        CustomFluidDefinition definition = definitionOpt.get();
                        // Apply temperature effects only if the fluid is hot
                        hotbath$applyTemperatureEffects(player, definition);
                    }
                }
            }
        } catch (Throwable e) {
            CompatManager.reportRuntimeError("twilightforest", "BrittleFlaskItemMixin.finishUsingItem", e);
        }
    }

    /**
     * Applies temperature effects for compatible mods.
     * Only applies warming effects if the fluid is defined as hot.
     */
    @Unique
    private void hotbath$applyTemperatureEffects(Player player, CustomFluidDefinition definition) {
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

    /**
     * Handle adding a custom fluid bottle to the flask.
     */
    @Unique
    private boolean hotbath$handleCustomFluidBottle(ItemStack flaskStack, ItemStack bottleStack, Player player) {
        ResourceLocation fluidId = CustomFluidDataComponents.getFluidId(bottleStack);
        if (fluidId == null) {
            return false;
        }
        
        Optional<CustomFluidDefinition> definitionOpt = CustomFluidAPI.getFluidDefinition(fluidId);
        if (definitionOpt.isEmpty()) {
            return false;
        }
        
        CustomFluidDefinition definition = definitionOpt.get();
        
        // Get current flask contents
        PotionFlaskComponent flaskContents = flaskStack.getOrDefault(
                TFDataComponents.POTION_FLASK_CONTENTS, PotionFlaskComponent.EMPTY);
        
        // Create PotionContents from custom fluid effects
        PotionContents customPotionContents = hotbath$createPotionContents(definition);
        
        // Check if we can add to the flask
        if (!hotbath$canAddToFlask(flaskContents, customPotionContents)) return false;
        
        // Consume bottle and give back glass bottle
        if (!player.getAbilities().instabuild) {
            bottleStack.shrink(1);
            if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
        }
        
        // Update flask contents (handles stack separation)
        hotbath$updateFlaskContents(flaskStack, flaskContents, customPotionContents, player);
        player.playSound(TFSounds.FLASK_FILL.get(), (flaskContents.doses() + 1) * 0.25F, 
                player.level().getRandom().nextFloat() * 0.1F + 0.9F);
        
        return true;
    }

    /**
     * Handle adding a legacy bath water bottle to the flask.
     */
    @Unique
    private boolean hotbath$handleLegacyBathBottle(ItemStack flaskStack, ItemStack bottleStack, Player player) {
        // Get the bath type from the item
        Item bottleItem = bottleStack.getItem();
        
        // Create PotionContents from the bath water bottle
        PotionContents customPotionContents = hotbath$createPotionContentsFromLegacyBottle(bottleItem);
        if (customPotionContents == null) return false;
        
        // Get current flask contents
        PotionFlaskComponent flaskContents = flaskStack.getOrDefault(
                TFDataComponents.POTION_FLASK_CONTENTS, PotionFlaskComponent.EMPTY);
        
        // Check if we can add to the flask
        if (!hotbath$canAddToFlask(flaskContents, customPotionContents)) return false;
        
        // Consume bottle and give back glass bottle
        if (!player.getAbilities().instabuild) {
            bottleStack.shrink(1);
            if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
        }
        
        // Update flask contents (handles stack separation)
        hotbath$updateFlaskContents(flaskStack, flaskContents, customPotionContents, player);
        player.playSound(TFSounds.FLASK_FILL.get(), (flaskContents.doses() + 1) * 0.25F, 
                player.level().getRandom().nextFloat() * 0.1F + 0.9F);
        
        return true;
    }

    /**
     * Check if we can add the potion contents to the flask.
     */
    @Unique
    private boolean hotbath$canAddToFlask(PotionFlaskComponent flaskContents, PotionContents newContents) {
        // Check if flask is full
        if (flaskContents.doses() >= FLASK_MAX_DOSES - flaskContents.breakage()) {
            return false;
        }
        
        // Check if flask is empty (no potion AND no customColor means truly empty)
        boolean flaskEmpty = flaskContents.potion().potion().isEmpty() 
                && flaskContents.potion().customColor().isEmpty()
                && flaskContents.doses() == 0;
        
        if (flaskEmpty) {
            return true;
        }
        
        // Compare effects to see if they're the same type
        return hotbath$arePotionContentsEqual(flaskContents.potion(), newContents);
    }

    /**
     * Compare two PotionContents to see if they have the same effects.
     */
    @Unique
    private boolean hotbath$arePotionContentsEqual(PotionContents a, PotionContents b) {
        // Compare custom color (used to identify HotBath fluids)
        if (a.customColor().isPresent() && b.customColor().isPresent()) {
            return a.customColor().get().equals(b.customColor().get());
        }
        
        // If one has potion holder and other doesn't, they're different
        if (a.potion().isPresent() != b.potion().isPresent()) {
            return false;
        }
        
        // Compare potion holders if both exist
        if (a.potion().isPresent()) {
            return a.potion().get().equals(b.potion().get());
        }
        
        // Both have no potion holder, compare custom effects list
        List<MobEffectInstance> effectsA = a.customEffects();
        List<MobEffectInstance> effectsB = b.customEffects();
        
        if (effectsA.size() != effectsB.size()) return false;
        
        // Simple comparison by effect type (ignoring duration for stacking purposes)
        for (int i = 0; i < effectsA.size(); i++) {
            if (!effectsA.get(i).getEffect().equals(effectsB.get(i).getEffect())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Update the flask contents with a new dose.
     * Handles stack separation: if multiple flasks are stacked, separates one for filling.
     */
    @Unique
    private void hotbath$updateFlaskContents(ItemStack flaskStack, PotionFlaskComponent oldContents, 
            PotionContents newContents, Player player) {
        PotionFlaskComponent newFlaskContents = new PotionFlaskComponent(
                newContents,
                oldContents.doses() + 1,
                oldContents.breakage(),
                oldContents.breakable()
        );
        
        // Handle stack separation like vanilla TF does
        if (flaskStack.getCount() > 1) {
            // Create a copy with count 1
            ItemStack copy = flaskStack.copyWithCount(1);
            // Shrink the original stack
            flaskStack.shrink(1);
            // Apply the new contents to the copy
            copy.set(TFDataComponents.POTION_FLASK_CONTENTS, newFlaskContents);
            // Give the filled flask to player
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        } else {
            // Single flask, just update in place
            flaskStack.set(TFDataComponents.POTION_FLASK_CONTENTS, newFlaskContents);
        }
    }

    /**
     * Create PotionContents from a CustomFluidDefinition.
     */
    @Unique
    private PotionContents hotbath$createPotionContents(CustomFluidDefinition definition) {
        List<MobEffectInstance> effects = definition.createEffectInstances();
        int color = definition.color();
        return new PotionContents(
                Optional.empty(),  // No base potion
                Optional.of(color),  // Use fluid color
                effects  // Custom effects
        );
    }

    /**
     * Create PotionContents from a legacy bath water bottle item.
     * Uses TFFlaskColorHelper to get the correct color from FluidsColor.
     * Only stores the color - effects are applied via finishUsingItem calling BathWaterEffects directly.
     * This avoids duplicate effect application and ensures identical behavior to drinking bottles.
     */
    @Unique
    private PotionContents hotbath$createPotionContentsFromLegacyBottle(Item bottleItem) {
        // Use TFFlaskColorHelper to get the color for this bottle type
        int color = TFFlaskColorHelper.getColorForBottle(bottleItem);
        if (color != -1) {
            return new PotionContents(Optional.empty(), Optional.of(color), List.of());
        }
        return null;
    }
}
