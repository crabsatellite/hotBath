package com.crabmod.hotbath.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import sfiomn.legendarysurvivaloverhaul.registry.MobEffectRegistry;
import sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil;
import sfiomn.legendarysurvivaloverhaul.api.thirst.ThirstUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for LSO API calls. Only loaded when LSO is present.
 */
public class LSOApiHelper {
    
    // Cache for temperature modifier values to avoid redundant API calls
    // Stores the last applied warmth value so we don't need to clear and re-read
    private static final Map<UUID, CachedTempData> TEMP_MODIFIER_CACHE = new ConcurrentHashMap<>();
    private static final double TEMP_UPDATE_THRESHOLD = 0.5; // Only update if change > 0.5°C
    
    // Record to store both the applied warmth and a timestamp for periodic recalculation
    private record CachedTempData(double appliedWarmth, long lastCalculationTime) {}
    
    // Bottle effects
    private static final int HOT_DRINK_BOTTLE_DURATION = 300; // 15 seconds
    private static final int HOT_DRINK_BOTTLE_AMPLIFIER = 0;
    private static final int HOT_DRINK_BOTTLE_AMPLIFIER_BOOSTED = 2; // When bathing
    
    // Immersion: Cold resistance accumulation
    private static final int COLD_RESISTANCE_AMPLIFIER = 2;
    private static final int MAX_RESISTANCE_DURATION = 6000; // 5 minutes
    
    // Bath temperature modifier
    private static final UUID HOT_BATH_TEMP_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final float TARGET_WARM_TEMPERATURE = 27.0f; // HOT zone middle
    
    // Cold immunity effect - prevents shivering after leaving bath
    private static final int COLD_IMMUNITY_DURATION = 200; // 10 seconds
    
    // Thirst
    private static final int HYDRATION = 5;
    private static final float SATURATION = 0.5f;
    
    /**
     * Apply HOT_DRINK effect for bottle (Level 1 or Level 3 if bathing, 15 seconds)
     * @param isBathing Whether player is currently in hot bath (for boosted effect)
     */
    public static void applyBottleTemperatureEffect(Player player, boolean isBathing) {
        MobEffect hotDrink = MobEffectRegistry.HOT_DRINk.get();
        
        // Use stronger effect if bathing
        int amplifier = isBathing ? HOT_DRINK_BOTTLE_AMPLIFIER_BOOSTED : HOT_DRINK_BOTTLE_AMPLIFIER;
        
        MobEffectInstance effect = new MobEffectInstance(
            hotDrink,
            HOT_DRINK_BOTTLE_DURATION,
            amplifier,
            false,
            true,
            true
        );
        player.addEffect(effect);
    }
    
    /**
     * Update cold resistance effect by adding duration
     * Called every 10 seconds of bathing to add 1 minute of resistance
     * @param player The player to update
     * @param durationToAdd Duration in ticks to add to the effect
     */
    public static void updateImmersionResistanceEffect(Player player, int durationToAdd) {
        MobEffect coldResistance = MobEffectRegistry.COLD_RESISTANCE.get();
        
        // Get current effect duration, or 0 if not present
        int currentDuration = 0;
        if (player.hasEffect(coldResistance)) {
            MobEffectInstance currentEffect = player.getEffect(coldResistance);
            if (currentEffect != null) {
                currentDuration = currentEffect.getDuration();
            }
        }
        
        // Add duration, capped at maximum (5 minutes)
        int newDuration = Math.min(currentDuration + durationToAdd, MAX_RESISTANCE_DURATION);
        
        MobEffectInstance effect = new MobEffectInstance(
            coldResistance,
            newDuration,
            COLD_RESISTANCE_AMPLIFIER,
            false,
            true,
            true
        );
        player.addEffect(effect);
    }
    
    // How often to fully recalculate temperature (5 seconds in game ticks)
    private static final long TEMP_RECALCULATION_INTERVAL = 100;
    
