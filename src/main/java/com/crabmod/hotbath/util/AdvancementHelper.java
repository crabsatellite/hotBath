package com.crabmod.hotbath.util;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class to cache advancement completion status and avoid redundant checks.
 * The award() method is idempotent in Minecraft, but we can avoid the lookup overhead.
 * 
 * Note: The cache only stores positive completions. If an advancement is revoked,
 * we always check the actual game state before caching.
 */
public class AdvancementHelper {
    
    // Two-level cache: UUID -> Set of earned advancement IDs
    // This allows O(1) cleanup when player logs out
    private static final Map<UUID, Set<String>> ADVANCEMENT_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Try to award an advancement to a player, with caching to avoid redundant checks.
     * 
     * @param player The player to award the advancement to
     * @param advancementId The advancement ID (e.g., "hotbath:foot_health")
     * @param criterionKey The criterion key to trigger (e.g., "code_triggered")
     * @return true if the advancement was newly awarded, false if already had it or failed
     */
    public static boolean tryAwardAdvancement(ServerPlayer player, String advancementId, String criterionKey) {
        UUID playerUUID = player.getUUID();
        
        // Check cache first for fast path - but verify with game state to handle revokes
        Set<String> playerCache = ADVANCEMENT_CACHE.get(playerUUID);
        boolean cachedAsCompleted = playerCache != null && playerCache.contains(advancementId);
        
        // Get the advancement
        AdvancementHolder advancement = player.getServer()
                .getAdvancements()
                .get(ResourceLocation.tryParse(advancementId));
        
        if (advancement == null) {
            return false;
        }
        
        // Check actual game state
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            // Already completed in game, ensure cache is updated
            if (!cachedAsCompleted) {
                addToCache(playerUUID, advancementId);
            }
            return false;
        }
        
        // Not completed in game - if it was cached, the advancement was revoked, so remove from cache
        if (cachedAsCompleted) {
            playerCache.remove(advancementId);
        }
        
        // Award the advancement
        boolean awarded = player.getAdvancements().award(advancement, criterionKey);
        if (awarded) {
            addToCache(playerUUID, advancementId);
        }
        
        return awarded;
    }
    
    private static void addToCache(UUID playerUUID, String advancementId) {
        ADVANCEMENT_CACHE.computeIfAbsent(playerUUID, k -> ConcurrentHashMap.newKeySet())
                .add(advancementId);
    }
    
    /**
     * Clean up cache for a player when they log out.
     * O(1) operation using two-level map structure.
     */
    public static void cleanup(UUID playerUUID) {
        ADVANCEMENT_CACHE.remove(playerUUID);
    }
}
