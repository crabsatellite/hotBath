package com.crabmod.hotbath.compat;

import net.neoforged.neoforge.common.NeoForge;

public class ToughAsNailsCompat {
    public static void init() {
        NeoForge.EVENT_BUS.register(ToughAsNailsDrinkHandler.class);
        NeoForge.EVENT_BUS.register(ToughAsNailsEventHandler.class);
        ToughAsNailsRegistration.init();
    }
}
