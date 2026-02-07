package com.crabmod.hotbath.compat;

public class LSOCompat {
    public static void init() {
        CompatManager.registerEventHandlers("legendarysurvivaloverhaul", LSOEventHandler.class, LSOThirstHandler.class);
    }
}










