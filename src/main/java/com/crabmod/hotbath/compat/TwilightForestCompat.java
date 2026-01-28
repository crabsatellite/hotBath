package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

/**
 * Compatibility layer for Twilight Forest integration.
 * Registers event handlers when Twilight Forest is present.
 */
public class TwilightForestCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Initializing Twilight Forest compatibility...");
        MinecraftForge.EVENT_BUS.register(TwilightForestEventHandler.class);
        LOGGER.info("Twilight Forest event handler registered.");
    }
}
