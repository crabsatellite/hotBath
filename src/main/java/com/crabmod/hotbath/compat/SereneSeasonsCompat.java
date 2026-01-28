package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Compatibility layer for Serene Seasons integration.
 * Registers event handlers when Serene Seasons is present.
 */
public class SereneSeasonsCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Initializing Serene Seasons compatibility...");
        NeoForge.EVENT_BUS.register(SereneSeasonsEventHandler.class);
        LOGGER.info("Serene Seasons event handler registered.");
    }
}
