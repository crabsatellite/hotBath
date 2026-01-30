package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to fix fluid behavior with waterlogged blocks containing hotBath fluids.
 * 
 * <p>Problem: When a fluid ticks inside a waterlogged block (like stairs), the vanilla
 * FlowingFluid.tick() method can replace the entire block with air or a fluid block
 * if it determines the fluid should disappear or change. This destroys the waterlogged
 * block (e.g., stairs disappear).</p>
 * 
 * <p>Solution: Intercept the tick method and handle waterlogged blocks specially,
 * but ONLY for blocks that contain hotBath fluids (not vanilla water).</p>
 * 
 * <p>Non-invasive design: Only affects blocks with stored hotBath fluid types,
 * vanilla water behavior is completely untouched.</p>
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    /**
     * Intercept the tick method to prevent destruction of waterlogged blocks
     * that contain hotBath fluids.
     * 
     * <p>NON-INVASIVE: Only intercepts when the block has a stored hotBath fluid.
     * Vanilla water in waterlogged blocks is handled normally by vanilla code.</p>
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void hotbath$onTick(Level level, BlockPos pos, FluidState state, CallbackInfo ci) {
        BlockState blockState = level.getBlockState(pos);
        
        // Quick exit: Not a waterlogged block
        if (!blockState.hasProperty(BlockStateProperties.WATERLOGGED) 
                || !blockState.getValue(BlockStateProperties.WATERLOGGED)) {
            return;
        }
        
        // NON-INVASIVE CHECK: Only handle if this block has a stored hotBath fluid
        // Vanilla water in waterlogged blocks will pass through and use vanilla logic
        Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
        if (storedFluid == null || !HotbathFluidHelper.isHotbathFluid(storedFluid)) {
            // No hotBath fluid stored - let vanilla handle it
            return;
        }
        
        // This is a waterlogged block with a hotBath fluid
        // Handle the tick specially to avoid destroying the block
        FlowingFluid fluid = (FlowingFluid) (Object) this;
        
        // Verify the ticking fluid matches the stored fluid
        if (!fluid.isSame(storedFluid)) {
            // Different fluid is ticking - this shouldn't happen normally
            // Let vanilla handle it to avoid unexpected behavior
            return;
        }
        
        // Check if the fluid should remain or be removed
        boolean hasAdjacentSource = hotbath$hasAdjacentSourceOrAbove(fluid, level, pos);
        
        if (!hasAdjacentSource) {
            // The fluid should disappear - just set WATERLOGGED to false
            // DON'T replace the block with air!
            if (!level.isClientSide()) {
                level.setBlock(pos, blockState.setValue(BlockStateProperties.WATERLOGGED, false), 3);
                HotbathWaterloggingHelper.removeFluidType(level, pos);
            }
            ci.cancel();
            return;
        }
        
        // Fluid should stay - for waterlogged blocks, source state is always maintained
        // Schedule next tick and let spread happen (spread() is safe due to our spreadTo hook)
        // Don't cancel - let vanilla's spread() run at the end of tick()
    }
    
    /**
     * Check if there are adjacent source blocks or fluid above that would sustain this fluid.
     * Simplified check - we only need to know if fluid should stay or drain.
     */
    @Unique
    private boolean hotbath$hasAdjacentSourceOrAbove(FlowingFluid fluid, Level level, BlockPos pos) {
        // Check above first (most common case for flowing water)
        FluidState aboveFluid = level.getFluidState(pos.above());
        if (aboveFluid.getType().isSame(fluid)) {
            return true;
        }
        
        // Check horizontal neighbors for source blocks
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            FluidState adjacentFluid = level.getFluidState(pos.relative(direction));
            if (adjacentFluid.getType().isSame(fluid) && adjacentFluid.isSource()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get the source version of a fluid.
     */
    @Unique
    private Fluid hotbath$getSourceFluid(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowingFluid) {
            return flowingFluid.getSource();
        }
        return fluid;
    }
    
    /**
     * Intercept spreadTo to prevent it from destroying waterlogged blocks
     * that already contain hotBath fluids.
     * 
     * <p>NON-INVASIVE: Only intercepts when the target block has a stored hotBath fluid.
     * Vanilla water spreading is handled normally.</p>
     */
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void hotbath$onSpreadTo(LevelAccessor level, BlockPos pos, BlockState blockState, 
                                     Direction direction, FluidState fluidState, CallbackInfo ci) {
        // Quick exit: Not a waterlogged block
        if (!blockState.hasProperty(BlockStateProperties.WATERLOGGED) 
                || !blockState.getValue(BlockStateProperties.WATERLOGGED)) {
            return;
        }
        
        // NON-INVASIVE CHECK: Only handle if this block has a stored hotBath fluid
        Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
        if (storedFluid == null || !HotbathFluidHelper.isHotbathFluid(storedFluid)) {
            // No hotBath fluid stored - let vanilla handle it
            return;
        }
        
        // Block already has a hotBath fluid - prevent any spread operation
        // that might try to replace or destroy the block
        
        // If the spreading fluid is also a hotBath fluid, handle it specially
        if (HotbathFluidHelper.isHotbathFluid(fluidState.getType())) {
            FlowingFluid spreadingFluid = (FlowingFluid) fluidState.getType();
            if (spreadingFluid.isSame(storedFluid)) {
                // Same fluid type - nothing to do, block is already waterlogged
                ci.cancel();
                return;
            }
            
            // Different hotBath fluid trying to spread in
            // Update the stored fluid type to the new one (source version)
            if (!level.isClientSide()) {
                Fluid sourceFluid = HotbathFluidHelper.getSourceFluid(fluidState.getType());
                HotbathWaterloggingHelper.storeFluidType(level, pos, sourceFluid);
            }
            ci.cancel();
        }
        // If it's not a hotBath fluid (vanilla water, lava, etc.), let vanilla handle it
    }
}
