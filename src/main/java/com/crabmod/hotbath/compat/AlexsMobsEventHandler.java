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
    
    // Cache for monkey hot spring search results to avoid repeated block searches
    // Key: monkey entity ID, Value: cached search result
    private static final java.util.Map<Integer, CachedHotSpringSearch> MONKEY_HOTSPRING_CACHE = 
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final int CACHE_VALIDITY_TICKS = 200; // 10 seconds cache validity
    private static final int CACHE_CLEANUP_INTERVAL = 1200; // 1 minute cleanup
    private static long lastCacheCleanup = 0;
    
    // Track monkeys that have recently been made to sit in hot springs
    // This prevents us from constantly resetting their sitting state
    private static final java.util.Set<Integer> MONKEYS_SITTING_IN_HOTSPRING = 
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    
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
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        // Only process capuchin monkeys on server side
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof EntityCapuchinMonkey monkey)) return;
        
        // Only check every MONKEY_CHECK_INTERVAL ticks for performance
        if (monkey.tickCount % MONKEY_CHECK_INTERVAL != 0) return;
        
        // Check if monkey is already in hot spring
        if (isInHotSpring(monkey)) {
            applyHotSpringBuff(monkey);
        } else {
            // Check if monkey just left hot spring (was sitting but not tamed)
            if (monkey.isSitting() && !monkey.isTame() && !monkey.isOrderedToSit()) {
                handleMonkeyLeavingHotSpring(monkey);
            }
            // Try to attract monkey to nearby hot spring
            attractToHotSpring(monkey);
        }
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
     * Makes them sit down to relax (like Japanese macaques!).
     * The monkey can leave on its own after a while - this is idle behavior, not forced.
     */
    private static void applyHotSpringBuff(EntityCapuchinMonkey monkey) {
        int monkeyId = monkey.getId();
        
        // Only make monkey sit down once when entering the hot spring
        // This allows the monkey's natural idle behavior to take over
        // (they will stand up after 75-125 ticks on their own)
        if (!MONKEYS_SITTING_IN_HOTSPRING.contains(monkeyId)) {
            if (!monkey.isSitting()) {
                monkey.setOrderedToSit(true);
            }
            MONKEYS_SITTING_IN_HOTSPRING.add(monkeyId);
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
     * Handle monkey leaving the hot spring.
     * Clears the tracking state so the monkey can be attracted again later.
     */
    private static void handleMonkeyLeavingHotSpring(EntityCapuchinMonkey monkey) {
        int monkeyId = monkey.getId();
        
        // Clear tracking - monkey is no longer in hot spring
        MONKEYS_SITTING_IN_HOTSPRING.remove(monkeyId);
        
        // If monkey was sitting in the hot spring (not tamed owner-ordered sit), make them stand
        if (monkey.isSitting() && !monkey.isTame()) {
            monkey.setOrderedToSit(false);
        }
    }
    
    /**
     * Attract monkeys to nearby hot springs.
     * Wild (untamed) monkeys will naturally move towards hot springs.
     */
    private static void attractToHotSpring(EntityCapuchinMonkey monkey) {
        // Only attract wild monkeys (tamed ones follow their owner)
        if (monkey.isTame()) return;
        
        // Don't attract if monkey is sitting or has a target
        if (monkey.isOrderedToSit() || monkey.getTarget() != null) return;
        
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
