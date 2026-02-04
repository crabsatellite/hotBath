package com.crabmod.hotbath.dirtiness;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Data class that stores player dirtiness information.
 * 
 * Design: 
 * - Uses accumulated "dirt points" for dynamic dirtiness calculation
 * - Base rate: 5 game days (120,000 ticks) = 100% dirty with multiplier 1.0
 * - Environmental/activity factors modify the accumulation rate
 * - Dirtiness is calculated on-demand for performance
 * - Uses deterministic seed based on player UUID for consistent dirt spot placement
 * - Gradual bathing: standing in hot water gradually cleans you
 */
public class DirtinessData implements INBTSerializable<CompoundTag> {
    
    /** Accumulated dirt points (0 = clean, POINTS_TO_MAX_DIRTY = 100% dirty) */
    private float accumulatedDirt = 0;
    
    /** Seed for deterministic dirt spot generation (derived from player UUID) */
    private long dirtSeed = 0;
    
    /** Whether the player has been initialized */
    private boolean initialized = false;
    
    /** Game time when player first reached 100% dirtiness (0 = not at max) */
    private long maxDirtyStartTime = 0;
    
    // ==================== CONSTANTS ====================
    
    // Base accumulation: 5 game days to reach 100% with multiplier 1.0
    public static final int TICKS_PER_DAY = 24000;
    public static final int DAYS_TO_MAX_DIRTY = 5;
    public static final float POINTS_TO_MAX_DIRTY = 100.0f;
    
    // Base dirt points per tick (at multiplier 1.0)
    // 100 points / (5 days * 24000 ticks) = 0.000833... points/tick
    public static final float BASE_DIRT_PER_TICK = POINTS_TO_MAX_DIRTY / (TICKS_PER_DAY * DAYS_TO_MAX_DIRTY);
    
    // Bathing speed: 20 seconds (400 ticks) standing still = fully clean from 100% dirty
    public static final int TICKS_TO_CLEAN_STILL = 400;
    // Swimming/moving multiplier (20/15 = 1.33x faster when moving, 15 seconds to clean)
    public static final float MOVING_CLEAN_MULTIPLIER = 20.0f / 15.0f;
    // Base clean points per tick when standing still
    public static final float BASE_CLEAN_PER_TICK = POINTS_TO_MAX_DIRTY / TICKS_TO_CLEAN_STILL;
    
    // Fly spawning: 2 game days at 100% dirtiness = flies start appearing
    public static final int DAYS_AT_MAX_FOR_FLIES = 2;
    public static final int TICKS_AT_MAX_FOR_FLIES = TICKS_PER_DAY * DAYS_AT_MAX_FOR_FLIES; // 48000 ticks
    
    public DirtinessData() {
    }
    
    /**
     * Get the current dirtiness percentage (0.0 to 1.0)
     */
    public float getDirtiness(long currentGameTime) {
        return Math.min(1.0f, Math.max(0.0f, accumulatedDirt / POINTS_TO_MAX_DIRTY));
    }
    
    /**
     * Add dirt based on environmental multiplier.
     * Called once per second (20 ticks) from handler.
     * 
     * @param multiplier Combined environmental/activity multiplier
     * @param ticks Number of ticks since last update (usually 20)
     * @param currentGameTime Current game time for tracking max dirty duration
     */
    public void addDirt(float multiplier, int ticks, long currentGameTime) {
        if (!initialized) return;
        
        boolean wasAtMax = accumulatedDirt >= POINTS_TO_MAX_DIRTY;
        
        float dirtToAdd = BASE_DIRT_PER_TICK * multiplier * ticks;
        this.accumulatedDirt = Math.min(POINTS_TO_MAX_DIRTY, this.accumulatedDirt + dirtToAdd);
        
        // Track when player first reaches 100% dirtiness
        boolean isAtMax = accumulatedDirt >= POINTS_TO_MAX_DIRTY;
        if (!wasAtMax && isAtMax) {
            // Just reached 100%
            this.maxDirtyStartTime = currentGameTime;
        }
    }
    
    /**
     * Add dirt based on environmental multiplier (backwards compatible overload).
     */
    public void addDirt(float multiplier, int ticks) {
        addDirt(multiplier, ticks, 0);
    }
    
    /**
     * Add instant dirt from events (combat, mining, etc.)
     * 
     * @param percentage Percentage of max dirtiness to add (0.0 to 1.0)
     */
    public void addInstantDirt(float percentage) {
        if (!initialized) return;
        
        float dirtToAdd = percentage * POINTS_TO_MAX_DIRTY;
        this.accumulatedDirt = Math.min(POINTS_TO_MAX_DIRTY, this.accumulatedDirt + dirtToAdd);
    }
    
    /**
     * Reduce dirtiness by a percentage (e.g., from splash hot water).
     * @param currentGameTime current game time
     * @param percentage the percentage to reduce (0.10 = 10%)
     */
    public void reduceDirtiness(long currentGameTime, float percentage) {
        if (!initialized) return;
        
        float dirtToRemove = percentage * POINTS_TO_MAX_DIRTY;
        this.accumulatedDirt = Math.max(0, this.accumulatedDirt - dirtToRemove);
        
        // If no longer at 100%, reset fly timer
        if (getDirtiness(currentGameTime) < 1.0f) {
            this.maxDirtyStartTime = 0;
        }
        
        // Generate new seed if player got cleaner
        if (dirtToRemove > 0) {
            generateNewSeed(currentGameTime);
        }
    }
    
    /**
     * Reset dirtiness (player took a bath) - instant clean for backwards compatibility
     */
    public void takeBath(long currentGameTime) {
        boolean wasNotClean = getDirtiness(currentGameTime) >= 0.05f;
        this.accumulatedDirt = 0;
        this.maxDirtyStartTime = 0; // Reset fly timer
        
        if (wasNotClean) {
            generateNewSeed(currentGameTime);
        }
    }
    
