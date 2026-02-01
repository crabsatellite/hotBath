package com.crabmod.hotbath.mixin.twilightforest;

import com.crabmod.hotbath.compat.CompatManager;
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
     */
    @Unique
    private String hotbath$getLegacyBathTypeId(Item bottleItem) {
        if (bottleItem == ItemRegister.HOT_WATER_BOTTLE.get()) {
            return "hot_water";
        } else if (bottleItem == ItemRegister.HONEY_BATH_BOTTLE.get()) {
            return "honey_bath";
        } else if (bottleItem == ItemRegister.MILK_BATH_BOTTLE.get()) {
            return "milk_bath";
        } else if (bottleItem == ItemRegister.HERBAL_BATH_BOTTLE.get()) {
            return "herbal_bath";
        } else if (bottleItem == ItemRegister.PEONY_BATH_BOTTLE.get()) {
            return "peony_bath";
        } else if (bottleItem == ItemRegister.ROSE_BATH_BOTTLE.get()) {
            return "rose_bath";
        }
        return null;
    }

    /**
     * Get the color for a legacy bath bottle item.
     */
    @Unique
    private int hotbath$getLegacyBathColor(Item bottleItem) {
        if (bottleItem == ItemRegister.HOT_WATER_BOTTLE.get()) {
            return 0xE0FFFF;  // Light cyan
        } else if (bottleItem == ItemRegister.HONEY_BATH_BOTTLE.get()) {
            return 0xFFB300;  // Amber/Gold
        } else if (bottleItem == ItemRegister.MILK_BATH_BOTTLE.get()) {
            return 0xFFFAF0;  // Floral white
        } else if (bottleItem == ItemRegister.HERBAL_BATH_BOTTLE.get()) {
            return 0x2B8B57;  // Sea green
        } else if (bottleItem == ItemRegister.PEONY_BATH_BOTTLE.get()) {
            return 0xFFB6C1;  // Light pink
        } else if (bottleItem == ItemRegister.ROSE_BATH_BOTTLE.get()) {
            return 0xFF69B4;  // Hot pink
        }
        return 0xFFFFFF;  // White fallback
    }
}
