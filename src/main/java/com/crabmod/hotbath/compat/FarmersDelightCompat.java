package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

/**
 * Compatibility layer for Farmer's Delight integration.
 * Registers event handlers when Farmer's Delight is present.
 */
public class FarmersDelightCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Initializing Farmer's Delight compatibility...");
        MinecraftForge.EVENT_BUS.register(FarmersDelightEventHandler.class);
        LOGGER.info("Farmer's Delight event handler registered.");
    }
}
