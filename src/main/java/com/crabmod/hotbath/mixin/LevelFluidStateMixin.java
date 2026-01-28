package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to override getFluidState to return the correct fluid type for hotBath fluids.
 * This ensures that waterlogged blocks display the correct fluid texture.
 */
@Mixin(Level.class)
public abstract class LevelFluidStateMixin {

    /**
     * Inject at the HEAD of getFluidState to return the correct fluid for hotBath waterlogged blocks
     */
    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    private void hotbath$getFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        Level level = (Level) (Object) this;
        BlockState blockState = level.getBlockState(pos);
        
        // Check if the block is waterlogged
        if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && blockState.getValue(BlockStateProperties.WATERLOGGED)) {
            // Get the stored fluid type
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
            
            if (storedFluid != null && storedFluid != Fluids.EMPTY && storedFluid != Fluids.WATER) {
                // Return the stored fluid's default state (source block)
                cir.setReturnValue(storedFluid.defaultFluidState());
            }
        }
    }
}
