package com.crabmod.hotbath.compat;

import net.minecraftforge.fml.ModList;

/**
 * Integration with Legendary Survival Overhaul mod
 */
public class LegendarySurvivalOverhaulIntegration {
    private static final String LSO_MOD_ID = "legendarysurvivaloverhaul";

    public static boolean isLSOLoaded() {
        return ModList.get().isLoaded(LSO_MOD_ID);
    }
}










