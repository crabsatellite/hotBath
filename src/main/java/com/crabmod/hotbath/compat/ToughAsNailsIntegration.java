package com.crabmod.hotbath.compat;

import net.neoforged.fml.ModList;

/**
 * Integration with Tough As Nails mod
 */
public class ToughAsNailsIntegration {
    private static final String TAN_MOD_ID = "toughasnails";
    
    // Cache the result to avoid repeated ModList lookups
    private static Boolean cachedLoaded = null;

    public static boolean isToughAsNailsLoaded() {
        if (cachedLoaded == null) {
            cachedLoaded = ModList.get().isLoaded(TAN_MOD_ID);
        }
        return cachedLoaded;
    }
}
