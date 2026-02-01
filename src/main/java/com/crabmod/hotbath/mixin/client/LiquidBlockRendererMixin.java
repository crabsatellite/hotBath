package com.crabmod.hotbath.mixin.client;

import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * Mixin to fix hotBath fluid rendering in waterlogged blocks and between different custom fluids.
 * 
 * <p>Problem 1: The vanilla LiquidBlockRenderer gets adjacent FluidStates via 
 * BlockState.getFluidState(), which returns vanilla water for waterlogged blocks.
 * This causes hotBath fluids to not connect properly between adjacent waterlogged blocks.</p>
 * 
 * <p>Problem 2: All dynamic custom fluids share the same base fluid type (DYNAMIC_FLUID_STILL),
 * so the vanilla isSame() check returns true for all of them. This prevents different
 * custom fluids (e.g., milk_tea vs green_tea) from rendering boundaries between them.</p>
 * 
 * <p>Solution: Redirect the relevant method calls to use our corrected fluid state logic,
 * and compare customFluidIds when both positions contain dynamic custom fluids.</p>
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
     * Thread-local storage for current level context.
     * Used by static method redirects to access BlockEntity data.
     */
    @Unique
    private static final ThreadLocal<BlockAndTintGetter> hotbath$level = new ThreadLocal<>();

    /**
     * Capture position and level context at start of tesselate.
     */
    @Inject(method = "tesselate", at = @At("HEAD"))
    private void hotbath$captureContext(BlockAndTintGetter level, BlockPos pos, 
            com.mojang.blaze3d.vertex.VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        hotbath$pos.set(pos.immutable());
        hotbath$level.set(level);
    }

    /**
     * Clear context at end of tesselate.
     */
    @Inject(method = "tesselate", at = @At("RETURN"))
    private void hotbath$clearContext(BlockAndTintGetter level, BlockPos pos, 
            com.mojang.blaze3d.vertex.VertexConsumer buffer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        hotbath$pos.remove();
        hotbath$level.remove();
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
     * Get the customFluidId at a position (from BlockEntity or waterlogging storage).
     * Returns null if position doesn't contain a dynamic custom fluid.
     */
    @Unique
    private static ResourceLocation hotbath$getCustomFluidId(BlockAndTintGetter level, BlockPos pos) {
        if (level == null || pos == null) return null;
        
        // Try BlockEntity first (for normal fluid blocks)
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            return customBe.getFluidId();
        }
        
        // Try waterlogging storage (for waterlogged blocks)
        return HotbathWaterloggingHelper.getStoredCustomFluidIdClientDirect(pos);
    }
    
    /**
     * Check if a fluid is a dynamic custom fluid.
     */
    @Unique
    private static boolean hotbath$isDynamicCustomFluid(Fluid fluid) {
        if (fluid == null) return false;
        Fluid stillFluid = DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get();
        Fluid flowingFluid = DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get();
        return fluid.isSame(stillFluid) || fluid.isSame(flowingFluid);
    }

    /**
     * Redirect shouldHideAdjacentFluidFace in isNeighborStateHidingOverlay.
     * Checks if adjacent block has same hotBath fluid, considering both waterlogged 
     * blocks and normal fluid blocks with customFluidId.
     */
    @Redirect(
        method = "isNeighborStateHidingOverlay",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;shouldHideAdjacentFluidFace(Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)Z"
        )
    )
    private static boolean hotbath$redirectShouldHideFluidFace(BlockState otherState, Direction neighborFace, FluidState selfState) {
        BlockPos currentPos = hotbath$pos.get();
        BlockAndTintGetter level = hotbath$level.get();
        
        if (currentPos == null) {
            return otherState.shouldHideAdjacentFluidFace(neighborFace, selfState);
        }
        
        BlockPos otherPos = currentPos.relative(neighborFace.getOpposite());
        
        // Check if the self fluid is a dynamic custom fluid
        boolean selfIsDynamic = hotbath$isDynamicCustomFluid(selfState.getType());
        
        // Get the other block's fluid
        FluidState otherFluidState = otherState.getFluidState();
        Fluid otherFluid = otherFluidState.getType();
        
        // Check for waterlogged block fluid override
        if (otherState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && otherState.getValue(BlockStateProperties.WATERLOGGED)) {
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(otherPos);
            if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                otherFluid = storedFluid;
            }
        }
        
        boolean otherIsDynamic = hotbath$isDynamicCustomFluid(otherFluid);
        
        // If both are dynamic custom fluids, compare customFluidIds
        if (selfIsDynamic && otherIsDynamic && level != null) {
            ResourceLocation selfCustomId = hotbath$getCustomFluidId(level, currentPos);
            ResourceLocation otherCustomId = hotbath$getCustomFluidId(level, otherPos);
            
            // If both have customFluidIds, check if they match
            if (selfCustomId != null && otherCustomId != null) {
                // Hide face only if customFluidIds match (same custom fluid)
                return selfCustomId.equals(otherCustomId);
            }
            // If one or both don't have customFluidId, treat as different fluids (show boundary)
            if (selfCustomId != null || otherCustomId != null) {
                return false;
            }
            // Both null - treat as same fluid (shouldn't happen in practice)
            return true;
        }
        
        // If either is a hotBath fluid but not dynamic, use normal isSame logic
        if (HotbathFluidHelper.isHotbathFluid(selfState.getType()) || HotbathFluidHelper.isHotbathFluid(otherFluid)) {
            return selfState.getType().isSame(otherFluid);
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
