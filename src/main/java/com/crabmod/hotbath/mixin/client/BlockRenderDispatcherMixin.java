package com.crabmod.hotbath.mixin.client;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Client-side mixin to intercept fluid rendering.
 * This ensures that waterlogged blocks render with the correct hotBath fluid texture.
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    /**
     * Modify the fluidState variable in renderLiquid to use the correct hotBath fluid.
     * This is called during chunk compilation when rendering fluids.
     */
    @ModifyVariable(
        method = "renderLiquid",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private FluidState hotbath$modifyFluidState(FluidState fluidState, BlockPos pos, BlockAndTintGetter level, VertexConsumer consumer, BlockState blockState) {
        // Check if the block is waterlogged
        if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && blockState.getValue(BlockStateProperties.WATERLOGGED)) {
            // Get the stored fluid type from our helper
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClient(level, pos);
            
            if (storedFluid != null && storedFluid != Fluids.EMPTY && storedFluid != Fluids.WATER) {
                // Return the stored fluid's source state for rendering
                return storedFluid.defaultFluidState();
            }
        }
        return fluidState;
    }
}
