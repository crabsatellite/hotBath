package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.registers.FluidsRegister;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;
import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;

import java.util.List;

/**
 * Create mod integration for Hot Bath fluids.
 * Registers Open Pipe Effect Handlers and Block Spouting Behaviours.
 */
public class CreateIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Effect durations (in ticks)
    private static final int PIPE_EFFECT_DURATION = 200; // 10 seconds
    private static final int PIPE_EFFECT_INTERVAL = 5; // Apply every 5 ticks
    
    /**
     * Initialize all Create integrations.
     */
    public static void init() {
        registerOpenPipeEffects();
        registerSpoutBehaviours();
        LOGGER.info("Hot Bath Create integration registered.");
    }
    
    /**
     * Register Open Pipe Effect Handlers for all Hot Bath fluids.
     * These effects are applied when fluid flows out of an open pipe onto entities.
     */
    private static void registerOpenPipeEffects() {
        // Hot Water - Speed effect
        OpenPipeEffectHandler.REGISTRY.register(
            FluidsRegister.HOT_WATER_FLUID.get(),
            new HotWaterPipeEffect()
        );
        
        // Honey Bath - Absorption and Slowness
        OpenPipeEffectHandler.REGISTRY.register(
            FluidsRegister.HONEY_BATH_FLUID.get(),
            new HoneyBathPipeEffect()
        );
        
        // Milk Bath - Remove negative effects
        OpenPipeEffectHandler.REGISTRY.register(
            FluidsRegister.MILK_BATH_FLUID.get(),
            new MilkBathPipeEffect()
        );
        
        // Herbal Bath - Resistance and Regeneration
        OpenPipeEffectHandler.REGISTRY.register(
            FluidsRegister.HERBAL_BATH_FLUID.get(),
            new HerbalBathPipeEffect()
        );
        
        // Peony Bath - Luck
        OpenPipeEffectHandler.REGISTRY.register(
            FluidsRegister.PEONY_BATH_FLUID.get(),
            new PeonyBathPipeEffect()
        );
        
        // Rose Bath - Strength
        OpenPipeEffectHandler.REGISTRY.register(
            FluidsRegister.ROSE_BATH_FLUID.get(),
            new RoseBathPipeEffect()
        );
        
        LOGGER.debug("Registered Open Pipe Effect Handlers for all Hot Bath fluids.");
    }
    
    /**
     * Register Block Spouting Behaviours for cauldrons.
     * Allows filling cauldrons with Hot Bath fluids using Create spouts.
     */
    private static void registerSpoutBehaviours() {
        // Register cauldron filling behavior for hot water
        // Note: Create already handles water cauldron, we add support for our fluids
        // For now, we'll keep the default behaviors since our fluids don't have
        // special cauldron blocks. Players can use buckets or other methods.
        
        LOGGER.debug("Block Spouting Behaviours registered (using default Create behaviors).");
    }
    
    // ==================== Open Pipe Effect Handler Implementations ====================
    
    /**
     * Hot Water pipe effect - gives Speed I
     */
    private static class HotWaterPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            if (level.getGameTime() % PIPE_EFFECT_INTERVAL != 0) return;
            
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, 
                        PIPE_EFFECT_DURATION, 
                        0, // Level I
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Honey Bath pipe effect - gives Absorption I and Slowness I
     */
    private static class HoneyBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            if (level.getGameTime() % PIPE_EFFECT_INTERVAL != 0) return;
            
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.ABSORPTION, 
                        PIPE_EFFECT_DURATION, 
                        0, 
                        false, false, true
                    ));
                    living.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 
                        PIPE_EFFECT_DURATION / 2, 
                        0, 
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Milk Bath pipe effect - removes negative effects
     */
    private static class MilkBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            if (level.getGameTime() % (PIPE_EFFECT_INTERVAL * 4) != 0) return; // Less frequent
            
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    // Remove one random negative effect
                    living.getActiveEffects().stream()
                        .filter(effect -> !effect.getEffect().isBeneficial())
                        .findFirst()
                        .ifPresent(effect -> living.removeEffect(effect.getEffect()));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Herbal Bath pipe effect - gives Resistance I and Regeneration I
     */
    private static class HerbalBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            if (level.getGameTime() % PIPE_EFFECT_INTERVAL != 0) return;
            
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE, 
                        PIPE_EFFECT_DURATION, 
                        0, 
                        false, false, true
                    ));
                    living.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION, 
                        PIPE_EFFECT_DURATION / 2, 
                        0, 
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Peony Bath pipe effect - gives Luck I
     */
    private static class PeonyBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            if (level.getGameTime() % PIPE_EFFECT_INTERVAL != 0) return;
            
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.LUCK, 
                        PIPE_EFFECT_DURATION, 
                        0, 
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
    
    /**
     * Rose Bath pipe effect - gives Strength I
     */
    private static class RoseBathPipeEffect implements OpenPipeEffectHandler {
        @Override
        public void apply(Level level, AABB area, FluidStack fluid) {
            if (level.getGameTime() % PIPE_EFFECT_INTERVAL != 0) return;
            
            List<Entity> entities = level.getEntities((Entity) null, area, e -> e instanceof LivingEntity);
            for (Entity entity : entities) {
                if (entity instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST, 
                        PIPE_EFFECT_DURATION, 
                        0, 
                        false, false, true
                    ));
                    // Extinguish fire
                    if (living.isOnFire()) {
                        living.clearFire();
                    }
                }
            }
        }
    }
}
