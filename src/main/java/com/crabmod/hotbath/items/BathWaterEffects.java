package com.crabmod.hotbath.items;

import com.crabmod.hotbath.compat.BathWaterBottleTANModifier;
import com.crabmod.hotbath.compat.ToughAsNailsIntegration;
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
        applyToughAsNailsWarmEffect(entity);
    }

    /**
     * Honey Bath Water Effect: Instant heal + Absorption I for 5 seconds (weakened from Absorption II 20s)
     */
    public static void honeyBathEffect(LivingEntity entity) {
        entity.heal(2.0F); // Instant heal 2 hearts
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 5 * 20, 0, false, false, true));
        applyToughAsNailsWarmEffect(entity);
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
        applyToughAsNailsWarmEffect(entity);
    }

    /**
     * Herbal Bath Water Effect: Resistance I for 5 seconds (weakened from 20s)
     */
    public static void herbalBathEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5 * 20, 0, false, false, true));
        applyToughAsNailsWarmEffect(entity);
    }

    /**
     * Peony Bath Water Effect: Instant heal only (weakened from heal + buffs)
     */
    public static void peonyBathEffect(LivingEntity entity) {
        entity.heal(2.0F); // Instant heal 2 hearts
        applyToughAsNailsWarmEffect(entity);
    }

    /**
     * Rose Bath Water Effect: Instant heal + Strength I for 5 seconds (weakened from 20s)
     */
    public static void roseBathEffect(LivingEntity entity) {
        entity.heal(2.0F); // Instant heal 2 hearts
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 5 * 20, 0, false, false, true));
        applyToughAsNailsWarmEffect(entity);
    }

    /**
     * Apply ToughAsNails WARM temperature effect if mod is loaded
     * Uses the BathWaterBottleTANModifier to track temporary warm effect
     */
    private static void applyToughAsNailsWarmEffect(LivingEntity entity) {
        if (ToughAsNailsIntegration.isToughAsNailsLoaded() && entity instanceof net.minecraft.world.entity.player.Player player) {
            BathWaterBottleTANModifier.applyWarmEffect(player);
        }
    }

    /**
     * Check if effect is harmful
     */
    private static boolean isHarmfulEffect(Holder<MobEffect> effect) {
        return effect == MobEffects.POISON
                || effect == MobEffects.WITHER
                || effect == MobEffects.BLINDNESS
                || effect == MobEffects.MOVEMENT_SLOWDOWN
                || effect == MobEffects.WEAKNESS
                || effect == MobEffects.HUNGER
                || effect == MobEffects.BAD_OMEN
                || effect == MobEffects.DARKNESS
                || effect == MobEffects.GLOWING
                || effect == MobEffects.HARM
                || effect == MobEffects.LEVITATION
                || effect == MobEffects.DIG_SLOWDOWN
                || effect == MobEffects.CONFUSION;
    }
}
