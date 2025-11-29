package com.crabmod.hotbath.compat;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.loading.FMLEnvironment;

public class LSOCompat {
    public static void init() {
        NeoForge.EVENT_BUS.register(LSOEventHandler.class);
        NeoForge.EVENT_BUS.register(LSOThirstHandler.class);
        
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(LSOThirstTooltipHandler.class);
        }
    }
}
