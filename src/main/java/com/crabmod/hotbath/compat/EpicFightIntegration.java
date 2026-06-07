package com.crabmod.hotbath.compat;

import net.minecraftforge.fml.ModList;

public class EpicFightIntegration {
    private static final String EPICFIGHT_MOD_ID = "epicfight";

    public static boolean isEpicFightLoaded() {
        return ModList.get().isLoaded(EPICFIGHT_MOD_ID);
    }
}
