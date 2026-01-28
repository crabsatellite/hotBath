package com.crabmod.hotbath.compat;

import net.neoforged.fml.ModList;

/**
 * Integration with Alex's Mobs mod.
 * Provides fly/mosquito/cockroach attraction mechanics for dirty players.
 */
public class AlexsMobsIntegration {
    private static final String ALEXSMOBS_MOD_ID = "alexsmobs";

    public static boolean isAlexsMobsLoaded() {
        return ModList.get().isLoaded(ALEXSMOBS_MOD_ID);
    }
}