    /**
     * Gradually clean the player while bathing.
     * Called each tick while player is in hot water.
     * @param currentGameTime current game time
     * @param isMoving whether the player is moving/swimming (accelerates cleaning)
     * @return true if any cleaning was done, false if already clean
     */
    public boolean progressBath(long currentGameTime, boolean isMoving) {
        if (!initialized) {
            initialized = true;
            return false;
        }
        
        if (accumulatedDirt <= 0) {
            return false; // Already clean
        }
        
        // Check if we're about to become clean (for generating new seed)
        boolean wasNotClean = getDirtiness(currentGameTime) >= 0.05f;
        boolean wasAtMax = accumulatedDirt >= POINTS_TO_MAX_DIRTY;
        
        // Remove dirt points
        float cleanRate = BASE_CLEAN_PER_TICK;
        if (isMoving) {
            cleanRate *= MOVING_CLEAN_MULTIPLIER;
        }
        
        this.accumulatedDirt = Math.max(0, this.accumulatedDirt - cleanRate);
        
        // Reset fly timer if no longer at 100%
        if (wasAtMax && accumulatedDirt < POINTS_TO_MAX_DIRTY) {
            this.maxDirtyStartTime = 0;
        }
        
        // If player just became clean, generate new seed for next dirtiness cycle
        if (wasNotClean && getDirtiness(currentGameTime) < 0.05f) {
            generateNewSeed(currentGameTime);
        }
        
        return true;
    }
    
    /**
     * Generate a new random seed for the next dirtiness cycle.
     * This ensures different dirt patterns each time player gets dirty.
     */
    private void generateNewSeed(long currentGameTime) {
        // Use current time and existing seed to create variation
        this.dirtSeed = this.dirtSeed * 6364136223846793005L + currentGameTime + 1442695040888963407L;
    }
    
    /**
     * Check if player is currently clean enough (for effects, achievements, etc.)
     */
    public boolean isClean(long currentGameTime) {
        return getDirtiness(currentGameTime) < 0.05f;
    }
    
    /**
     * Check if player has been at 100% dirtiness long enough to attract flies.
     * Requires 2 game days (48000 ticks) at max dirtiness.
     * 
     * @param currentGameTime Current game time
     * @return true if flies should spawn around this player
     */
    public boolean shouldSpawnFlies(long currentGameTime) {
        // maxDirtyStartTime == 0 means not tracking (not at 100%)
        // maxDirtyStartTime can be negative in new worlds, which is valid
        if (maxDirtyStartTime == 0) {
            return false;
        }
        return (currentGameTime - maxDirtyStartTime) >= TICKS_AT_MAX_FOR_FLIES;
    }
    
    /**
     * Get how long the player has been at 100% dirtiness (in ticks).
     * Returns 0 if not currently at max.
     */
    public long getTicksAtMaxDirty(long currentGameTime) {
        // maxDirtyStartTime == 0 means not tracking
        if (maxDirtyStartTime == 0) {
            return 0;
        }
        return currentGameTime - maxDirtyStartTime;
    }
    
    /**
     * Initialize for a new player
     */
    public void initialize(long currentGameTime, long playerSeed) {
        if (!initialized) {
            this.accumulatedDirt = 0;
            this.initialized = true;
        }
        this.dirtSeed = playerSeed;
    }
    
    public float getAccumulatedDirt() {
        return accumulatedDirt;
    }
    
    public long getDirtSeed() {
        return dirtSeed;
    }
    
    public void setDirtSeed(long seed) {
        this.dirtSeed = seed;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("accumulatedDirt", accumulatedDirt);
        tag.putLong("dirtSeed", dirtSeed);
        tag.putBoolean("initialized", initialized);
        tag.putLong("maxDirtyStartTime", maxDirtyStartTime);
        return tag;
    }
    
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.accumulatedDirt = tag.getFloat("accumulatedDirt");
        this.dirtSeed = tag.getLong("dirtSeed");
        this.initialized = tag.getBoolean("initialized");
        this.maxDirtyStartTime = tag.getLong("maxDirtyStartTime");
    }
    
    /**
     * Copy data from another instance (for death/respawn)
     */
    public void copyFrom(DirtinessData other) {
        this.accumulatedDirt = other.accumulatedDirt;
        this.dirtSeed = other.dirtSeed;
        this.initialized = other.initialized;
        this.maxDirtyStartTime = other.maxDirtyStartTime;
    }
    
    /**
     * DEBUG: Set dirtiness to a specific percentage
     * @param dirtinessPercent 0.0 to 1.0
     */
    public void setDirtinessDebug(long currentGameTime, float dirtinessPercent) {
        this.initialized = true;
        this.accumulatedDirt = dirtinessPercent * POINTS_TO_MAX_DIRTY;
        
        // Reset fly timer if not at 100%
        if (dirtinessPercent < 1.0f) {
            this.maxDirtyStartTime = 0;
        } else if (this.maxDirtyStartTime == 0) {
            // Just reached 100%, start tracking
            this.maxDirtyStartTime = currentGameTime;
        }
    }
    
    /**
     * DEBUG: Set to 100% dirty with flies already active.
     * Simulates having been at max dirtiness for 2+ days.
     */
    public void setDirtinessDebugWithFlies(long currentGameTime) {
        this.initialized = true;
        this.accumulatedDirt = POINTS_TO_MAX_DIRTY;
        // Set start time to 2+ days ago so flies spawn immediately
        this.maxDirtyStartTime = currentGameTime - TICKS_AT_MAX_FOR_FLIES - 1000;
    }
}
