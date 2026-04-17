package com.crabmod.hotbath.mixin.twilightforest;

import com.crabmod.hotbath.compat.*;
import com.crabmod.hotbath.compat.twilightforest.TFFlaskColorHelper;
import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidBottleItem;
import com.crabmod.hotbath.custom_fluid.CustomFluidNBTHelper;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.items.BathWaterBottleItem;
import com.crabmod.hotbath.registers.ItemRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.init.TFSounds;
import twilightforest.item.BrittleFlaskItem;

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
 * 
 * For 1.20.1 Forge version - uses NBT instead of DataComponents
 */
@SuppressWarnings({"NullableProblems", "DataFlowIssue"})
@Mixin(BrittleFlaskItem.class)
public class BrittleFlaskItemMixin {

    @Unique
    private static final String HOTBATH_POTION_PREFIX = "hotbath:";
    
    @Unique
    private static final int MAX_USES = 4;

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
     * Override isBarVisible to also show bar for HotBath fluids.
     */
    @Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
    private void hotbath$isBarVisibleForHotBath(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.getBoolean("IsHotBathContent") && tag.getInt("Uses") > 0) {
                cir.setReturnValue(true);
            }
        } catch (Throwable e) {
            CompatManager.reportRuntimeError("twilightforest", "BrittleFlaskItemMixin.isBarVisible", e);
        }
    }

    /**
     * Override getBarColor to use CustomColor for HotBath fluids.
     */
    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void hotbath$getBarColorForHotBath(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        try {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.getBoolean("IsHotBathContent") && tag.contains("CustomColor")) {
                int color = tag.getInt("CustomColor");
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
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.getBoolean("IsHotBathContent")) {
                // Check for legacy bath type (hot_water, honey_bath, etc.)
                if (tag.contains("HotBathType")) {
                    String bathTypeId = tag.getString("HotBathType");
                    TFFlaskColorHelper.applyEffectByTypeId(entity, bathTypeId);
                }
                // Check for custom fluids and apply temperature effects
                else if (tag.contains("HotBathFluidId") && entity instanceof Player player) {
                    String fluidIdStr = tag.getString("HotBathFluidId");
                    ResourceLocation fluidId = new ResourceLocation(fluidIdStr);
                    Optional<CustomFluidDefinition> definitionOpt = CustomFluidAPI.getFluidDefinition(fluidId);
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
        ResourceLocation fluidId = CustomFluidNBTHelper.getFluidId(bottleStack);
        if (fluidId == null) return false;
        
        Optional<CustomFluidDefinition> definitionOpt = CustomFluidAPI.getFluidDefinition(fluidId);
        if (definitionOpt.isEmpty()) return false;
        
        CustomFluidDefinition definition = definitionOpt.get();
        
        // Get or create flask NBT
        CompoundTag flaskTag = flaskStack.getOrCreateTag();
        
        // Create a unique potion ID for this custom fluid
        String hotbathPotionId = HOTBATH_POTION_PREFIX + fluidId.toString();
        
        // Check if we can add to the flask
        if (!hotbath$canAddToFlask(flaskTag, hotbathPotionId)) return false;
        
        // Consume bottle and give back glass bottle
        if (!player.getAbilities().instabuild) {
            bottleStack.shrink(1);
            if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
        }
        
        // Update flask NBT
        hotbath$updateFlaskNbt(flaskTag, hotbathPotionId, definition.color());
        
        // Store custom fluid effects in flask NBT
        hotbath$storeCustomFluidEffects(flaskTag, definition);
        
        player.playSound(TFSounds.FLASK_FILL.get(), flaskTag.getInt("Uses") * 0.25F, 
                player.level().getRandom().nextFloat() * 0.1F + 0.9F);
        
        return true;
    }

    /**
     * Handle adding a legacy bath water bottle to the flask.
     */
    @Unique
    private boolean hotbath$handleLegacyBathBottle(ItemStack flaskStack, ItemStack bottleStack, Player player) {
        Item bottleItem = bottleStack.getItem();
        
        // Get the bath type identifier
        String bathTypeId = hotbath$getLegacyBathTypeId(bottleItem);
        if (bathTypeId == null) return false;
        
        // Create a unique potion ID for this bath type
        String hotbathPotionId = HOTBATH_POTION_PREFIX + bathTypeId;
        
        // Get or create flask NBT
        CompoundTag flaskTag = flaskStack.getOrCreateTag();
        
        // Check if we can add to the flask
        if (!hotbath$canAddToFlask(flaskTag, hotbathPotionId)) return false;
        
        // Consume bottle and give back glass bottle
        if (!player.getAbilities().instabuild) {
            bottleStack.shrink(1);
            if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
        }
        
        // Get bath color for display
        int bathColor = hotbath$getLegacyBathColor(bottleItem);
        
        // Update flask NBT
        hotbath$updateFlaskNbt(flaskTag, hotbathPotionId, bathColor);
        
        // Store the bath type for effect application when drinking
        flaskTag.putString("HotBathType", bathTypeId);
        
        player.playSound(TFSounds.FLASK_FILL.get(), flaskTag.getInt("Uses") * 0.25F, 
                player.level().getRandom().nextFloat() * 0.1F + 0.9F);
        
        return true;
    }

    /**
     * Check if we can add the potion to the flask.
     */
    @Unique
    private boolean hotbath$canAddToFlask(CompoundTag flaskTag, String potionId) {
        // Check if flask can be refilled
        if (flaskTag.contains("Refillable") && !flaskTag.getBoolean("Refillable")) {
            return false;
        }

        int currentUses = flaskTag.getInt("Uses");

        // Check if flask is full
        if (currentUses >= MAX_USES) {
            return false;
        }

        // Check if flask is empty or contains the same potion
        if (!flaskTag.contains("Potion")) {
            return true;
        }

        String currentPotion = flaskTag.getString("Potion");
        return currentPotion.equals(potionId);
    }

    /**
     * Update the flask NBT with a new dose.
     */
    @Unique
    private void hotbath$updateFlaskNbt(CompoundTag flaskTag, String potionId, int color) {
        flaskTag.putString("Potion", potionId);
        flaskTag.putInt("Uses", flaskTag.getInt("Uses") + 1);
        // Store the custom color for rendering
        flaskTag.putInt("CustomColor", color);
        // Mark as HotBath content
        flaskTag.putBoolean("IsHotBathContent", true);
    }

    /**
     * Store custom fluid effects in the flask NBT for later application.
     */
    @Unique
    private void hotbath$storeCustomFluidEffects(CompoundTag flaskTag, CustomFluidDefinition definition) {
        // Store the fluid ID so we can look up effects when drinking
        flaskTag.putString("HotBathFluidId", definition.id().toString());
    }

    /**
     * Get the bath type identifier for a legacy bath bottle item.
     * Uses TFFlaskColorHelper for centralized color/type management.
     */
    @Unique
    private String hotbath$getLegacyBathTypeId(Item bottleItem) {
        return TFFlaskColorHelper.getTypeIdForBottle(bottleItem);
    }

    /**
     * Get the color for a legacy bath bottle item.
     * Uses TFFlaskColorHelper for centralized color management from FluidsColor.
     */
    @Unique
    private int hotbath$getLegacyBathColor(Item bottleItem) {
        int color = TFFlaskColorHelper.getColorForBottle(bottleItem);
        return color != -1 ? color : 0xFFFFFF;  // White fallback
    }
}
