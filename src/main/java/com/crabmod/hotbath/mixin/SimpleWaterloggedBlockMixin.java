package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.CustomFluidDataComponents;
import com.crabmod.hotbath.custom_fluid.CustomFluidItems;
import com.crabmod.hotbath.custom_fluid.CustomFluidRegistry;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to extend SimpleWaterloggedBlock to accept any fluid in the #minecraft:water tag.
 * This allows hotBath fluids to waterlog blocks like stairs, slabs, fences, etc.
 * 
 * <p>Priority is set high to ensure we run before other mods' mixins.</p>
 */
@Mixin(value = SimpleWaterloggedBlock.class, priority = 500)
public interface SimpleWaterloggedBlockMixin {

    /**
     * Modify canPlaceLiquid to accept hotBath fluids in waterloggable blocks.
     * Only handles hotBath fluids specifically, not other mods' water-like fluids.
     * Also allows replacing existing waterlogged fluid with a different hotBath fluid.
     * For dynamic custom fluids, also checks customFluidId to distinguish between different custom fluids.
     * 
     * IMPORTANT: Only SOURCE fluids can waterlog blocks - flowing fluids should not waterlog!
     */
    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos,
                                         BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        // Only handle hotBath fluids specifically - non-invasive to other mods
        if (HotbathFluidHelper.isHotbathFluid(fluid)) {
            // IMPORTANT: Only SOURCE fluids can waterlog blocks
            // Flowing fluids should not be able to waterlog - they should just be destroyed
            // when a block is placed in them
            if (fluid instanceof FlowingFluid flowingFluid) {
                // Check if this is the flowing version (not the source)
                if (flowingFluid.getSource() != flowingFluid) {
                    // This is the flowing version, don't allow it to waterlog
                    // Let vanilla behavior handle this (will return false)
                    return;
                }
            }
            
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                boolean isWaterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
                if (!isWaterlogged) {
                    // Not waterlogged, can place
                    cir.setReturnValue(true);
                } else {
                    // Already waterlogged - check if we can replace the existing fluid
                    // Allow replacement if the new fluid is different from the stored one
                    Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
                    Fluid sourceFluid = hotbath$getSourceFluid(fluid);
                    if (storedFluid == null || storedFluid == Fluids.WATER || storedFluid == Fluids.EMPTY) {
                        // Stored fluid is vanilla water or unknown, allow replacement
                        cir.setReturnValue(true);
                    } else if (storedFluid != sourceFluid) {
                        // Different fluid type, allow replacement
                        cir.setReturnValue(true);
                    } else if (hotbath$isDynamicCustomFluid(sourceFluid)) {
                        // Same base fluid type (DYNAMIC_FLUID_STILL), but might be different custom fluids
                        // Check customFluidId to distinguish between different custom fluids
                        ResourceLocation storedCustomId = hotbath$getStoredCustomFluidIdFromBlockGetter(level, pos);
                        ResourceLocation newCustomId = hotbath$findCustomFluidIdFromSurrounding(level, pos);
                        if (storedCustomId == null || newCustomId == null || !storedCustomId.equals(newCustomId)) {
                            // Different custom fluid or unknown, allow replacement
                            cir.setReturnValue(true);
                        }
                        // If same custom fluid ID, let vanilla behavior handle (return false)
                    }
                    // If same fluid, let vanilla behavior handle (return false)
                }
            }
        }
    }

    /**
     * Modify placeLiquid to handle hotBath fluids.
     * Only handles hotBath fluids specifically - non-invasive to other mods.
     * Uses defensive programming to ensure block state is properly preserved.
     * Also handles replacing existing waterlogged fluid with a different hotBath fluid.
     * 
     * IMPORTANT: Only SOURCE fluids can waterlog blocks - flowing fluids should not waterlog!
     */
    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$placeLiquid(LevelAccessor level, BlockPos pos, BlockState state,
                                      FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        // Only handle hotBath fluids specifically
        if (HotbathFluidHelper.isHotbathFluid(fluidState.getType())) {
            // IMPORTANT: Only SOURCE fluids can waterlog blocks
            // Flowing fluids should not be able to waterlog - they should just be destroyed
            // when a block is placed in them
            if (!fluidState.isSource()) {
                // This is a flowing fluid, don't allow it to waterlog
                // Return false to indicate placement failed (the fluid should be destroyed)
                cir.setReturnValue(false);
                return;
            }
            
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                boolean isAlreadyWaterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
                
                // DEFENSIVE: Get fresh block state from world to ensure we have the latest
                BlockState currentState = level.getBlockState(pos);
                
                // Verify the block is still the same type
                if (!currentState.is(state.getBlock())) {
                    // Block changed, don't proceed
                    cir.setReturnValue(false);
                    return;
                }
                
                // Store the SOURCE fluid type for later retrieval
                // IMPORTANT: Always store the source version, not the flowing version
                // This ensures proper behavior in fluid tick logic (isSource() check)
                Fluid fluidToStore = hotbath$getSourceFluid(fluidState.getType());
                HotbathWaterloggingHelper.storeFluidType(level, pos, fluidToStore);
                
                // For dynamic custom fluids, also store the custom fluid ID
                // by finding the source CustomFluidBlockEntity
                ResourceLocation customFluidId = null;
                if (hotbath$isDynamicCustomFluid(fluidToStore)) {
                    customFluidId = hotbath$findCustomFluidIdFromSurrounding(level, pos);
                    if (customFluidId != null) {
                        HotbathWaterloggingHelper.storeCustomFluidId(level, pos, customFluidId);
                    }
                } else {
                    // Not a dynamic custom fluid, clear any stored custom ID
                    HotbathWaterloggingHelper.removeCustomFluidId(level, pos);
                }
                
                if (!isAlreadyWaterlogged) {
                    // DEFENSIVE: Create new state from current world state, not passed parameter
                    BlockState newState = currentState.setValue(BlockStateProperties.WATERLOGGED, true);
                    
                    // Set the block state with flag 3 (notify clients + neighbors)
                    boolean success = level.setBlock(pos, newState, 3);
                    
                    if (success) {
                        // Schedule tick for fluid behavior
                        level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
                        
                        // DEFENSIVE: Verify the block was actually set correctly
                        BlockState verifyState = level.getBlockState(pos);
                        if (!verifyState.hasProperty(BlockStateProperties.WATERLOGGED) 
                                || !verifyState.getValue(BlockStateProperties.WATERLOGGED)) {
                            // Block state was not set correctly, try again with higher priority flag
                            level.setBlock(pos, newState, 2 | 16 | 32 | 64);
                        }
                        
                        // Trigger neighbor updates to propagate fluid to blocks below
                        hotbath$triggerFluidSpreadUpdate(level, pos, fluidToStore, customFluidId);
                    } else {
                        // setBlock failed, clean up stored fluid type
                        HotbathWaterloggingHelper.removeFluidType(level, pos);
                    }
                    
                    cir.setReturnValue(success);
                } else {
                    // Already waterlogged - we're replacing the fluid
                    // No need to change block state, just update the stored fluid type (done above)
                    // Schedule tick for fluid behavior
                    level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
                    
                    // IMPORTANT: Trigger neighbor updates to propagate the new fluid type
                    // This ensures fluid blocks below update to the new type
                    hotbath$triggerFluidSpreadUpdate(level, pos, fluidToStore, customFluidId);
                    
                    cir.setReturnValue(true);
                }
            }
        }
    }

    /**
     * Modify pickupBlock to return the correct bucket for hotBath fluids.
     * Only handles hotBath fluids specifically - non-invasive to other mods.
     */
    @Inject(method = "pickupBlock", at = @At("HEAD"), cancellable = true)
    default void hotbath$pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos,
                                      BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            // Get the stored fluid type
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
            
            // Only handle hotBath fluids specifically
            if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                // DEFENSIVE: Get fresh state from world
                BlockState currentState = level.getBlockState(pos);
                if (currentState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    level.setBlock(pos, currentState.setValue(BlockStateProperties.WATERLOGGED, false), 3);
                }
                
                ItemStack bucket;
                
                // Check if this is a dynamic custom fluid
                if (hotbath$isDynamicCustomFluid(storedFluid)) {
                    // Get the custom fluid ID from storage
                    ResourceLocation customFluidId = HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos);
                    if (customFluidId != null) {
                        // Create a custom fluid bucket with the correct fluid ID
                        bucket = CustomFluidDataComponents.createStack(
                                CustomFluidItems.CUSTOM_FLUID_BUCKET.get(), customFluidId);
                    } else {
                        // Fallback to empty bucket if no custom ID stored
                        bucket = ItemStack.EMPTY;
                    }
                } else {
                    // Built-in hotBath fluid - use standard bucket
                    bucket = new ItemStack(storedFluid.getBucket());
                }
                
                HotbathWaterloggingHelper.removeFluidType(level, pos);
                HotbathWaterloggingHelper.removeFromClientCache(pos);
                
                if (!bucket.isEmpty()) {
                    cir.setReturnValue(bucket);
                }
            }
        }
    }
    
    /**
     * Check if a fluid is the dynamic custom fluid (from data packs).
     */
    @Unique
    private static boolean hotbath$isDynamicCustomFluid(Fluid fluid) {
        Fluid sourceFluid = hotbath$getSourceFluid(fluid);
        return sourceFluid == DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()
                || sourceFluid == DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get();
    }
    
    /**
     * Trigger fluid spread updates to propagate the fluid to adjacent and below blocks.
     * This is called when replacing a waterlogged fluid or placing a new one.
     * 
     * <p>This method handles two scenarios:
     * 1. The block below is a fluid block of the old type - replace it with the new fluid
     * 2. The block below is air or replaceable - let normal fluid mechanics handle it</p>
     * 
     * <p>For dynamic custom fluids, compares customFluidId to determine if update is needed,
     * since all custom fluids share the same DYNAMIC_FLUID_STILL type.</p>
     * 
     * @param level The level
     * @param pos The position of the source (waterlogged block or fluid block)
     * @param newFluid The new fluid type to propagate
     * @param customFluidId The custom fluid ID (null for built-in fluids)
     */
    @Unique
    private static void hotbath$triggerFluidSpreadUpdate(LevelAccessor level, BlockPos pos, 
                                                          Fluid newFluid, ResourceLocation customFluidId) {
        if (level.isClientSide()) return;
        
        // Simple approach: Schedule ticks for all neighboring fluid blocks
        // The overridden getNewLiquid in DynamicCustomFluid will handle the customFluidId check
        // and flowing fluids without valid source support will automatically disappear
        
        BlockState currentState = level.getBlockState(pos);
        
        // Schedule ticks for all 6 directions
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            FluidState neighborFluidState = level.getFluidState(neighborPos);
            
            if (!neighborFluidState.isEmpty() && HotbathFluidHelper.isHotbathFluid(neighborFluidState.getType())) {
                // Schedule a fluid tick - the fluid's tick() method will call getNewLiquid()
                // which we've overridden to check customFluidId
                level.scheduleTick(neighborPos, neighborFluidState.getType(), 
                        neighborFluidState.getType().getTickDelay(level));
            }
        }
        
        // Also trigger block update notifications so fluids know their neighbors changed
        if (level instanceof net.minecraft.world.level.Level realLevel) {
            realLevel.updateNeighborsAt(pos, currentState.getBlock());
        }
    }
    
    /**
     * Get the custom fluid ID at a specific position.
     * Works for both waterlogged blocks (from storage) and fluid blocks (from BlockEntity).
     */
    @Unique
    private static ResourceLocation hotbath$getCustomFluidIdAt(LevelAccessor level, BlockPos pos, BlockState state) {
        // Check if it's a waterlogged block
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos);
        }
        
        // Check if it's a CustomFluidBlockEntity
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            return customBe.getFluidId();
        }
        
        return null;
    }
    
    /**
     * Find the custom fluid ID from surrounding CustomFluidBlockEntity blocks.
     * Searches current position first, then adjacent positions.
     * This version works with BlockGetter for use in canPlaceLiquid.
     */
    @Unique
    private static ResourceLocation hotbath$findCustomFluidIdFromSurrounding(BlockGetter level, BlockPos pos) {
        // First check the current position (in case the fluid block is there)
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe && customBe.getFluidId() != null) {
            return customBe.getFluidId();
        }
        
        // Search adjacent positions for CustomFluidBlockEntity
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            BlockEntity adjacentBe = level.getBlockEntity(adjacent);
            if (adjacentBe instanceof CustomFluidBlockEntity customBe && customBe.getFluidId() != null) {
                return customBe.getFluidId();
            }
        }
        
        return null;
    }
    
    /**
     * Get the stored custom fluid ID from a BlockGetter.
     * This works for both server side (if level is LevelAccessor) and client side (using cache).
     */
    @Unique
    private static ResourceLocation hotbath$getStoredCustomFluidIdFromBlockGetter(BlockGetter level, BlockPos pos) {
        if (level instanceof LevelAccessor levelAccessor) {
            return HotbathWaterloggingHelper.getStoredCustomFluidId(levelAccessor, pos);
        }
        // Fallback to client cache for pure BlockGetter
        return HotbathWaterloggingHelper.getStoredCustomFluidIdClientDirect(pos);
    }
    
    /**
     * Get the source version of a fluid.
     * For FlowingFluid, this returns the source fluid.
     * For other fluids, returns the fluid itself.
     * 
     * <p>This is important because we need to store the source version to ensure
     * proper behavior in fluid tick logic (isSource() check in FlowingFluid.tick()).</p>
     */
    @Unique
    private static Fluid hotbath$getSourceFluid(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowingFluid) {
            return flowingFluid.getSource();
        }
        return fluid;
    }
}
