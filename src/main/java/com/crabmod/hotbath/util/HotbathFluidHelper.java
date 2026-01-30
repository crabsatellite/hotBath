package com.crabmod.hotbath.util;

import com.crabmod.hotbath.registers.FluidsRegister;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Helper class to identify hotBath mod fluids.
 * 
 * <p>This reduces invasiveness by only affecting hotBath's own fluids,
 * not other mods' water-like fluids.</p>
 */
public class HotbathFluidHelper {
    
    /**
     * Check if a fluid is a hotBath mod fluid (source or flowing).
     * 
     * @param fluid The fluid to check
     * @return true if this is a hotBath mod fluid
     */
    public static boolean isHotbathFluid(Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        
        // Get source fluid for comparison
        Fluid sourceFluid = getSourceFluid(fluid);
        
        // Check against all hotBath fluids
        return sourceFluid == FluidsRegister.HOT_WATER_FLUID.get()
                || sourceFluid == FluidsRegister.HONEY_BATH_FLUID.get()
                || sourceFluid == FluidsRegister.MILK_BATH_FLUID.get()
                || sourceFluid == FluidsRegister.HERBAL_BATH_FLUID.get()
                || sourceFluid == FluidsRegister.PEONY_BATH_FLUID.get()
                || sourceFluid == FluidsRegister.ROSE_BATH_FLUID.get();
    }
    
    /**
     * Get the source fluid from a potentially flowing fluid.
     */
    public static Fluid getSourceFluid(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowingFluid) {
            return flowingFluid.getSource();
        }
        return fluid;
    }
}
