package com.crabmod.hotbath.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HealthRegenHandler {
    // Use memory cache instead of PersistentData for better performance
    // Timer resets when player leaves bath anyway, so no need for persistence
    private static final Map<UUID, Integer> HEALTH_REGEN_TIMERS = new ConcurrentHashMap<>();
    
    public static void regenHealth(
            float regenHealthNumber, double perSecondsNumber, ServerPlayer player) {

        UUID playerUUID = player.getUUID();

        int healthRegenTimer = HEALTH_REGEN_TIMERS.getOrDefault(playerUUID, 0) + 1;
        HEALTH_REGEN_TIMERS.put(playerUUID, healthRegenTimer);

        if (healthRegenTimer >= 20 * perSecondsNumber) {
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();

            if (currentHealth < maxHealth) {
                player.setHealth(Math.min(currentHealth + regenHealthNumber, maxHealth));
            }
            HEALTH_REGEN_TIMERS.put(playerUUID, 0);
        }
    }
    
    /**
     * Reset timer when player exits the bath area.
     * Should be called when player is no longer in bath to keep memory clean.
     */
    public static void resetTimer(ServerPlayer player) {
        HEALTH_REGEN_TIMERS.remove(player.getUUID());
    }
    
    /**
     * Clean up player data when they log out.
     */
    public static void cleanup(UUID playerUUID) {
        HEALTH_REGEN_TIMERS.remove(playerUUID);
    }
}
