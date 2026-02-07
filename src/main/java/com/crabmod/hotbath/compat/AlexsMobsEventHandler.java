package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.dirtiness.DirtinessAttachment;
import com.crabmod.hotbath.dirtiness.DirtinessData;
import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey;
import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import com.github.alexthe666.alexsmobs.entity.EntityCrimsonMosquito;
import com.github.alexthe666.alexsmobs.entity.EntityFly;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Random;

/**
 * Event handler for Alex's Mobs integration.
 * 
 * Features:
 * - Dirty players attract flies, mosquitoes, and cockroaches
 * - Raccoons wash items 1.5x faster in hot bath fluids (via Mixin)
 * - Raccoons have 1.5x tame chance when washing in hot bath fluids (via Mixin)
 * - Capuchin monkeys are attracted to hot springs (like Japanese macaques!)
 * - Monkeys in hot springs receive regeneration buff and are easier to tame (via Mixin)
 */
public class AlexsMobsEventHandler {
    
    private static final Random RANDOM = new Random();
    
    // How often to check for nearby mobs (every 40 ticks = 2 seconds)
    private static final int CHECK_INTERVAL = 40;
    
    // Monkey hot spring attraction settings
    private static final double MONKEY_ATTRACTION_RANGE = 16.0;
    private static final int MONKEY_CHECK_INTERVAL = 60; // 3 seconds
    
    // Low-priority sitting behavior timing (similar to cat behavior)
    private static final int MIN_SOAK_TIME = 600; // 30 seconds minimum soak
    private static final int MAX_SOAK_TIME = 2400; // 2 minutes maximum soak
    private static final float RANDOM_LEAVE_CHANCE = 0.03f; // 3% chance to leave per check
    
    // Cache for monkey hot spring search results to avoid repeated block searches
    // Key: monkey entity ID, Value: cached search result
    private static final java.util.Map<Integer, CachedHotSpringSearch> MONKEY_HOTSPRING_CACHE = 
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final int CACHE_VALIDITY_TICKS = 200; // 10 seconds cache validity
    private static final int CACHE_CLEANUP_INTERVAL = 1200; // 1 minute cleanup
    private static long lastCacheCleanup = 0;
    
    // Track monkeys that are soaking in hot springs with their soak start time
    // Key: monkey entity ID, Value: game time when soaking started
    private static final java.util.Map<Integer, Long> MONKEYS_SOAKING_IN_HOTSPRING = 
            new java.util.concurrent.ConcurrentHashMap<>();
    
    private record CachedHotSpringSearch(BlockPos result, long cacheTime, BlockPos monkeyPos) {}
    
    // Range to search for mobs
    private static final double FLY_ATTRACTION_RANGE = 16.0;
    private static final double MOSQUITO_ATTRACTION_RANGE = 16.0; // Reduced from 24 for performance
    private static final double COCKROACH_LOITER_RANGE = 12.0;
    
