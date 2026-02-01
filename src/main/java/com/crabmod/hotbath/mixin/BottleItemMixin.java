package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.custom_fluid.CustomFluidAPI;
import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.DynamicCustomFluidBlock;
import com.crabmod.hotbath.fluid_blocks.*;
import com.crabmod.hotbath.registers.ItemRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Mixin for BottleItem to allow filling glass bottles from custom fluid blocks.
 * 
 * <p>Problem: Vanilla glass bottles only check for water blocks (via FluidTags.WATER),
 * which doesn't include our custom fluids. This prevents players from filling bottles
 * with custom fluids directly from the world.</p>
 * 
 * <p>Solution: Inject at the head of the use method to check if the player is looking
 * at a custom fluid block. If so, fill the bottle with that custom fluid instead of
 * proceeding with vanilla behavior.</p>
 */
@Mixin(BottleItem.class)
public class BottleItemMixin {
    
    /**
     * Inject at HEAD to intercept bottle usage when looking at custom fluid blocks.
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void hotbath$fillFromCustomFluid(Level level, Player player, InteractionHand hand,
                                              CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack heldItem = player.getItemInHand(hand);
        
        // Perform ray trace to find what block the player is looking at
        BlockHitResult hitResult = hotbath$getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return; // Not looking at a block, let vanilla handle it
        }
        
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        // First, check if it's a dynamic custom fluid block
        if (block instanceof DynamicCustomFluidBlock) {
            // Get the fluid definition from BlockEntity
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CustomFluidBlockEntity customBe) {
                Optional<CustomFluidDefinition> definitionOpt = customBe.getFluidDefinition();
                if (definitionOpt.isPresent()) {
                    CustomFluidDefinition definition = definitionOpt.get();
                    
                    // Fill the bottle with custom fluid
                    if (!level.isClientSide) {
                        // Play sound and emit game event
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                    }
                    
                    // Create the filled bottle for returning
                    ItemStack filledBottle = CustomFluidAPI.createBottle(definition);
                    
                    // Use ItemUtils to handle the exchange properly (works for both survival and creative)
                    ItemStack result = ItemUtils.createFilledResult(heldItem, player, filledBottle);
                    
                    cir.setReturnValue(InteractionResultHolder.sidedSuccess(result, level.isClientSide()));
                    return;
                }
            }
            return; // No valid fluid definition, let vanilla handle it
        }
        
        // Check for legacy bath fluid blocks (5 types)
        Item bottleItem = hotbath$getLegacyBathBottle(block);
        if (bottleItem != null) {
            // Fill the bottle with legacy bath fluid
            if (!level.isClientSide) {
                // Play sound and emit game event
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }
            
            // Create the filled bottle for returning
            ItemStack filledBottle = new ItemStack(bottleItem);
            
            // Use ItemUtils to handle the exchange properly (works for both survival and creative)
            ItemStack result = ItemUtils.createFilledResult(heldItem, player, filledBottle);
            
            cir.setReturnValue(InteractionResultHolder.sidedSuccess(result, level.isClientSide()));
        }
        // Not a hotbath fluid, let vanilla handle it
    }
    
    /**
     * Maps legacy bath fluid blocks to their corresponding bottle items.
     */
    @Unique
    private static Item hotbath$getLegacyBathBottle(Block block) {
        if (block instanceof HotWaterBlock) {
            return ItemRegister.HOT_WATER_BOTTLE.get();
        } else if (block instanceof HoneyBathBlock) {
            return ItemRegister.HONEY_BATH_BOTTLE.get();
        } else if (block instanceof MilkBathBlock) {
            return ItemRegister.MILK_BATH_BOTTLE.get();
        } else if (block instanceof HerbalBathBlock) {
            return ItemRegister.HERBAL_BATH_BOTTLE.get();
        } else if (block instanceof PeonyBathBlock) {
            return ItemRegister.PEONY_BATH_BOTTLE.get();
        } else if (block instanceof RoseBathBlock) {
            return ItemRegister.ROSE_BATH_BOTTLE.get();
        }
        return null;
    }
    
    /**
     * Helper method to perform ray trace. This is a copy of Item.getPlayerPOVHitResult
     * since it's protected and we can't access it directly.
     */
    @Unique
    private static BlockHitResult hotbath$getPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluidMode) {
        float xRot = player.getXRot();
        float yRot = player.getYRot();
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
        float f2 = (float) Math.cos(-yRot * ((float)Math.PI / 180F) - (float)Math.PI);
        float f3 = (float) Math.sin(-yRot * ((float)Math.PI / 180F) - (float)Math.PI);
        float f4 = (float) -Math.cos(-xRot * ((float)Math.PI / 180F));
        float f5 = (float) Math.sin(-xRot * ((float)Math.PI / 180F));
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double reach = player.blockInteractionRange();
        net.minecraft.world.phys.Vec3 lookVec = eyePos.add((double)f6 * reach, (double)f5 * reach, (double)f7 * reach);
        return level.clip(new ClipContext(eyePos, lookVec, ClipContext.Block.OUTLINE, fluidMode, player));
    }
}
