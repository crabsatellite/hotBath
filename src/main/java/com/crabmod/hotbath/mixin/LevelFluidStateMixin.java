package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to override getFluidState to return the correct fluid type for hotBath fluids.
 * This ensures that waterlogged blocks display the correct fluid texture.
 * 
 * <p>IMPORTANT: Always returns the SOURCE state of the fluid. This is critical because:
 * <ul>
 *   <li>Waterlogged blocks should always appear to contain full (source-level) fluid</li>
 *   <li>FlowingFluid.tick() checks isSource() to determine if the block should be removed</li>
 *   <li>If we return a flowing state, tick() may try to replace the waterlogged block with air</li>
 * </ul>
 * </p>
 * 
 * <p>Performance optimization: We inject at RETURN instead of HEAD to avoid calling
 * getBlockState twice. We can check the returned FluidState to see if it's water,
 * and only then do the expensive lookup.</p>
 */
@Mixin(Level.class)
public abstract class LevelFluidStateMixin {

    /**
     * Inject at RETURN of getFluidState to replace water with hotBath fluid if stored.
     * 
     * <p>Performance: By injecting at RETURN, we avoid calling getBlockState again.
     * We only do the expensive cache lookup if the returned state is water AND 
     * we have a cached entry for this position.</p>
     */
    @Inject(method = "getFluidState", at = @At("RETURN"), cancellable = true)
    private void hotbath$getFluidState(BlockPos pos, CallbackInfoReturnable<FluidState> cir) {
        FluidState originalState = cir.getReturnValue();
        
        // Quick exit: Only process if the original state is water
        // This avoids expensive lookups for air, lava, or other fluids
        if (originalState.isEmpty() || originalState.getType() != Fluids.WATER) {
            return;
        }
        
        // Now check if we have a stored hotBath fluid for this position
        // This uses ConcurrentHashMap.get() which is O(1)
        Level level = (Level) (Object) this;
        Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
        
        // Only handle hotBath fluids specifically
        if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
            // Return the SOURCE state of the stored fluid
            FluidState sourceState = hotbath$getSourceFluidState(storedFluid);
            cir.setReturnValue(sourceState);
        }
    }
    
    /**
     * Get the source FluidState for a fluid.
     * For FlowingFluid, this returns getSource(false) state.
     * For other fluids, returns defaultFluidState().
     */
    @Unique
    private FluidState hotbath$getSourceFluidState(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowingFluid) {
            return flowingFluid.getSource(false);
        }
        return fluid.defaultFluidState();
    }
}
