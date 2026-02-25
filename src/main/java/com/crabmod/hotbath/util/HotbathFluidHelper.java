package com.crabmod.hotbath.util;

import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.registers.FluidsRegister;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class to identify hotBath mod fluids.
 * 
 * <p>This reduces invasiveness by only affecting hotBath's own fluids,
 * not other mods' water-like fluids.</p>
 * 
 * <p>Performance: Uses a cached Set for O(1) lookup instead of multiple == comparisons.
 * The cache is lazily initialized and thread-safe.</p>
 */
public class HotbathFluidHelper {
    
    /**
     * Cached set of all hotBath fluid instances (both source and flowing).
     * Uses ConcurrentHashMap.newKeySet() for thread-safe lazy initialization.
     */
    private static volatile Set<Fluid> hotbathFluidCache = null;
    
    /**
     * Check if a fluid is a hotBath mod fluid (source or flowing).
     * 
     * <p>Performance optimized: Uses cached Set lookup O(1) instead of 
     * multiple == comparisons.</p>
     * 
     * @param fluid The fluid to check
     * @return true if this is a hotBath mod fluid
     */
    public static boolean isHotbathFluid(Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        
        // Use cached set for fast lookup
        Set<Fluid> cache = getOrBuildCache();
        return cache.contains(fluid);
    }
    
    /**
     * Get or build the hotBath fluid cache.
     * Uses double-checked locking for thread-safe lazy initialization.
     */
    private static Set<Fluid> getOrBuildCache() {
        Set<Fluid> cache = hotbathFluidCache;
        if (cache == null) {
            synchronized (HotbathFluidHelper.class) {
                cache = hotbathFluidCache;
                if (cache == null) {
                    cache = buildFluidCache();
                    hotbathFluidCache = cache;
                }
            }
        }
        return cache;
    }
    
    /**
     * Build the fluid cache containing all hotBath fluids (source and flowing).
     */
    private static Set<Fluid> buildFluidCache() {
        Set<Fluid> cache = ConcurrentHashMap.newKeySet();
        
        // Add built-in fluids (both source and flowing)
        cache.add(FluidsRegister.HOT_WATER_FLUID.get());
        cache.add(FluidsRegister.HOT_WATER_FLOWING.get());
        cache.add(FluidsRegister.HONEY_BATH_FLUID.get());
        cache.add(FluidsRegister.HONEY_BATH_FLOWING.get());
        cache.add(FluidsRegister.MILK_BATH_FLUID.get());
        cache.add(FluidsRegister.MILK_BATH_FLOWING.get());
        cache.add(FluidsRegister.HERBAL_BATH_FLUID.get());
        cache.add(FluidsRegister.HERBAL_BATH_FLOWING.get());
        cache.add(FluidsRegister.PEONY_BATH_FLUID.get());
        cache.add(FluidsRegister.PEONY_BATH_FLOWING.get());
        cache.add(FluidsRegister.ROSE_BATH_FLUID.get());
        cache.add(FluidsRegister.ROSE_BATH_FLOWING.get());

        // Add dynamic custom fluids
        cache.add(DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get());
        cache.add(DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get());

        return cache;
    }
    
    /**
     * Invalidate the cache. Call this if fluids are dynamically registered/unregistered.
     * Not typically needed for normal operation.
     */
    public static void invalidateCache() {
        hotbathFluidCache = null;
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
