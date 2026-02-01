package com.crabmod.hotbath.mixin.sodium;

import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for Sodium's BlockOcclusionCache to fix boundary rendering between different dynamic custom fluids.
 * 
 * <p>Problem: All dynamic custom fluids share the same base fluid type (DYNAMIC_FLUID_STILL),
 * so when Sodium checks if two adjacent fluids are the same, it always returns true for any
 * two dynamic custom fluids. This causes the boundary between different custom fluids
 * (e.g., milk_tea and green_tea) to not be rendered.</p>
 * 
 * <p>Solution: Inject into shouldDrawFullBlockFluidSide to check the customFluidId stored in
 * each block's BlockEntity. If the customFluidIds are different, force the face to be rendered.</p>
 */
@Mixin(value = BlockOcclusionCache.class, remap = false)
public class BlockOcclusionCacheMixin {

    /**
     * Mutable position object for neighbor lookup to avoid allocation.
     */
    @Unique
    private final BlockPos.MutableBlockPos hotbath$mutablePos = new BlockPos.MutableBlockPos();

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
     * Get the customFluidId at a position (from BlockEntity or waterlogging storage).
     */
    @Unique
    private static ResourceLocation hotbath$getCustomFluidId(BlockGetter level, BlockPos pos) {
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
     * Inject at the head of shouldDrawFullBlockFluidSide to handle dynamic custom fluid boundaries.
     * If both the current fluid and the adjacent fluid are dynamic custom fluids with different
     * customFluidIds, force the face to be rendered by returning true early.
     */
    @Inject(
        method = "shouldDrawFullBlockFluidSide",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void hotbath$checkDynamicFluidBoundary(
            BlockState selfBlockState,
            BlockGetter view,
            BlockPos selfPos,
            Direction facing,
            FluidState fluid,
            VoxelShape fluidShape,
            CallbackInfoReturnable<Boolean> cir) {

        // Only process dynamic custom fluids
        if (!hotbath$isDynamicCustomFluid(fluid.getType())) {
            return;
        }

        // Get the adjacent block position
        hotbath$mutablePos.set(
            selfPos.getX() + facing.getStepX(),
            selfPos.getY() + facing.getStepY(),
            selfPos.getZ() + facing.getStepZ()
        );

        // Get the adjacent block's fluid state
        BlockState otherState = view.getBlockState(hotbath$mutablePos);
        FluidState otherFluidState = otherState.getFluidState();
        Fluid otherFluid = otherFluidState.getType();

        // Check for waterlogged block fluid override
        if (otherState.hasProperty(BlockStateProperties.WATERLOGGED)
                && otherState.getValue(BlockStateProperties.WATERLOGGED)) {
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(hotbath$mutablePos);
            if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                otherFluid = storedFluid;
            }
        }

        // If the adjacent block also contains a dynamic custom fluid, compare customFluidIds
        if (hotbath$isDynamicCustomFluid(otherFluid)) {
            ResourceLocation selfCustomId = hotbath$getCustomFluidId(view, selfPos);
            ResourceLocation otherCustomId = hotbath$getCustomFluidId(view, hotbath$mutablePos);

            // If both have customFluidIds and they are different, force face to render
            if (selfCustomId != null && otherCustomId != null && !selfCustomId.equals(otherCustomId)) {
                cir.setReturnValue(true);
                return;
            }

            // If one has an ID and the other doesn't, treat as different fluids
            if ((selfCustomId != null) != (otherCustomId != null)) {
                cir.setReturnValue(true);
                return;
            }
        }
        // If adjacent is not a dynamic custom fluid but current is, let normal logic handle it
    }
}
