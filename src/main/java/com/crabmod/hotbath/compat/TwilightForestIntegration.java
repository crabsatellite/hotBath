package com.crabmod.hotbath.compat;

import net.minecraftforge.fml.ModList;

/**
 * Integration with Twilight Forest mod.
 * Provides:
 * - Frost effect removal when bathing in hot water
 * - Frost resistance buff after bathing
 * - Firefly particle effects around bath pools in Twilight Forest dimension
 */
public class TwilightForestIntegration {
    private static final String TWILIGHT_FOREST_MOD_ID = "twilightforest";

    public static boolean isTwilightForestLoaded() {
        return ModList.get().isLoaded(TWILIGHT_FOREST_MOD_ID);
    }
}
