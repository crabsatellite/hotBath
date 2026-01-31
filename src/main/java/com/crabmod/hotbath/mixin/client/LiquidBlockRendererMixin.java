package com.crabmod.hotbath.mixin.client;

import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin to fix hotBath fluid rendering in waterlogged blocks (Forge 1.20.1 version).
 * 
 * <p>Problem: The vanilla LiquidBlockRenderer gets adjacent FluidStates via 
 * BlockState.getFluidState(), which returns vanilla water for waterlogged blocks.
 * This causes hotBath fluids to not connect properly between adjacent waterlogged blocks.</p>
 * 
 * <p>Solution: Redirect the isNeighborSameFluid and getHeight method calls to use 
 * our corrected fluid state logic that considers stored hotBath fluids.</p>
 * 
 * <p>Note: Unlike NeoForge 1.21, Forge 1.20.1 does not have the 
 * isNeighborStateHidingOverlay/shouldHideAdjacentFluidFace methods. Instead, 
 * we redirect isNeighborSameFluid which is used for fluid connection logic.</p>
 */
@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    /**
     * Get the stored hotBath fluid for a waterlogged block, or null if not applicable.
     * Only returns hotBath fluids specifically.
     */
    @Unique
    private static Fluid hotbath$getStoredFluid(BlockPos pos, BlockState blockState) {
        if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && blockState.getValue(BlockStateProperties.WATERLOGGED)) {
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClient(null, pos);
            // Only return hotBath fluids specifically
            if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                return storedFluid;
            }
        }
        return null;
    }

    /**
     * Redirect getHeight(level, fluid, pos, blockState, fluidState) in tesselate method.
     * This fixes the fluid height calculation to consider stored hotBath fluids.
     */
    @Redirect(
        method = "tesselate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)F"
        )
    )
    private float hotbath$redirectGetHeight(LiquidBlockRenderer self,
            BlockAndTintGetter level, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState) {
        Fluid storedFluid = hotbath$getStoredFluid(pos, blockState);
        FluidState correctedFluidState = storedFluid != null ? storedFluid.defaultFluidState() : fluidState;
        return hotbath$computeHeight(level, fluid, pos, blockState, correctedFluidState);
    }

    /**
     * Redirect getHeight(level, fluid, pos) in calculateAverageHeight method.
     * This fixes the corner height calculation to consider stored hotBath fluids.
     */
    @Redirect(
        method = "calculateAverageHeight",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/LiquidBlockRenderer;getHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;)F"
        )
    )
    private float hotbath$redirectGetHeightCorner(LiquidBlockRenderer self, 
            BlockAndTintGetter level, Fluid fluid, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        Fluid storedFluid = hotbath$getStoredFluid(pos, blockState);
        FluidState fluidState = storedFluid != null ? storedFluid.defaultFluidState() : blockState.getFluidState();
        return hotbath$computeHeight(level, fluid, pos, blockState, fluidState);
    }

    /**
     * Compute fluid height, matching original logic but using corrected fluid checks.
     * Original getHeight logic:
     * - If fluid matches, check if fluid above matches -> return 1.0F, else return own height
     * - If fluid doesn't match but block is not solid -> return 0.0F
     * - If block is solid -> return -1.0F
     */
    @Unique
    private static float hotbath$computeHeight(BlockAndTintGetter level, Fluid renderingFluid, BlockPos pos, 
            BlockState blockState, FluidState fluidState) {
        // Check if this position's fluid matches the rendering fluid
        Fluid posFluid = fluidState.getType();
        Fluid storedFluid = hotbath$getStoredFluid(pos, blockState);
        boolean isSame = renderingFluid.isSame(posFluid) || (storedFluid != null && renderingFluid.isSame(storedFluid));
        
        if (isSame) {
            // Check the block above
            BlockPos posAbove = pos.above();
            BlockState stateAbove = level.getBlockState(posAbove);
            Fluid storedAbove = hotbath$getStoredFluid(posAbove, stateAbove);
            Fluid fluidAbove = storedAbove != null ? storedAbove : stateAbove.getFluidState().getType();
            
            if (renderingFluid.isSame(fluidAbove)) {
                return 1.0F;
            }
            return fluidState.getOwnHeight();
        }
        return !blockState.isSolid() ? 0.0F : -1.0F;
    }
}