    /**
     * Apply temperature modifier to warm player to HOT zone (27°C).
     * Only warms if player is colder than target; no effect if already warm.
     * Uses caching to minimize API calls - only recalculates every 5 seconds or on significant change.
     */
    public static void applyBathTemperatureModifier(Player player) {
        UUID playerUUID = player.getUUID();
        long currentTime = player.level().getGameTime();
        
        CachedTempData cached = TEMP_MODIFIER_CACHE.get(playerUUID);
        
        // Check if we need to recalculate (first time, or periodic recalculation)
        boolean needsRecalculation = cached == null || 
            (currentTime - cached.lastCalculationTime()) >= TEMP_RECALCULATION_INTERVAL;
        
        if (needsRecalculation) {
            // Get current temperature (includes our existing modifier if any)
            float currentTargetTemp = TemperatureUtil.getPlayerTargetTemperature(player);
            
            // Calculate base temperature by subtracting our current modifier
            double currentModifier = cached != null ? cached.appliedWarmth() : 0;
            float baseTargetTemp = (float) (currentTargetTemp - currentModifier);
            
            // Calculate new warmth needed
            double warmthNeeded = Math.max(0, TARGET_WARM_TEMPERATURE - baseTargetTemp);
            
            // Only call API if value changed significantly (avoid unnecessary calls)
            if (cached == null || Math.abs(warmthNeeded - currentModifier) > TEMP_UPDATE_THRESHOLD) {
                TemperatureUtil.addTemperatureModifier(player, warmthNeeded, HOT_BATH_TEMP_MODIFIER_UUID);
            }
            TEMP_MODIFIER_CACHE.put(playerUUID, new CachedTempData(warmthNeeded, currentTime));
        }
        // If not time for recalculation, keep using cached value (no API call needed)
    }
    
    /**
     * Remove temperature modifier when player leaves bath.
     */
    public static void removeBathTemperatureModifier(Player player) {
        UUID playerUUID = player.getUUID();
        CachedTempData cached = TEMP_MODIFIER_CACHE.get(playerUUID);
        // Only call API if we actually had a modifier applied
        if (cached != null && cached.appliedWarmth() > 0) {
            TemperatureUtil.addTemperatureModifier(player, 0.0, HOT_BATH_TEMP_MODIFIER_UUID);
        }
        TEMP_MODIFIER_CACHE.remove(playerUUID);
    }
    
    /**
     * Apply COLD_IMMUNITY effect to prevent shivering.
     * Used after bathing for 10+ seconds, refreshed only when effect is missing or about to expire.
     */
    public static void applyColdImmunityEffect(Player player) {
        MobEffect coldImmunity = MobEffectRegistry.COLD_IMMUNITY.get();
        
        // Only add effect if not present or about to expire (< 5 seconds remaining)
        MobEffectInstance current = player.getEffect(coldImmunity);
        if (current == null || current.getDuration() < 100) {
            MobEffectInstance effect = new MobEffectInstance(
                coldImmunity,
                COLD_IMMUNITY_DURATION,
                0,
                false,
                false, // Hide particles
                true
            );
            player.addEffect(effect);
        }
    }
    
    /**
     * Add thirst and saturation to player.
     */
    public static void addThirst(Player player) {
        ThirstUtil.takeDrink(player, HYDRATION, SATURATION);
    }
    
    /**
     * Get thirst values for tooltip
     */
    public static int getHydration() {
        return HYDRATION;
    }
    
    public static float getSaturation() {
        return SATURATION;
    }
    
    /**
     * Apply COLD_RESISTANCE effect for splash bottle (Level 2, 30 seconds)
     */
    public static void applySplashTemperatureEffect(Player player) {
        MobEffect coldResistance = MobEffectRegistry.COLD_RESISTANCE.get();
        
        MobEffectInstance effect = new MobEffectInstance(
            coldResistance,
            600, // 30 seconds
            1, // Level 2 (Amplifier 1)
            false,
            false,
            true
        );
        player.addEffect(effect);
    }
    
    /**
     * Clean up player cache when they log out to prevent memory leaks.
     */
    public static void cleanupPlayerCache(Player player) {
        TEMP_MODIFIER_CACHE.remove(player.getUUID());
    }
}

