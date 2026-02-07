package com.crabmod.hotbath.compat;

import net.minecraftforge.common.MinecraftForge;

public class ToughAsNailsCompat {
    public static void init() {
        CompatManager.registerEventHandlers("toughasnails", ToughAsNailsDrinkHandler.class, ToughAsNailsEventHandler.class);
        ToughAsNailsRegistration.init();
    }
}










