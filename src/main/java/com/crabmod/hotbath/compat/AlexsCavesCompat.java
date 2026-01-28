package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Compatibility layer for Alex's Caves integration.
 * Registers event handlers when Alex's Caves is present.
 */
public class AlexsCavesCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Initializing Alex's Caves compatibility...");
        NeoForge.EVENT_BUS.register(AlexsCavesEventHandler.class);
        LOGGER.info("Alex's Caves event handler registered.");
    }
}
