package com.crabmod.hotbath.compat;

import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

/**
 * Integration with Twilight Forest mod.
 * Provides:
 * - Frost effect removal when bathing in hot water
 * - Frost resistance buff after bathing
 * - Firefly particle effects around bath pools in Twilight Forest dimension
 * - Ice mob damage when in hot bath
 */
public class TwilightForestIntegration {
    private static final String TWILIGHT_FOREST_MOD_ID = "twilightforest";
    
    // Cached state to avoid repeated lookups in hot paths
    private static Boolean cachedIsLoaded = null;
    private static volatile boolean compatDisabled = false;

    public static boolean isTwilightForestLoaded() {
        if (cachedIsLoaded == null) {
            cachedIsLoaded = ModList.get().isLoaded(TWILIGHT_FOREST_MOD_ID);
        }
        return cachedIsLoaded;
    }
    
    /**
     * Check if an entity is a Twilight Forest ice mob that should take damage in hot bath.
     * This includes: IceCrystal, StableIceCore, UnstableIceCore, SnowGuardian, and SnowQueen.
     * 
     * Performance optimized: uses cached state check instead of CompatManager for hot path.
     * 
     * @param entity The entity to check
     * @return true if the entity is an ice mob
     */
    public static boolean isTwilightForestIceMob(Entity entity) {
        // Fast path: if not loaded or disabled, return immediately
        if (!isTwilightForestLoaded() || compatDisabled) {
            return false;
        }
        
        try {
            return TwilightForestEventHandler.isIceMob(entity);
        } catch (Throwable e) {
            // Disable on first error and notify CompatManager
            compatDisabled = true;
            CompatManager.reportRuntimeError(TWILIGHT_FOREST_MOD_ID, "isIceMob check", e);
            return false;
        }
    }
}
