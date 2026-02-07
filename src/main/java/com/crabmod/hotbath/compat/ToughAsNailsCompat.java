package com.crabmod.hotbath.compat;

public class ToughAsNailsCompat {
    public static void init() {
        CompatManager.registerEventHandlers("toughasnails",
            ToughAsNailsDrinkHandler.class,
            ToughAsNailsEventHandler.class
        );
        ToughAsNailsRegistration.init();
    }
}
