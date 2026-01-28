package com.crabmod.hotbath.dirtiness;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Calculates dynamic dirtiness factors based on player activity and environment.
 * 
 * Design Goals:
 * - High performance: Only check once per second (20 ticks)
 * - Smart caching: Cache biome checks until player moves significantly
 * - Multiplier system: All factors combine multiplicatively
 * 
 * Factor Categories:
 * 1. Biome (cached) - Desert, Swamp, Jungle, Nether = dirtier
 * 2. Activity (per-check) - Running, Swimming, Mining
 * 3. Combat (event-driven) - Tracked via flags
 * 4. Weather (per-check) - Rain cleans slightly
 * 5. Y-Level (per-check) - Underground = dirtier
 */
public class DirtinessFactors {
    
    // ==================== BIOME MULTIPLIERS ====================
    public static final float BIOME_DESERT = 1.5f;      // Sandy, dusty
    public static final float BIOME_BADLANDS = 1.5f;    // Dusty mesa
    public static final float BIOME_SWAMP = 1.5f;       // Muddy
    public static final float BIOME_MANGROVE = 1.4f;    // Muddy water
    public static final float BIOME_JUNGLE = 1.3f;      // Humid, sticky
    public static final float BIOME_MUSHROOM = 0.8f;    // Magical clean
    public static final float BIOME_NETHER = 1.6f;      // Ash and soot
    public static final float BIOME_DEFAULT = 1.0f;
    
    // ==================== ACTIVITY MULTIPLIERS ====================
    public static final float ACTIVITY_IDLE = 1.0f;
    public static final float ACTIVITY_WALKING = 1.15f;
    public static final float ACTIVITY_SPRINTING = 1.3f;
    public static final float ACTIVITY_SWIMMING = 0.7f;  // Water cleans a bit
    public static final float ACTIVITY_CRAWLING = 1.4f;  // Crawling in dirt
    public static final float ACTIVITY_FLYING = 0.9f;    // Elytra, clean air
    
    // ==================== COMBAT MULTIPLIERS ====================
    public static final float COMBAT_ACTIVE = 1.5f;     // Recently in combat
    public static final float COMBAT_NONE = 1.0f;
    
    // ==================== ENVIRONMENT MULTIPLIERS ====================
    public static final float WEATHER_RAIN = 0.6f;      // Rain washes
    public static final float WEATHER_THUNDER = 0.5f;   // Heavy rain
    public static final float WEATHER_CLEAR = 1.0f;
    
    public static final float UNDERGROUND_DEEP = 1.4f;  // Y < 0 (deep caves)
    public static final float UNDERGROUND_CAVE = 1.2f;  // Y < 50
    public static final float SURFACE = 1.0f;
    
    // ==================== SPECIAL SURFACE MULTIPLIERS ====================
    public static final float ON_DIRT_BLOCK = 1.2f;     // Standing on dirt/mud
    public static final float ON_SAND_BLOCK = 1.15f;    // Standing on sand
    public static final float ON_CLEAN_BLOCK = 1.0f;
    
    // ==================== INSTANT DIRT EVENTS (percentage added) ====================
    public static final float EVENT_HURT = 0.02f;       // 2% when hurt
    public static final float EVENT_KILL_MOB = 0.03f;   // 3% when killing mob
    public static final float EVENT_KILL_BOSS = 0.08f;  // 8% when killing boss
    public static final float EVENT_EXPLOSION = 0.05f;  // 5% near explosion
    public static final float EVENT_MINING = 0.005f;    // 0.5% per block mined
    
    // ==================== CACHE SETTINGS ====================
    private static final int BIOME_CACHE_DISTANCE_SQ = 64; // Re-check biome after moving 8 blocks
    
    // Per-player cached data (stored in PlayerDirtinessTracker)
    private BlockPos lastBiomeCheckPos = null;
    private float cachedBiomeMultiplier = BIOME_DEFAULT;
    
    // Combat tracking
    private long lastCombatTime = 0;
    private static final int COMBAT_COOLDOWN = 100; // 5 seconds
    
    // Movement tracking
    private double lastX, lastY, lastZ;
    private boolean initialized = false;
    
    /**
     * Calculate total dirt multiplier for current tick.
     * Called once per second for performance.
     * 
     * @return Combined multiplier (typically 0.5 - 2.5)
     */
    public float calculateMultiplier(ServerPlayer player, long gameTime) {
        float multiplier = 1.0f;
        
        // 1. Biome factor (cached)
        multiplier *= getBiomeMultiplier(player);
        
        // 2. Activity factor
        multiplier *= getActivityMultiplier(player);
        
        // 3. Combat factor
        multiplier *= getCombatMultiplier(gameTime);
        
        // 4. Weather factor
        multiplier *= getWeatherMultiplier(player);
        
        // 5. Y-Level factor
        multiplier *= getDepthMultiplier(player);
        
        // 6. Ground block factor
        multiplier *= getGroundMultiplier(player);
        
        return multiplier;
    }
    
    /**
     * Get biome-based multiplier with caching.
     */
    private float getBiomeMultiplier(ServerPlayer player) {
        BlockPos currentPos = player.blockPosition();
        
        // Check if we need to update cache
        if (lastBiomeCheckPos == null || 
            currentPos.distSqr(lastBiomeCheckPos) > BIOME_CACHE_DISTANCE_SQ) {
            
            lastBiomeCheckPos = currentPos;
            cachedBiomeMultiplier = calculateBiomeMultiplier(player);
        }
        
        return cachedBiomeMultiplier;
    }
    
