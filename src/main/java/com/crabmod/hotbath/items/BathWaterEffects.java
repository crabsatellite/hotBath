package com.crabmod.hotbath.items;

import com.crabmod.hotbath.compat.*;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Drinking effects for bath water bottles
 */
public class BathWaterEffects {
    private static final Random RANDOM = new Random();

    /**
     * Hot Water Bottle Effect: Speed I for 5 seconds (weakened from 20s)
     */
    public static void hotWaterEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5 * 20, 0, false, false, true));
        applyTemperatureEffects(entity);
    }

    /**
     * Honey Bath Water Effect: Instant heal + Absorption I for 5 seconds (weakened from Absorption II 20s)
     */
    public static void honeyBathEffect(LivingEntity entity) {
        entity.heal(2.0F); // Instant heal 2 hearts
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 5 * 20, 0, false, false, true));
        applyTemperatureEffects(entity);
    }

    /**
     * Milk Bath Water Effect: Remove 1 random harmful effect (weakened from removing all)
     */
    public static void milkBathEffect(LivingEntity entity) {
        List<MobEffectInstance> harmfulEffects = new ArrayList<>();
        
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            Holder<MobEffect> effectHolder = effect.getEffect();
            if (isHarmfulEffect(effectHolder) && effectHolder != MobEffects.UNLUCK) {
                harmfulEffects.add(effect);
            }
        }
        
        if (!harmfulEffects.isEmpty()) {
            MobEffectInstance effectToRemove = harmfulEffects.get(RANDOM.nextInt(harmfulEffects.size()));
            entity.removeEffect(effectToRemove.getEffect());
        }
        applyTemperatureEffects(entity);
    }

    /**
     * Herbal Bath Water Effect: Resistance I for 5 seconds (weakened from 20s)
     */
    public static void herbalBathEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5 * 20, 0, false, false, true));
        applyTemperatureEffects(entity);
    }

    /**
     * Peony Bath Water Effect: Instant heal only (weakened from heal + buffs)
     */
    public static void peonyBathEffect(LivingEntity entity) {
        entity.heal(2.0F); // Instant heal 2 hearts
        applyTemperatureEffects(entity);
    }

    /**
     * Rose Bath Water Effect: Instant heal + Strength I for 5 seconds (weakened from 20s)
     */
    public static void roseBathEffect(LivingEntity entity) {
        entity.heal(2.0F); // Instant heal 2 hearts
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 5 * 20, 0, false, false, true));
        applyTemperatureEffects(entity);
    }

    /**
     * Apply temperature effects for ToughAsNails, Cold Sweat, and Legendary Survival Overhaul if mods are loaded
     */
    private static void applyTemperatureEffects(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        
        // Apply ToughAsNails temperature effect (10 seconds, WARM)
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            BathWaterBottleTANModifier.applyWarmEffect(player);
        }
        
        // Apply Cold Sweat temperature effect (5 seconds, 36°C)
        if (ColdSweatIntegration.isColdSweatLoaded()) {
            BathWaterBottleColdSweatModifier.applyWarmEffect(player);
        }
        
        // Apply Legendary Survival Overhaul temperature effect (5 seconds, 20.0)
        if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            BathWaterBottleLSOModifier.applyWarmEffect(player);
        }
    }

    /**
     * Apply only temperature effects (no potion effects).
     * Used when player drinks from bath water sources using ToughAsNails' hand drinking feature.
     */
    public static void applyTemperatureEffectsOnly(net.minecraft.world.entity.player.Player player) {
        // Apply ToughAsNails temperature effect (10 seconds, WARM)
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            BathWaterBottleTANModifier.applyWarmEffect(player);
        }
        
        // Apply Cold Sweat temperature effect (5 seconds, 36°C)
        if (ColdSweatIntegration.isColdSweatLoaded()) {
            BathWaterBottleColdSweatModifier.applyWarmEffect(player);
        }
        
        // Apply Legendary Survival Overhaul temperature effect (5 seconds, 20.0)
        if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            BathWaterBottleLSOModifier.applyWarmEffect(player);
        }
    }

    /**
     * Check if effect is harmful.
     * Uses the effect's category to determine if it's beneficial or harmful,
     * which provides better compatibility with modded effects.
     */
    private static boolean isHarmfulEffect(Holder<MobEffect> effectHolder) {
        MobEffect effect = effectHolder.value();
        // Use the effect's category - HARMFUL effects are negative effects
        // This automatically supports modded effects that properly set their category
        return !effect.isBeneficial() && effectHolder != MobEffects.UNLUCK;
    }
}
