package com.crabmod.hotbath.compat;

import net.minecraftforge.common.MinecraftForge;

public class ColdSweatCompat {
    public static void init() {
        MinecraftForge.EVENT_BUS.register(ColdSweatEventHandler.class);
    }
}










