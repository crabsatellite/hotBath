package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

/**
 * Compatibility layer for Alex's Mobs integration.
 * Registers event handlers when Alex's Mobs is present.
 */
public class AlexsMobsCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Initializing Alex's Mobs compatibility...");
        MinecraftForge.EVENT_BUS.register(AlexsMobsEventHandler.class);
        LOGGER.info("Alex's Mobs event handler registered.");
    }
}
