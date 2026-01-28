package com.crabmod.hotbath.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HungerRegenHandler {
    // Use memory cache instead of PersistentData for better performance
    // Timer resets when player leaves bath anyway, so no need for persistence
    private static final Map<UUID, Integer> HUNGER_REGEN_TIMERS = new ConcurrentHashMap<>();
    
    public static void regenHunger(
            int regenHungerNumber, float perSecondsNumber, ServerPlayer player) {
        UUID playerUUID = player.getUUID();

        int hungerRegenTimer = HUNGER_REGEN_TIMERS.getOrDefault(playerUUID, 0) + 1;
        HUNGER_REGEN_TIMERS.put(playerUUID, hungerRegenTimer);

        int ticksPerRegen =
                (int) (perSecondsNumber * 20); // Calculate the number of ticks required to recover

        if (hungerRegenTimer >= ticksPerRegen) { // Regenerate based on the given number of seconds
            int currentFoodLevel = player.getFoodData().getFoodLevel();
            int maxFoodLevel = 20;

            if (currentFoodLevel < maxFoodLevel) {
                player.getFoodData().eat(regenHungerNumber, 0);
            }
            HUNGER_REGEN_TIMERS.put(playerUUID, 0);
        }
    }
    
    /**
     * Reset timer when player exits the bath area.
     */
    public static void resetTimer(ServerPlayer player) {
        HUNGER_REGEN_TIMERS.remove(player.getUUID());
    }
    
    /**
     * Clean up player data when they log out.
     */
    public static void cleanup(UUID playerUUID) {
        HUNGER_REGEN_TIMERS.remove(playerUUID);
    }
}










