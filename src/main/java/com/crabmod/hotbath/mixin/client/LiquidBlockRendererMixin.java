package com.crabmod.hotbath.mixin.client;

import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to fix hotBath fluid rendering in waterlogged blocks.
 * 
 * <p>Problem: The vanilla LiquidBlockRenderer gets adjacent FluidStates via 
 * BlockState.getFluidState(), which returns vanilla water for waterlogged blocks.
 * This causes hotBath fluids to not connect properly between adjacent waterlogged blocks.</p>
 * 
 * <p>Solution: Redirect the relevant method calls to use our corrected fluid state logic.</p>
 */
@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {

    /**
     * Thread-local storage for current render position.
     * Used by static method redirects to get position context.
     */
    @Unique
    private static final ThreadLocal<BlockPos> hotbath$pos = new ThreadLocal<>();

    /**
     * Capture position context at start of tesselate.
     */
    @Inject(method = "tesselate", at = @At("HEAD"))
    private void hotbath$captureContext(BlockAndTintGetter level, BlockPos pos, 
            com.mojang.blaze3d.vertex.VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        hotbath$pos.set(pos.immutable());
    }

    /**
     * Clear context at end of tesselate.
     */
    @Inject(method = "tesselate", at = @At("RETURN"))
    private void hotbath$clearContext(BlockAndTintGetter level, BlockPos pos, 
            com.mojang.blaze3d.vertex.VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        hotbath$pos.remove();
    }

    /**
     * Get the stored hotBath fluid for a waterlogged block, or null if not applicable.
     * Only returns hotBath fluids specifically.
     */
    @Unique
    private static Fluid hotbath$getStoredFluid(BlockPos pos, BlockState blockState) {
        if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && blockState.getValue(BlockStateProperties.WATERLOGGED)) {
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(pos);
            // Only return hotBath fluids specifically
            if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                return storedFluid;
            }
        }
        return null;
    }

    /**
     * Redirect shouldHideAdjacentFluidFace in isNeighborStateHidingOverlay.
     * Checks if adjacent waterlogged block has same hotBath fluid.
     */
    @Redirect(
        method = "isNeighborStateHidingOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;shouldHideAdjacentFluidFace(Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)Z"
        )
    )
    private static boolean hotbath$redirectShouldHideFluidFace(BlockState otherState, Direction neighborFace, FluidState selfState) {
        // Check if otherState is waterlogged with hotBath fluid
        BlockPos currentPos = hotbath$pos.get();
        if (currentPos != null && otherState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && otherState.getValue(BlockStateProperties.WATERLOGGED)) {
            BlockPos otherPos = currentPos.relative(neighborFace.getOpposite());
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(otherPos);
            if (HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                return storedFluid.isSame(selfState.getType());
            }
        }
        return otherState.shouldHideAdjacentFluidFace(neighborFace, selfState);
    }

    /**
     * Redirect getHeight(level, fluid, pos, blockState, fluidState) in tesselate method.
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
