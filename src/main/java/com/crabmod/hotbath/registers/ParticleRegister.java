package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleRegister {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, HotBath.MOD_ID);

    public static final RegistryObject<SimpleParticleType> STEAM_PARTICLE =
            PARTICLE_TYPES.register("steam_particle", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> HOT_WATER_SPLASH =
            PARTICLE_TYPES.register("hot_water_splash", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> HONEY_WATER_SPLASH =
            PARTICLE_TYPES.register("honey_water_splash", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> MILK_WATER_SPLASH =
            PARTICLE_TYPES.register("milk_water_splash", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> HERBAL_WATER_SPLASH =
            PARTICLE_TYPES.register("herbal_water_splash", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> PEONY_WATER_SPLASH =
            PARTICLE_TYPES.register("peony_water_splash", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> ROSE_WATER_SPLASH =
            PARTICLE_TYPES.register("rose_water_splash", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> HOT_WATER_EFFECT =
            PARTICLE_TYPES.register("hot_water_effect", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> HONEY_BATH_EFFECT =
            PARTICLE_TYPES.register("honey_bath_effect", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> MILK_BATH_EFFECT =
            PARTICLE_TYPES.register("milk_bath_effect", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> HERBAL_BATH_EFFECT =
            PARTICLE_TYPES.register("herbal_bath_effect", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> PEONY_BATH_EFFECT =
            PARTICLE_TYPES.register("peony_bath_effect", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> ROSE_BATH_EFFECT =
            PARTICLE_TYPES.register("rose_bath_effect", () -> new SimpleParticleType(true));

    // Bubbles
    public static final RegistryObject<SimpleParticleType> HOT_WATER_BUBBLE =
            PARTICLE_TYPES.register("hot_water_bubble", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> HONEY_BATH_BUBBLE =
            PARTICLE_TYPES.register("honey_bath_bubble", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> MILK_BATH_BUBBLE =
            PARTICLE_TYPES.register("milk_bath_bubble", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> HERBAL_BATH_BUBBLE =
            PARTICLE_TYPES.register("herbal_bath_bubble", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PEONY_BATH_BUBBLE =
            PARTICLE_TYPES.register("peony_bath_bubble", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> ROSE_BATH_BUBBLE =
            PARTICLE_TYPES.register("rose_bath_bubble", () -> new SimpleParticleType(false));

    // Dripping (Hanging)
    public static final RegistryObject<SimpleParticleType> DRIPPING_HOT_WATER =
            PARTICLE_TYPES.register("dripping_hot_water", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DRIPPING_HONEY_BATH =
            PARTICLE_TYPES.register("dripping_honey_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DRIPPING_MILK_BATH =
            PARTICLE_TYPES.register("dripping_milk_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DRIPPING_HERBAL_BATH =
            PARTICLE_TYPES.register("dripping_herbal_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DRIPPING_PEONY_BATH =
            PARTICLE_TYPES.register("dripping_peony_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DRIPPING_ROSE_BATH =
            PARTICLE_TYPES.register("dripping_rose_bath", () -> new SimpleParticleType(false));

    // Falling
    public static final RegistryObject<SimpleParticleType> FALLING_HOT_WATER =
            PARTICLE_TYPES.register("falling_hot_water", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FALLING_HONEY_BATH =
            PARTICLE_TYPES.register("falling_honey_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FALLING_MILK_BATH =
            PARTICLE_TYPES.register("falling_milk_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FALLING_HERBAL_BATH =
            PARTICLE_TYPES.register("falling_herbal_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FALLING_PEONY_BATH =
            PARTICLE_TYPES.register("falling_peony_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> FALLING_ROSE_BATH =
            PARTICLE_TYPES.register("falling_rose_bath", () -> new SimpleParticleType(false));

    // Landing
    public static final RegistryObject<SimpleParticleType> LANDING_HOT_WATER =
            PARTICLE_TYPES.register("landing_hot_water", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> LANDING_HONEY_BATH =
            PARTICLE_TYPES.register("landing_honey_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> LANDING_MILK_BATH =
            PARTICLE_TYPES.register("landing_milk_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> LANDING_HERBAL_BATH =
            PARTICLE_TYPES.register("landing_herbal_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> LANDING_PEONY_BATH =
            PARTICLE_TYPES.register("landing_peony_bath", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> LANDING_ROSE_BATH =
            PARTICLE_TYPES.register("landing_rose_bath", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
