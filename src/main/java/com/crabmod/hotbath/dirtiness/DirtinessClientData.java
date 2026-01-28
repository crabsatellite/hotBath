package com.crabmod.hotbath.dirtiness;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for dirtiness data.
 * Stores synced values from server to avoid frequent lookups.
 */
public class DirtinessClientData {
    
    private static final Map<UUID, CachedData> CACHE = new ConcurrentHashMap<>();
    
    private record CachedData(float dirtiness, long dirtSeed, boolean hasFlies) {}
    
    /**
     * Set the cached dirtiness value for a player
     */
    public static void setDirtiness(UUID playerId, float dirtiness, long dirtSeed, boolean hasFlies) {
        CACHE.put(playerId, new CachedData(dirtiness, dirtSeed, hasFlies));
    }
    
    /**
     * Set the cached dirtiness value for a player (backwards compatible)
     */
    public static void setDirtiness(UUID playerId, float dirtiness, long dirtSeed) {
        setDirtiness(playerId, dirtiness, dirtSeed, false);
    }
    
    /**
     * Get the cached dirtiness value for a player
     */
    public static float getDirtiness(UUID playerId) {
        CachedData data = CACHE.get(playerId);
        return data != null ? data.dirtiness : 0.0f;
    }
    
    /**
     * Get the dirt seed for deterministic rendering
     */
    public static long getDirtSeed(UUID playerId) {
        CachedData data = CACHE.get(playerId);
        return data != null ? data.dirtSeed : 0L;
    }
    
    /**
     * Check if player has flies (extremely dirty for 2+ days)
     */
    public static boolean hasFlies(UUID playerId) {
        CachedData data = CACHE.get(playerId);
        return data != null && data.hasFlies;
    }
    
    /**
     * Remove cached data when player logs out
     */
    public static void remove(UUID playerId) {
        CACHE.remove(playerId);
    }
    
    /**
     * Clear all cached data
     */
    public static void clear() {
        CACHE.clear();
    }
}
