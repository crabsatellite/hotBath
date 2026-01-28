package com.crabmod.hotbath.compat;

import net.neoforged.fml.ModList;

/**
 * Integration with Serene Seasons mod.
 * Provides:
 * - Extra resistance/warmth buff when bathing in winter
 * - Prevents water from freezing near hot bath liquids
 */
public class SereneSeasonsIntegration {
    private static final String SERENE_SEASONS_MOD_ID = "sereneseasons";

    public static boolean isSereneSeasonsLoaded() {
        return ModList.get().isLoaded(SERENE_SEASONS_MOD_ID);
    }
}
