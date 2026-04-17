package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

public class EpicFightCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        LOGGER.info("Initializing Epic Fight compatibility...");

        if (!FMLEnvironment.dist.isClient()) {
            LOGGER.info("Skipping Epic Fight compat on dedicated server (client-only rendering).");
            return;
        }

        EpicFightClientHelper.registerLayers();
        LOGGER.info("Epic Fight compatibility initialized.");
    }
}
