package com.crabmod.hotbath.compat;

import net.neoforged.fml.ModList;

/**
 * Integration with Farmer's Delight mod.
 * Provides:
 * - Comfort effect when bathing in hot water (slow regeneration)
 */
public class FarmersDelightIntegration {
    private static final String FARMERS_DELIGHT_MOD_ID = "farmersdelight";

    public static boolean isFarmersDelightLoaded() {
        return ModList.get().isLoaded(FARMERS_DELIGHT_MOD_ID);
    }
}
