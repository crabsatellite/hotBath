package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Compatibility layer for Create mod integration.
 * Checks if Create is loaded and initializes integration features.
 */
public class CreateCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String CREATE_MOD_ID = "create";
    
    private static boolean createLoaded = false;
    
    /**
     * Check if Create mod is loaded.
     */
    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded(CREATE_MOD_ID);
    }

    public static boolean isIntegrationActive() {
        return createLoaded;
    }
    
    /**
     * Initialize Create compatibility.
     * Should be called during mod setup.
     */
    public static void init() {
        createLoaded = ModList.get().isLoaded(CREATE_MOD_ID);
        
        if (createLoaded) {
            LOGGER.info("Create mod detected! Initializing Hot Bath integration...");
            try {
                CreateIntegration.init();
                LOGGER.info("Create integration initialized successfully.");
            } catch (Throwable e) {
                LOGGER.error("Failed to initialize Create integration", e);
                createLoaded = false;
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (e instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException("Failed to initialize Create integration", e);
            }
        } else {
            LOGGER.debug("Create mod not detected, skipping integration.");
        }
    }
}
