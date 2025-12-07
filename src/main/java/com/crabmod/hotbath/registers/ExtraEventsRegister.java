package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.advancements.AdvancementTrigger;
import com.crabmod.hotbath.particles.SteamParticle;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.registries.RegisterEvent;

@EventBusSubscriber(modid = HotBath.MOD_ID)
public class ExtraEventsRegister {
    @SubscribeEvent
    public static void registerParticlesFactories(final RegisterParticleProvidersEvent event) {
        Minecraft.getInstance()
                .particleEngine
                .register(ParticleRegister.STEAM_PARTICLE.get(), SteamParticle.CozySmokeFactory::new);
    }

    @SubscribeEvent
    public static void registerAdvancementTrigger(RegisterEvent event) {
        CriteriaTriggers.register(new AdvancementTrigger("hotbath", "foot_health"));
        CriteriaTriggers.register(new AdvancementTrigger("hotbath", "milk_skin"));
        CriteriaTriggers.register(new AdvancementTrigger("hotbath", "chronic_invalid"));
        CriteriaTriggers.register(new AdvancementTrigger("hotbath", "rose_body_fragrance"));
    }
}











