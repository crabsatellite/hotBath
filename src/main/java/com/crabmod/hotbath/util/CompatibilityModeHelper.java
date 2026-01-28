package com.crabmod.hotbath.util;

import com.crabmod.hotbath.HotBathConfig;

/**
 * Helper class for checking compatibility mode in mixins.
 * This class provides a safe way to check if compatibility mode is enabled,
 * even when called early in the mod loading process.
 */
public class CompatibilityModeHelper {
    
    /**
     * Check if compatibility mode is enabled.
     * This method is safe to call from mixins and will return false
     * if the config is not yet loaded.
     * 
     * @return true if compatibility mode is enabled, false otherwise
     */
    public static boolean isCompatibilityModeEnabled() {
        try {
            return HotBathConfig.isCompatibilityModeEnabled();
        } catch (Exception e) {
            // If config is not yet loaded, assume compatibility mode is off
            return false;
        }
    }
}
