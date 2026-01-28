package com.crabmod.hotbath.entity;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared behavior for cats (vanilla and Raycat) sitting near hot springs.
 * This is a LOW PRIORITY idle behavior - it should NOT override:
 * - Being tempted by items (fish, etc.)
 * - Following owner (if tamed)
 * - Running from threats
 * - Any other active AI goals
 * 
 * Similar to how vanilla cats sit on chests or beds.
 */
public class HotSpringCatBehavior {
    
    private static final Random RANDOM = new Random();
    
    // Behavior constants
    private static final int CHECK_INTERVAL = 60; // 3 seconds
    private static final int MIN_SIT_TIME = 600; // 30 seconds minimum
    private static final int MAX_SIT_TIME = 2400; // 2 minutes maximum
    private static final float RANDOM_LEAVE_CHANCE = 0.05f; // 5% chance per check
    private static final double SEARCH_RANGE = 8.0;
    
    // Track when each entity started sitting (by entity ID)
    private static final Map<Integer, Long> SIT_START_TIME = new ConcurrentHashMap<>();
    
    // Cache for hot spring searches
    private static final Map<Integer, CachedSearch> HOTSPRING_CACHE = new ConcurrentHashMap<>();
    private static final int CACHE_VALIDITY_TICKS = 200; // 10 seconds
    
    private record CachedSearch(BlockPos result, long cacheTime, BlockPos entityPos) {}
    
    /**
     * Interface for entities that can sit (both Cat and RaycatEntity implement similar methods)
     */
    public interface SittableEntity {
        boolean isInSittingPose();
        void setInSittingPose(boolean sitting);
        boolean isTame();
        boolean isOrderedToSit();
    }
    
    /**
     * Process cat behavior for hot spring sitting.
     * Call this every tick for the entity.
     * 
     * @param entity The tamable animal (Cat or Raycat)
     * @param sittable Interface to control sitting pose
     * @return true if the entity is currently sitting from hot spring behavior
     */
    public static boolean processCatTick(TamableAnimal entity, SittableEntity sittable) {
        if (entity.level().isClientSide()) return false;
        
        int entityId = entity.getId();
        
        // Clean up if entity is dead or removed
        if (!entity.isAlive()) {
            SIT_START_TIME.remove(entityId);
            HOTSPRING_CACHE.remove(entityId);
            return false;
        }
        
        // Don't interfere with tamed animals at all
        if (sittable.isTame()) return false;
        
        // Check if entity is currently sitting from hot spring
        boolean isSittingFromHotSpring = sittable.isInSittingPose() && 
                                         SIT_START_TIME.containsKey(entityId);
        
        if (isSittingFromHotSpring) {
            // Check if entity should stand up - ANY of these conditions
            boolean shouldStandUp = 
                entity.hurtTime > 0 ||                          // Being attacked
                entity.getTarget() != null ||                   // Has attack target
                entity.getLastHurtByMob() != null ||           // Was hurt recently
                entity.getNavigation().isInProgress() ||        // AI wants to move somewhere
                !entity.getNavigation().isDone();               // Navigation has a path
            
            // Also check sitting duration
            Long sitStartTime = SIT_START_TIME.get(entityId);
            if (sitStartTime != null) {
                long sittingDuration = entity.level().getGameTime() - sitStartTime;
                if (sittingDuration > MAX_SIT_TIME || 
                    (sittingDuration > MIN_SIT_TIME && RANDOM.nextFloat() < RANDOM_LEAVE_CHANCE)) {
                    shouldStandUp = true;
                }
            }
            
            if (shouldStandUp) {
                sittable.setInSittingPose(false);
                SIT_START_TIME.remove(entityId);
                return false;
            }
            return true; // Still sitting
        }
        
        // Only check for sitting opportunity every few seconds for performance
        if (entity.tickCount % CHECK_INTERVAL != 0) return false;
        
        // Don't sit if entity is busy doing something
        if (entity.getTarget() != null || 
            entity.getLastHurtByMob() != null ||
            !entity.getNavigation().isDone()) {
            return false;
        }
        
        BlockPos pos = entity.blockPosition();
        
        // Check if position is good for sitting
        if (isAdjacentToHotSpring(entity.level(), pos)) {
            if (!sittable.isInSittingPose()) {
                sittable.setInSittingPose(true);
                SIT_START_TIME.put(entityId, entity.level().getGameTime());
            }
            return true;
        } else if (isSittingFromHotSpring) {
            // Moved away from hot spring, stand up
            if (!isNearHotSpring(entity.level(), pos)) {
                sittable.setInSittingPose(false);
                SIT_START_TIME.remove(entityId);
            }
        }
        
        return false;
    }
    
