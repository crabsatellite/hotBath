package com.crabmod.hotbath.compat;

import net.minecraftforge.fml.ModList;

/**
 * Integration with Legendary Survival Overhaul mod
 */
public class LegendarySurvivalOverhaulIntegration {
    private static final String LSO_MOD_ID = "legendarysurvivaloverhaul";
    
    // Cache the result to avoid repeated ModList lookups
    private static Boolean cachedLoaded = null;

    public static boolean isLSOLoaded() {
        if (cachedLoaded == null) {
            cachedLoaded = ModList.get().isLoaded(LSO_MOD_ID);
        }
        return cachedLoaded;
    }
}










