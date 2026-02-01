package com.crabmod.hotbath.mixin.twilightforest;

import com.crabmod.hotbath.compat.CompatManager;
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
        
        // Update flask contents
        hotbath$updateFlaskContents(flaskStack, flaskContents, customPotionContents);
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
        
        // Update flask contents
        hotbath$updateFlaskContents(flaskStack, flaskContents, customPotionContents);
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
     */
    @Unique
    private void hotbath$updateFlaskContents(ItemStack flaskStack, PotionFlaskComponent oldContents, 
            PotionContents newContents) {
        PotionFlaskComponent newFlaskContents = new PotionFlaskComponent(
                newContents,
                oldContents.doses() + 1,
                oldContents.breakage(),
                oldContents.breakable()
        );
        
        flaskStack.set(TFDataComponents.POTION_FLASK_CONTENTS, newFlaskContents);
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
     */
    @Unique
    private PotionContents hotbath$createPotionContentsFromLegacyBottle(Item bottleItem) {
        // Map legacy bath bottles to their colors and create effects
        // The actual effects will be applied when drinking from the flask
        
        if (bottleItem == ItemRegister.HOT_WATER_BOTTLE.get()) {
            return new PotionContents(Optional.empty(), Optional.of(0xE0FFFF), List.of());
        } else if (bottleItem == ItemRegister.HONEY_BATH_BOTTLE.get()) {
            return new PotionContents(Optional.empty(), Optional.of(0xFFB300), List.of());
        } else if (bottleItem == ItemRegister.MILK_BATH_BOTTLE.get()) {
            return new PotionContents(Optional.empty(), Optional.of(0xFFFAF0), List.of());
        } else if (bottleItem == ItemRegister.HERBAL_BATH_BOTTLE.get()) {
            return new PotionContents(Optional.empty(), Optional.of(0x2B8B57), List.of());
        } else if (bottleItem == ItemRegister.PEONY_BATH_BOTTLE.get()) {
            return new PotionContents(Optional.empty(), Optional.of(0xFFB6C1), List.of());
        } else if (bottleItem == ItemRegister.ROSE_BATH_BOTTLE.get()) {
            return new PotionContents(Optional.empty(), Optional.of(0xFF69B4), List.of());
        }
        
        return null;
    }
}