    // Cockroach loiter distance (they stay at this distance, not approaching fully)
    private static final double COCKROACH_MIN_DISTANCE = 3.0;
    private static final double COCKROACH_MAX_DISTANCE = 6.0;
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        CompatManager.safeEventCall("alexsmobs", "onPlayerTick", () -> {
        // Only process on server side
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Check if dirtiness system is enabled
        if (!HotBathConfig.isDirtinessEnabled()) return;
        
        // Only check every CHECK_INTERVAL ticks for performance
        if (player.tickCount % CHECK_INTERVAL != 0) return;
        
        // Get dirtiness data
        DirtinessData data = player.getData(DirtinessAttachment.DIRTINESS);
        if (data == null) return;
        
        long gameTime = player.level().getGameTime();
        boolean hasFlies = data.shouldSpawnFlies(gameTime);
        float dirtiness = data.getDirtiness(gameTime);
        
        // Handle fly attraction (only when player has flies - extremely dirty)
        if (hasFlies) {
            attractFlies(player);
            makeMosquitoesAggressive(player);
        }
        
        // Handle cockroach loitering (when player is at least 80% dirty)
        if (dirtiness >= 0.8f) {
            makeCockroachesLoiter(player, dirtiness);
        }
        });
    }
    
    /**
     * Attract nearby flies to swarm around the extremely dirty player.
     */
    private static void attractFlies(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(FLY_ATTRACTION_RANGE);
        List<EntityFly> flies = player.level().getEntitiesOfClass(
                EntityFly.class, 
                searchBox,
                fly -> fly.isAlive() && !fly.isBaby()
        );
        
        for (EntityFly fly : flies) {
            double distance = fly.distanceTo(player);
            
            // If fly is not too close, move it towards player
            if (distance > 2.0) {
                // Random offset around the player's head
                double offsetX = (RANDOM.nextDouble() - 0.5) * 2.0;
                double offsetY = RANDOM.nextDouble() * 0.5 + 1.0; // Head height
                double offsetZ = (RANDOM.nextDouble() - 0.5) * 2.0;
                
                Vec3 targetPos = player.position().add(offsetX, offsetY, offsetZ);
                fly.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.2);
            }
        }
    }
    
    /**
     * Make nearby mosquitoes more aggressive towards the dirty player.
     * They will prioritize targeting this player over others.
     */
    private static void makeMosquitoesAggressive(ServerPlayer player) {
        AABB searchBox = player.getBoundingBox().inflate(MOSQUITO_ATTRACTION_RANGE);
        List<EntityCrimsonMosquito> mosquitoes = player.level().getEntitiesOfClass(
                EntityCrimsonMosquito.class,
                searchBox,
                mosquito -> mosquito.isAlive() && mosquito.getTarget() == null
        );
        
        for (EntityCrimsonMosquito mosquito : mosquitoes) {
            // Set the dirty player as target if mosquito doesn't have one
            // This makes dirty players more likely to be attacked
            if (RANDOM.nextFloat() < 0.3f) { // 30% chance each check
                mosquito.setTarget(player);
            }
        }
    }
    
    /**
     * Make cockroaches loiter around the dirty player.
     * They don't approach directly but stay at a comfortable distance.
     */
    private static void makeCockroachesLoiter(ServerPlayer player, float dirtiness) {
        AABB searchBox = player.getBoundingBox().inflate(COCKROACH_LOITER_RANGE);
        List<EntityCockroach> cockroaches = player.level().getEntitiesOfClass(
                EntityCockroach.class,
                searchBox,
                cockroach -> cockroach.isAlive()
        );
        
        for (EntityCockroach cockroach : cockroaches) {
            double distance = cockroach.distanceTo(player);
            
            // Probability increases with dirtiness
            if (RANDOM.nextFloat() > dirtiness * 0.5f) continue;
            
            if (distance > COCKROACH_MAX_DISTANCE) {
                // Too far - move closer to loiter range
                Vec3 direction = player.position().subtract(cockroach.position()).normalize();
                double targetDist = COCKROACH_MIN_DISTANCE + RANDOM.nextDouble() * 
                        (COCKROACH_MAX_DISTANCE - COCKROACH_MIN_DISTANCE);
                Vec3 targetPos = player.position().subtract(direction.scale(targetDist));
                
                // Add some randomness to the position
                targetPos = targetPos.add(
                        (RANDOM.nextDouble() - 0.5) * 2.0,
                        0,
                        (RANDOM.nextDouble() - 0.5) * 2.0
                );
                
                cockroach.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 0.8);
            } else if (distance < COCKROACH_MIN_DISTANCE) {
                // Too close - back off a bit
                Vec3 direction = cockroach.position().subtract(player.position()).normalize();
                double targetDist = COCKROACH_MIN_DISTANCE + RANDOM.nextDouble() * 2.0;
                Vec3 targetPos = player.position().add(direction.scale(targetDist));
                
                cockroach.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.0);
            }
            // If in loiter range, they just wander naturally (no intervention needed)
        }
    }
    
    // ==================== Monkey Hot Spring Features ====================
    
    /**
     * Handle monkey tick events for hot spring attraction and buff.
     * Capuchin monkeys are attracted to nearby hot springs and receive
     * regeneration buff when bathing (like Japanese macaques!).
     * 
     * This is a LOW PRIORITY idle behavior - monkeys should:
     * - NOT be forced to stay in the hot spring
     * - Naturally leave after soaking for a while
     * - Not be re-attracted immediately after leaving
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        CompatManager.safeEventCall("alexsmobs", "onEntityTick", () -> {
        // Only process capuchin monkeys on server side
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof EntityCapuchinMonkey monkey)) return;
        
        int monkeyId = monkey.getId();
        boolean isSoaking = MONKEYS_SOAKING_IN_HOTSPRING.containsKey(monkeyId);
        
        // For soaking monkeys, check more frequently (every 10 ticks) to maintain sitting pose
        // For non-soaking monkeys, check less frequently (every 60 ticks) for performance
        int checkInterval = isSoaking ? 10 : MONKEY_CHECK_INTERVAL;
        if (monkey.tickCount % checkInterval != 0) return;
        
        long currentTime = monkey.level().getGameTime();
        
        // Clean up if monkey is dead or removed
        if (!monkey.isAlive()) {
            MONKEYS_SOAKING_IN_HOTSPRING.remove(monkeyId);
            MONKEY_HOTSPRING_CACHE.remove(monkeyId);
            return;
        }
        
        // Check if monkey is currently in hot spring
        boolean currentlyInHotSpring = isInHotSpring(monkey);
        
        if (currentlyInHotSpring) {
            // Monkey is in hot spring
            if (!isSoaking) {
                // Just entered - start soaking
                startSoaking(monkey, currentTime);
            } else {
                // Continue soaking - check if should leave
                if (shouldLeaveSoaking(monkey, currentTime)) {
                    stopSoaking(monkey);
                    // After leaving, don't re-attract for a while (handled by navigation being busy)
                    return;
                }
                // Apply buff while soaking
                applyHotSpringBuff(monkey);
            }
        } else {
            // Monkey is NOT in hot spring
            if (isSoaking) {
                // Just left the hot spring - clean up state
                stopSoaking(monkey);
            }
            
            // Only attract to hot spring if monkey is idle and not recently left
            // This is the LOW PRIORITY part - don't interfere with other behaviors
            if (shouldAttractToHotSpring(monkey)) {
                attractToHotSpring(monkey);
            }
        }
        });
    }
    
    /**
     * Check if monkey should be attracted to a hot spring.
     * This should be LOW PRIORITY - don't interfere with other behaviors.
     */
    private static boolean shouldAttractToHotSpring(EntityCapuchinMonkey monkey) {
        // Don't attract tamed monkeys (they follow their owner)
        if (monkey.isTame()) return false;
        
        // Don't attract if monkey was ordered to sit
        if (monkey.isOrderedToSit()) return false;
        
        // Don't attract if monkey has a target (fighting, fleeing, etc.)
        if (monkey.getTarget() != null) return false;
        
        // Don't attract if monkey was recently hurt
        if (monkey.getLastHurtByMob() != null) return false;
        
        // Don't attract if navigation is busy (monkey is going somewhere)
        if (monkey.getNavigation().isInProgress()) return false;
        
        return true;
    }
    
    /**
     * Start soaking in hot spring - make monkey sit and track time.
     */
    private static void startSoaking(EntityCapuchinMonkey monkey, long currentTime) {
        int monkeyId = monkey.getId();
        
        // Record soak start time
        MONKEYS_SOAKING_IN_HOTSPRING.put(monkeyId, currentTime);
        
        // CRITICAL: Stop navigation first to prevent spinning
        monkey.getNavigation().stop();
        
        // Make monkey sit down to relax
        monkey.setOrderedToSit(true);
        
        // Apply initial buff
        applyHotSpringBuff(monkey);
    }
    
    /**
     * Check if monkey should leave the hot spring based on time and random chance.
     */
    private static boolean shouldLeaveSoaking(EntityCapuchinMonkey monkey, long currentTime) {
        int monkeyId = monkey.getId();
        Long soakStartTime = MONKEYS_SOAKING_IN_HOTSPRING.get(monkeyId);
        
        if (soakStartTime == null) return true;
        
        long soakDuration = currentTime - soakStartTime;
        
        // Must leave after maximum soak time
        if (soakDuration > MAX_SOAK_TIME) {
            return true;
        }
        
        // After minimum time, random chance to leave
        if (soakDuration > MIN_SOAK_TIME && RANDOM.nextFloat() < RANDOM_LEAVE_CHANCE) {
            return true;
        }
        
        // Check if monkey wants to leave (being attacked, has target, etc.)
        if (monkey.hurtTime > 0 || 
            monkey.getTarget() != null || 
            monkey.getLastHurtByMob() != null) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Stop soaking - make monkey stand up and clear tracking.
     */
    private static void stopSoaking(EntityCapuchinMonkey monkey) {
        int monkeyId = monkey.getId();
        
        // Clear tracking
        MONKEYS_SOAKING_IN_HOTSPRING.remove(monkeyId);
        
        // Make monkey stand up (if it was sitting due to hot spring)
        if (monkey.isSitting() && !monkey.isTame()) {
            monkey.setOrderedToSit(false);
        }
        
        // Clear navigation so monkey will wander naturally
        monkey.getNavigation().stop();
    }
    
    /**
     * Check if a monkey is currently in a hot spring fluid.
     */
    private static boolean isInHotSpring(EntityCapuchinMonkey monkey) {
        BlockPos pos = monkey.blockPosition();
        BlockState state = monkey.level().getBlockState(pos);
        return state.getBlock() instanceof AbstractHotbathBlock;
    }
    
    /**
     * Apply regeneration buff to monkey bathing in hot spring.
     * Also ensures the monkey stays sitting and doesn't wander.
     */
    private static void applyHotSpringBuff(EntityCapuchinMonkey monkey) {
        // CRITICAL: Ensure monkey stays sitting and doesn't spin
        // The monkey's AI may try to make it stand up and move
        if (!monkey.isSitting()) {
            monkey.setOrderedToSit(true);
        }
        
        // Stop any navigation that might have started
        if (monkey.getNavigation().isInProgress()) {
            monkey.getNavigation().stop();
        }
        
        // Give regeneration effect (this refreshes while in hot spring)
        monkey.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION, 
                100, // 5 seconds
                0,   // Level I
                false, 
                true,  // Show particles (relaxing bubbles!)
                true
        ));
        
        // Small chance to play happy sound
        if (RANDOM.nextInt(20) == 0) {
            // Monkey is enjoying the hot spring!
            monkey.playAmbientSound();
        }
    }

    /**
     * Attract monkeys to nearby hot springs.
     * Wild (untamed) monkeys will naturally move towards hot springs.
     * This is LOW PRIORITY - only when monkey is completely idle.
     */
    private static void attractToHotSpring(EntityCapuchinMonkey monkey) {
        // Search for nearby hot spring
        BlockPos monkeyPos = monkey.blockPosition();
        BlockPos nearestHotSpring = findNearestHotSpring(monkey, monkeyPos);
        
        if (nearestHotSpring != null) {
            // Move towards the hot spring
            double targetX = nearestHotSpring.getX() + 0.5;
            double targetY = nearestHotSpring.getY();
            double targetZ = nearestHotSpring.getZ() + 0.5;
            
            // Only navigate if not too close
            double distance = monkey.position().distanceTo(new Vec3(targetX, targetY, targetZ));
            if (distance > 1.5 && distance < MONKEY_ATTRACTION_RANGE) {
                monkey.getNavigation().moveTo(targetX, targetY, targetZ, 0.8);
            }
        }
    }
    
    /**
     * Find the nearest hot spring fluid block within range.
     * Uses caching to avoid expensive block searches every tick.
     */
    private static BlockPos findNearestHotSpring(EntityCapuchinMonkey monkey, BlockPos center) {
        int entityId = monkey.getId();
        long currentTime = monkey.level().getGameTime();
        
        // Periodic cache cleanup
        if (currentTime - lastCacheCleanup > CACHE_CLEANUP_INTERVAL) {
            lastCacheCleanup = currentTime;
            MONKEY_HOTSPRING_CACHE.entrySet().removeIf(entry -> 
                    currentTime - entry.getValue().cacheTime() > CACHE_VALIDITY_TICKS * 2);
        }
        
        // Check cache first
        CachedHotSpringSearch cached = MONKEY_HOTSPRING_CACHE.get(entityId);
        if (cached != null) {
            // Cache is valid if: not expired AND monkey hasn't moved too far
            boolean cacheValid = (currentTime - cached.cacheTime()) < CACHE_VALIDITY_TICKS
                    && cached.monkeyPos().distSqr(center) < 9; // Within 3 blocks
            
            if (cacheValid) {
                // If cached result exists, verify it's still a hot spring
                if (cached.result() != null) {
                    BlockState state = monkey.level().getBlockState(cached.result());
                    if (state.getBlock() instanceof AbstractHotbathBlock) {
                        return cached.result();
                    }
                    // Hot spring was removed, invalidate cache
                } else {
                    // Cached "no hot spring found" - return null
                    return null;
                }
            }
        }
        
        // Perform actual search
        BlockPos nearest = performHotSpringSearch(monkey, center);
        
        // Store result in cache
        MONKEY_HOTSPRING_CACHE.put(entityId, new CachedHotSpringSearch(nearest, currentTime, center));
        
        return nearest;
    }
    
    /**
     * Perform the actual block search for hot springs.
     */
    private static BlockPos performHotSpringSearch(EntityCapuchinMonkey monkey, BlockPos center) {
        int searchRadius = (int) MONKEY_ATTRACTION_RANGE;
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        // Search in a spiral pattern from center (more efficient for finding nearby blocks)
        for (int r = 1; r <= searchRadius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    // Only check the edge of each "ring"
                    if (Math.abs(x) != r && Math.abs(z) != r) continue;
                    
                    for (int y = -2; y <= 2; y++) {
                        BlockPos checkPos = center.offset(x, y, z);
                        BlockState state = monkey.level().getBlockState(checkPos);
                        
                        if (state.getBlock() instanceof AbstractHotbathBlock) {
                            double dist = center.distSqr(checkPos);
                            if (dist < nearestDist) {
                                nearestDist = dist;
                                nearest = checkPos;
                            }
                        }
                    }
                }
            }
            
            // If found one in this ring, no need to search further
            if (nearest != null) break;
        }
        
        return nearest;
    }
}
