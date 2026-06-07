package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

public class EpicFightCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void init(IEventBus modEventBus) {
        LOGGER.info("Initializing Epic Fight compatibility...");

        if (FMLEnvironment.dist != Dist.CLIENT) {
            LOGGER.info("Skipping Epic Fight compat on dedicated server (client-only rendering).");
            return;
        }

        EpicFightClientHelper.registerLayers(modEventBus);
        LOGGER.info("Epic Fight compatibility initialized.");
    }
}
