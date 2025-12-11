package com.crabmod.hotbath.compat;

import net.minecraftforge.common.MinecraftForge;

public class ToughAsNailsCompat {
    public static void init() {
        MinecraftForge.EVENT_BUS.register(ToughAsNailsDrinkHandler.class);
        MinecraftForge.EVENT_BUS.register(ToughAsNailsEventHandler.class);
        ToughAsNailsRegistration.init();
    }
}










