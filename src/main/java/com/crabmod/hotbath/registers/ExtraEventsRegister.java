package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.advancements.AdvancementTrigger;
import com.crabmod.hotbath.particles.SteamParticle;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ExtraEventsRegister {
    
    // DeferredRegister for advancement triggers
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = 
            DeferredRegister.create(Registries.TRIGGER_TYPE, HotBath.MOD_ID);
    
    // Register all custom advancement triggers
    public static final Supplier<AdvancementTrigger> FOOT_HEALTH_TRIGGER = 
            TRIGGERS.register("foot_health", () -> new AdvancementTrigger("hotbath", "foot_health"));
    public static final Supplier<AdvancementTrigger> MILK_SKIN_TRIGGER = 
            TRIGGERS.register("milk_skin", () -> new AdvancementTrigger("hotbath", "milk_skin"));
    public static final Supplier<AdvancementTrigger> CHRONIC_INVALID_TRIGGER = 
            TRIGGERS.register("chronic_invalid", () -> new AdvancementTrigger("hotbath", "chronic_invalid"));
    public static final Supplier<AdvancementTrigger> ROSE_BODY_FRAGRANCE_TRIGGER = 
            TRIGGERS.register("rose_body_fragrance", () -> new AdvancementTrigger("hotbath", "rose_body_fragrance"));
    
    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }
    
    @EventBusSubscriber(modid = HotBath.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerParticlesFactories(final RegisterParticleProvidersEvent event) {
            Minecraft.getInstance()
                    .particleEngine
                    .register(ParticleRegister.STEAM_PARTICLE.get(), SteamParticle.CozySmokeFactory::new);
        }
    }
}
