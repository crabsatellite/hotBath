package com.crabmod.hotbath.compat;

import net.minecraftforge.common.MinecraftForge;

public class LSOCompat {
    public static void init() {
        MinecraftForge.EVENT_BUS.register(LSOEventHandler.class);
        MinecraftForge.EVENT_BUS.register(LSOThirstHandler.class);
    }
}










