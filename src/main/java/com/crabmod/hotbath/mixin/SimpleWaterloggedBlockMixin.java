package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to extend SimpleWaterloggedBlock to accept any fluid in the #minecraft:water tag.
 * This allows hotBath fluids to waterlog blocks like stairs, slabs, fences, etc.
 */
@Mixin(SimpleWaterloggedBlock.class)
public interface SimpleWaterloggedBlockMixin {

    /**
     * Modify canPlaceLiquid to accept any fluid in the water tag, not just Fluids.WATER
     */
    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos,
                                         BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        // Check if the state has WATERLOGGED property and the fluid is in the water tag
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            boolean isWaterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
            boolean isWaterTagFluid = fluid.defaultFluidState().is(FluidTags.WATER);
            
            if (!isWaterlogged && isWaterTagFluid) {
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Modify placeLiquid to handle any fluid in the water tag
     */
    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$placeLiquid(LevelAccessor level, BlockPos pos, BlockState state,
                                      FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        // If the fluid is in the water tag, handle it
        if (fluidState.is(FluidTags.WATER)) {
            if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                    && !state.getValue(BlockStateProperties.WATERLOGGED)) {
                // Store the fluid type for later retrieval
                // On server: store in SavedData (which also syncs to client cache)
                // On client: just update client cache directly for immediate rendering
                HotbathWaterloggingHelper.storeFluidType(level, pos, fluidState.getType());
                
                // Set the block state and schedule tick
                level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * Modify pickupBlock to return the correct bucket for hotBath fluids
     */
    @Inject(method = "pickupBlock", at = @At("HEAD"), cancellable = true)
    default void hotbath$pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos,
                                      BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            // Get the stored fluid type
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
            
            if (storedFluid != null && storedFluid != Fluids.WATER && storedFluid != Fluids.EMPTY) {
                level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
                HotbathWaterloggingHelper.removeFluidType(level, pos);
                // Also remove from client cache
                HotbathWaterloggingHelper.removeFromClientCache(pos);
                
                ItemStack bucket = new ItemStack(storedFluid.getBucket());
                if (!bucket.isEmpty()) {
                    cir.setReturnValue(bucket);
                }
            }
        }
    }
}
