package com.crabmod.hotbath.compat;

import net.minecraftforge.fml.ModList;

/**
 * Integration with Alex's Caves mod.
 * Provides interactions between hot bath fluids and Alex's Caves entities:
 * - GummyBear takes damage in hot water (melting!)
 * - Herbal bath can cure the IRRADIATED effect
 * - Gammaroach is attracted to dirty players (like cockroaches)
 * - Raycat sits near hot springs (like cats on chests)
 */
public class AlexsCavesIntegration {
    private static final String ALEXSCAVES_MOD_ID = "alexscaves";

    public static boolean isAlexsCavesLoaded() {
        return ModList.get().isLoaded(ALEXSCAVES_MOD_ID);
    }
}
