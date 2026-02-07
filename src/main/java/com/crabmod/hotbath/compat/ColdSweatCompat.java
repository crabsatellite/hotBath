package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class ColdSweatCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Initializing Cold Sweat compatibility...");
        // API class verification is handled by CompatManager (requiredApiClasses)
        CompatManager.registerEventHandlers("cold_sweat", ColdSweatEventHandler.class);
        LOGGER.info("Cold Sweat event handler registered.");
    }
}