    private float calculateBiomeMultiplier(ServerPlayer player) {
        Level level = player.level();
        Holder<Biome> biomeHolder = level.getBiome(player.blockPosition());
        
        // Check dimension first
        if (level.dimension() == Level.NETHER) {
            return BIOME_NETHER;
        }
        
        // Check biome tags
        if (biomeHolder.is(BiomeTags.IS_BADLANDS)) {
            return BIOME_BADLANDS;
        }
        if (biomeHolder.is(BiomeTags.HAS_DESERT_PYRAMID) || 
            biomeHolder.value().getBaseTemperature() > 1.5f) {
            return BIOME_DESERT;
        }
        if (biomeHolder.is(BiomeTags.HAS_SWAMP_HUT)) {
            return BIOME_SWAMP;
        }
        if (biomeHolder.is(BiomeTags.HAS_JUNGLE_TEMPLE) || 
            biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return BIOME_JUNGLE;
        }
        if (biomeHolder.is(BiomeTags.IS_MOUNTAIN)) {
            return 0.95f; // Slightly cleaner in mountains
        }
        if (biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_RIVER)) {
            return 0.85f; // Near water = cleaner
        }
        
        return BIOME_DEFAULT;
    }
    
    /**
     * Get activity-based multiplier.
     */
    private float getActivityMultiplier(ServerPlayer player) {
        // Initialize tracking
        if (!initialized) {
            lastX = player.getX();
            lastY = player.getY();
            lastZ = player.getZ();
            initialized = true;
            return ACTIVITY_IDLE;
        }
        
        // Calculate movement
        double dx = player.getX() - lastX;
        double dy = player.getY() - lastY;
        double dz = player.getZ() - lastZ;
        double horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
        
        // Update last position
        lastX = player.getX();
        lastY = player.getY();
        lastZ = player.getZ();
        
        // Check special states first
        if (player.isFallFlying()) {
            return ACTIVITY_FLYING;
        }
        if (player.isSwimming() || player.isInWater()) {
            return ACTIVITY_SWIMMING;
        }
        if (player.isShiftKeyDown() && horizontalSpeed > 0.01) {
            return ACTIVITY_CRAWLING;
        }
        if (player.isSprinting() && horizontalSpeed > 0.15) {
            return ACTIVITY_SPRINTING;
        }
        if (horizontalSpeed > 0.05) {
            return ACTIVITY_WALKING;
        }
        
        return ACTIVITY_IDLE;
    }
    
    /**
     * Get combat-based multiplier.
     */
    private float getCombatMultiplier(long gameTime) {
        if (gameTime - lastCombatTime < COMBAT_COOLDOWN) {
            return COMBAT_ACTIVE;
        }
        return COMBAT_NONE;
    }
    
    /**
     * Mark player as in combat.
     */
    public void markCombat(long gameTime) {
        this.lastCombatTime = gameTime;
    }
    
    /**
     * Get weather-based multiplier.
     */
    private float getWeatherMultiplier(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        
        // Only affects if player can see sky
        if (!level.canSeeSky(pos)) {
            return WEATHER_CLEAR;
        }
        
        if (level.isThundering()) {
            return WEATHER_THUNDER;
        }
        if (level.isRaining()) {
            return WEATHER_RAIN;
        }
        
        return WEATHER_CLEAR;
    }
    
    /**
     * Get depth-based multiplier.
     */
    private float getDepthMultiplier(ServerPlayer player) {
        int y = player.blockPosition().getY();
        
        if (y < 0) {
            return UNDERGROUND_DEEP;
        }
        if (y < 50 && !player.level().canSeeSky(player.blockPosition())) {
            return UNDERGROUND_CAVE;
        }
        
        return SURFACE;
    }
    
    /**
     * Get multiplier based on block player is standing on.
     */
    private float getGroundMultiplier(ServerPlayer player) {
        BlockPos belowPos = player.blockPosition().below();
        BlockState blockBelow = player.level().getBlockState(belowPos);
        
        if (blockBelow.is(BlockTags.DIRT) || blockBelow.is(BlockTags.SOUL_SPEED_BLOCKS)) {
            return ON_DIRT_BLOCK;
        }
        if (blockBelow.is(BlockTags.SAND)) {
            return ON_SAND_BLOCK;
        }
        
        return ON_CLEAN_BLOCK;
    }
    
    /**
     * Reset all cached data.
     */
    public void reset() {
        lastBiomeCheckPos = null;
        cachedBiomeMultiplier = BIOME_DEFAULT;
        lastCombatTime = 0;
        initialized = false;
    }
    
    // ==================== STATIC GETTERS FOR INSTANT EVENTS ====================
    
    /**
     * Get instant dirt percentage when player is hurt
     */
    public static float getInstantHurtPercent() {
        return EVENT_HURT;
    }
    
    /**
     * Get instant dirt percentage when player kills a mob
     */
    public static float getInstantKillPercent() {
        return EVENT_KILL_MOB;
    }
    
    /**
     * Get instant dirt percentage when player kills a boss
     */
    public static float getInstantBossKillPercent() {
        return EVENT_KILL_BOSS;
    }
    
    /**
     * Get instant dirt percentage when player is near an explosion
     */
    public static float getInstantExplosionPercent() {
        return EVENT_EXPLOSION;
    }
    
    /**
     * Get instant dirt percentage per block mined
     */
    public static float getInstantMiningPercent() {
        return EVENT_MINING;
    }
}