    /**
     * Try to attract idle cat to nearby hot spring.
     * Call this after processCatTick returns false.
     * 
     * @param entity The tamable animal
     * @param sittable Interface to check sitting state
     * @return true if navigation was set to move towards hot spring
     */
    public static boolean attractToHotSpring(TamableAnimal entity, SittableEntity sittable) {
        if (entity.level().isClientSide()) return false;
        if (sittable.isTame()) return false;
        if (sittable.isInSittingPose()) return false;
        if (entity.getTarget() != null) return false;
        if (!entity.getNavigation().isDone()) return false;
        
        // Only check occasionally for performance
        if (entity.tickCount % CHECK_INTERVAL != 0) return false;
        
        BlockPos pos = entity.blockPosition();
        BlockPos nearestHotSpring = findNearestHotSpringEdge(entity, pos);
        
        if (nearestHotSpring != null) {
            double distance = pos.distSqr(nearestHotSpring);
            if (distance > 2 && distance < SEARCH_RANGE * SEARCH_RANGE) {
                entity.getNavigation().moveTo(
                    nearestHotSpring.getX() + 0.5,
                    nearestHotSpring.getY(),
                    nearestHotSpring.getZ() + 0.5,
                    0.8
                );
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if position is directly adjacent to a hot spring block.
     * The entity must be standing on solid ground (not in water) to sit.
     */
    public static boolean isAdjacentToHotSpring(Level level, BlockPos pos) {
        // Check that the entity is NOT currently standing in hot spring water
        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() instanceof AbstractHotbathBlock) {
            return false; // Entity is IN the water, not next to it
        }
        
        // Check the block below - entity should be standing on solid ground
        BlockState belowState = level.getBlockState(pos.below());
        if (belowState.getBlock() instanceof AbstractHotbathBlock) {
            return false; // Entity is standing on water (shouldn't happen, but check anyway)
        }
        
        // Check if there's a hot spring block adjacent (including diagonals at same level)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos checkPos = pos.offset(dx, 0, dz);
                BlockState state = level.getBlockState(checkPos);
                if (state.getBlock() instanceof AbstractHotbathBlock) {
                    return true;
                }
                // Also check one block below (for sitting on edge of pool)
                BlockPos belowCheckPos = checkPos.below();
                BlockState belowCheckState = level.getBlockState(belowCheckPos);
                if (belowCheckState.getBlock() instanceof AbstractHotbathBlock) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if entity is near any hot spring (within search range)
     */
    public static boolean isNearHotSpring(Level level, BlockPos pos) {
        int range = 2;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);
                    if (state.getBlock() instanceof AbstractHotbathBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Find nearest hot spring edge position for the entity to sit at.
     * Uses caching for performance.
     */
    public static BlockPos findNearestHotSpringEdge(TamableAnimal entity, BlockPos entityPos) {
        int entityId = entity.getId();
        long currentTime = entity.level().getGameTime();
        
        // Check cache
        CachedSearch cached = HOTSPRING_CACHE.get(entityId);
        if (cached != null && 
            currentTime - cached.cacheTime < CACHE_VALIDITY_TICKS &&
            cached.entityPos.closerThan(entityPos, 2)) {
            return cached.result;
        }
        
        // Search for hot spring edge
        Level level = entity.level();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;
        
        int range = (int) SEARCH_RANGE;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos checkPos = entityPos.offset(dx, dy, dz);
                    
                    // Looking for a position that is:
                    // 1. Not in water
                    // 2. Adjacent to hot spring water
                    // 3. Has solid ground below
                    BlockState state = level.getBlockState(checkPos);
                    if (state.getBlock() instanceof AbstractHotbathBlock) continue;
                    if (!state.isAir() && !state.canBeReplaced()) continue;
                    
                    BlockState belowState = level.getBlockState(checkPos.below());
                    if (!belowState.isSolid()) continue;
                    if (belowState.getBlock() instanceof AbstractHotbathBlock) continue;
                    
                    // Check if adjacent to hot spring
                    boolean adjacentToWater = false;
                    for (int adx = -1; adx <= 1 && !adjacentToWater; adx++) {
                        for (int adz = -1; adz <= 1 && !adjacentToWater; adz++) {
                            if (adx == 0 && adz == 0) continue;
                            BlockPos adjPos = checkPos.offset(adx, 0, adz);
                            if (level.getBlockState(adjPos).getBlock() instanceof AbstractHotbathBlock) {
                                adjacentToWater = true;
                            }
                            adjPos = checkPos.offset(adx, -1, adz);
                            if (level.getBlockState(adjPos).getBlock() instanceof AbstractHotbathBlock) {
                                adjacentToWater = true;
                            }
                        }
                    }
                    
                    if (adjacentToWater) {
                        double distSq = checkPos.distSqr(entityPos);
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            bestPos = checkPos;
                        }
                    }
                }
            }
        }
        
        // Cache result
        HOTSPRING_CACHE.put(entityId, new CachedSearch(bestPos, currentTime, entityPos));
        
        return bestPos;
    }
    
    /**
     * Clean up cache entries for entities that no longer exist.
     * Call this periodically (e.g., every minute).
     */
    public static void cleanupCache(long currentTime) {
        SIT_START_TIME.entrySet().removeIf(entry -> {
            // Remove entries older than 5 minutes (entity probably doesn't exist anymore)
            Long startTime = entry.getValue();
            return currentTime - startTime > 6000;
        });
        
        HOTSPRING_CACHE.entrySet().removeIf(entry -> {
            return currentTime - entry.getValue().cacheTime > CACHE_VALIDITY_TICKS * 2;
        });
    }
    
    /**
     * Adapter for vanilla Cat entity
     */
    public static SittableEntity createCatAdapter(Cat cat) {
        return new SittableEntity() {
            @Override
            public boolean isInSittingPose() {
                return cat.isInSittingPose();
            }
            
            @Override
            public void setInSittingPose(boolean sitting) {
                cat.setInSittingPose(sitting);
            }
            
            @Override
            public boolean isTame() {
                return cat.isTame();
            }
            
            @Override
            public boolean isOrderedToSit() {
                return cat.isOrderedToSit();
            }
        };
    }
}
