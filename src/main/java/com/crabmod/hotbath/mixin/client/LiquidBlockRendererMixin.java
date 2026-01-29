package com.crabmod.hotbath.mixin.client;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
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
     * Thread-local storage for render context.
     */
    @Unique
    private static final ThreadLocal<BlockAndTintGetter> hotbath$level = new ThreadLocal<>();
    
    @Unique
    private static final ThreadLocal<BlockPos> hotbath$pos = new ThreadLocal<>();

    /**
     * Capture context at start of tesselate.
     */
    @Inject(method = "tesselate", at = @At("HEAD"))
    private void hotbath$captureContext(BlockAndTintGetter level, BlockPos pos, 
            com.mojang.blaze3d.vertex.VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        hotbath$level.set(level);
        hotbath$pos.set(pos.immutable());
    }

    /**
     * Clear context at end of tesselate.
     */
    @Inject(method = "tesselate", at = @At("RETURN"))
    private void hotbath$clearContext(BlockAndTintGetter level, BlockPos pos, 
            com.mojang.blaze3d.vertex.VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        hotbath$level.remove();
        hotbath$pos.remove();
    }

    /**
     * Get the correct FluidState for a waterlogged block position.
     */
    @Unique
    private static FluidState hotbath$getCorrectFluidState(BlockPos pos, BlockState blockState) {
        if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && blockState.getValue(BlockStateProperties.WATERLOGGED)) {
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(pos);
            if (storedFluid != null && storedFluid != Fluids.EMPTY && storedFluid != Fluids.WATER) {
                return storedFluid.defaultFluidState();
            }
        }
        return blockState.getFluidState();
    }

    /**
     * Check if two fluids are the same, considering hotBath waterlogging.
     */
    @Unique
    private static boolean hotbath$isSameFluid(Fluid renderingFluid, Fluid otherFluid, BlockPos otherPos) {
        if (renderingFluid.isSame(otherFluid)) {
            return true;
        }
        // Check if position has a stored hotBath fluid matching the rendering fluid
        Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(otherPos);
        if (storedFluid != null && storedFluid != Fluids.EMPTY && storedFluid != Fluids.WATER) {
            return renderingFluid.isSame(storedFluid);
        }
        return false;
    }

    /**
     * Redirect shouldHideAdjacentFluidFace in isNeighborStateHidingOverlay.
     * 
     * Original: otherState.shouldHideAdjacentFluidFace(neighborFace, selfState)
     * This checks if otherState's fluid equals selfState's fluid (the one being rendered).
     * 
     * We need to check our cache for the actual fluid in otherState.
     */
    @Redirect(
        method = "isNeighborStateHidingOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;shouldHideAdjacentFluidFace(Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)Z"
        )
    )
    private static boolean hotbath$redirectShouldHideFluidFace(BlockState otherState, Direction neighborFace, FluidState selfState) {
        // selfState is the fluid being rendered (could be hotBath fluid)
        // otherState is the adjacent block - we need to check if it has the same fluid
        
        // If otherState is waterlogged, check our cache
        if (otherState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && otherState.getValue(BlockStateProperties.WATERLOGGED)) {
            
            // We need the position of otherState, but this method doesn't have it
            // We'll use the context from tesselate
            BlockPos currentPos = hotbath$pos.get();
            if (currentPos != null) {
                // neighborFace is the face of otherState facing the current block
                // So we need to go in the opposite direction from current pos
                Direction dirToOther = neighborFace.getOpposite();
                BlockPos otherPos = currentPos.relative(dirToOther);
                
                Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(otherPos);
                if (storedFluid != null && storedFluid != Fluids.EMPTY && storedFluid != Fluids.WATER) {
                    // Compare stored fluid with the fluid being rendered
                    return storedFluid.isSame(selfState.getType());
                }
            }
        }
        
        // Fall back to original behavior
        return otherState.shouldHideAdjacentFluidFace(neighborFace, selfState);
    }

    /**
     * Redirect getHeight(level, fluid, pos, blockState, fluidState) in tesselate method.
     * These calls determine the fluid height at adjacent positions.
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
        FluidState correctedFluidState = hotbath$getCorrectFluidState(pos, blockState);
        return hotbath$computeHeight(level, fluid, pos, blockState, correctedFluidState);
    }

    /**
     * Redirect getHeight(level, fluid, pos) in calculateAverageHeight method.
     * This is called for corner height calculations.
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
        FluidState fluidState = hotbath$getCorrectFluidState(pos, blockState);
        return hotbath$computeHeight(level, fluid, pos, blockState, fluidState);
    }

    /**
     * Compute fluid height, matching original logic but using our corrected fluid checks.
     */
    @Unique
    private static float hotbath$computeHeight(BlockAndTintGetter level, Fluid renderingFluid, BlockPos pos, 
            BlockState blockState, FluidState fluidState) {
        // Check if fluid at this position matches the fluid being rendered
        if (hotbath$isSameFluid(renderingFluid, fluidState.getType(), pos)) {
            // Check the block above
            BlockPos posAbove = pos.above();
            BlockState stateAbove = level.getBlockState(posAbove);
            FluidState fluidAbove = hotbath$getCorrectFluidState(posAbove, stateAbove);
            
            if (hotbath$isSameFluid(renderingFluid, fluidAbove.getType(), posAbove)) {
                return 1.0F;
            } else {
                return fluidState.getOwnHeight();
            }
        } else {
            return !blockState.isSolid() ? 0.0F : -1.0F;
        }
    }
}
