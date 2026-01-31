package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.custom_fluid.CustomFluidDataComponents;
import com.crabmod.hotbath.custom_fluid.CustomFluidItems;
import com.crabmod.hotbath.custom_fluid.CustomFluidRegistry;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
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
     */
    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos,
                                         BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        // Only handle hotBath fluids specifically - non-invasive to other mods
        if (HotbathFluidHelper.isHotbathFluid(fluid)) {
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                boolean isWaterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
                if (!isWaterlogged) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    /**
     * Modify placeLiquid to handle hotBath fluids.
     * Only handles hotBath fluids specifically - non-invasive to other mods.
     * Uses defensive programming to ensure block state is properly preserved.
     */
    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$placeLiquid(LevelAccessor level, BlockPos pos, BlockState state,
                                      FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        // Only handle hotBath fluids specifically
        if (HotbathFluidHelper.isHotbathFluid(fluidState.getType())) {
            if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                    && !state.getValue(BlockStateProperties.WATERLOGGED)) {
                
                // DEFENSIVE: Get fresh block state from world to ensure we have the latest
                BlockState currentState = level.getBlockState(pos);
                
                // Verify the block is still the same type and not already waterlogged
                if (!currentState.is(state.getBlock())) {
                    // Block changed, don't proceed
                    cir.setReturnValue(false);
                    return;
                }
                
                if (currentState.hasProperty(BlockStateProperties.WATERLOGGED) 
                        && currentState.getValue(BlockStateProperties.WATERLOGGED)) {
                    // Already waterlogged, don't proceed
                    cir.setReturnValue(false);
                    return;
                }
                
                // Store the SOURCE fluid type for later retrieval
                // IMPORTANT: Always store the source version, not the flowing version
                // This ensures proper behavior in fluid tick logic (isSource() check)
                Fluid fluidToStore = hotbath$getSourceFluid(fluidState.getType());
                HotbathWaterloggingHelper.storeFluidType(level, pos, fluidToStore);
                
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
                } else {
                    // setBlock failed, clean up stored fluid type
                    HotbathWaterloggingHelper.removeFluidType(level, pos);
                }
                
                cir.setReturnValue(success);
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
