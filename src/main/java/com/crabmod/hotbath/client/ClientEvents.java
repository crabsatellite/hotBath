package com.crabmod.hotbath.client;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.client.particle.CustomDripParticle;
import com.crabmod.hotbath.client.particle.FlyParticle;
import com.crabmod.hotbath.client.particle.HotBathBubbleParticle;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.CustomFluidItems;
import com.crabmod.hotbath.custom_fluid.CustomFluidNBTHelper;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.particles.SteamParticle;
import com.crabmod.hotbath.registers.EntityRegister;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.registers.ParticleRegister;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = HotBath.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    
    private static final ResourceLocation HOT_STEAM_PREDICATE = new ResourceLocation(HotBath.MOD_ID, "hot_steam");
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(EntityRegister.THROWN_BATH_WATER.get(), ThrownItemRenderer::new);

        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HOT_WATER_FLUID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HOT_WATER_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HONEY_BATH_FLUID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HONEY_BATH_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.MILK_BATH_FLUID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.MILK_BATH_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HERBAL_BATH_FLUID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.HERBAL_BATH_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.PEONY_BATH_FLUID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.PEONY_BATH_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.ROSE_BATH_FLUID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(FluidsRegister.ROSE_BATH_FLOWING.get(), RenderType.translucent());
        
        // Dynamic custom fluids (data pack fluids) - need translucent rendering for transparency
        ItemBlockRenderTypes.setRenderLayer(DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get(), RenderType.translucent());
        
        // Register item property predicates for model overrides
        event.enqueueWork(() -> {
            registerHotSteamPredicate();
        });
    }
    
    /**
     * Registers the hot_steam predicate for custom fluid items.
     * This allows models to switch between with-smoke and without-smoke variants
     * based on whether the fluid is hot AND has showSteam enabled.
     */
    private static void registerHotSteamPredicate() {
        // Custom fluid bucket
        ItemProperties.register(CustomFluidItems.CUSTOM_FLUID_BUCKET.get(), HOT_STEAM_PREDICATE, 
            (stack, world, entity, seed) -> {
                CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
                return (definition != null && definition.isHot() && definition.showSteam()) ? 1.0f : 0.0f;
            });
        
        // Custom fluid bottle
        ItemProperties.register(CustomFluidItems.CUSTOM_FLUID_BOTTLE.get(), HOT_STEAM_PREDICATE, 
            (stack, world, entity, seed) -> {
                CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
                return (definition != null && definition.isHot() && definition.showSteam()) ? 1.0f : 0.0f;
            });
        
        // Splash custom fluid bottle
        ItemProperties.register(CustomFluidItems.CUSTOM_FLUID_SPLASH_BOTTLE.get(), HOT_STEAM_PREDICATE, 
            (stack, world, entity, seed) -> {
                CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
                return (definition != null && definition.isHot() && definition.showSteam()) ? 1.0f : 0.0f;
            });
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegister.STEAM_PARTICLE.get(), SteamParticle.CozySmokeFactory::new);
        event.registerSpriteSet(ParticleRegister.HOT_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.HONEY_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.MILK_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.HERBAL_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.PEONY_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.ROSE_WATER_SPLASH.get(), net.minecraft.client.particle.SplashParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.HOT_WATER_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.HONEY_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.MILK_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.HERBAL_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.PEONY_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.ROSE_BATH_EFFECT.get(), net.minecraft.client.particle.SpellParticle.Provider::new);

        // Bubbles
        event.registerSpriteSet(ParticleRegister.HOT_WATER_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.HONEY_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.MILK_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.HERBAL_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.PEONY_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);
        event.registerSpriteSet(ParticleRegister.ROSE_BATH_BUBBLE.get(), HotBathBubbleParticle.Provider::new);

        // Dripping (Hanging)
        event.registerSpriteSet(ParticleRegister.DRIPPING_HOT_WATER.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_HOT_WATER.get(), ParticleRegister.LANDING_HOT_WATER.get()));
        event.registerSpriteSet(ParticleRegister.DRIPPING_HONEY_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_HONEY_BATH.get(), ParticleRegister.LANDING_HONEY_BATH.get()));
        event.registerSpriteSet(ParticleRegister.DRIPPING_MILK_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_MILK_BATH.get(), ParticleRegister.LANDING_MILK_BATH.get()));
        event.registerSpriteSet(ParticleRegister.DRIPPING_HERBAL_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_HERBAL_BATH.get(), ParticleRegister.LANDING_HERBAL_BATH.get()));
        event.registerSpriteSet(ParticleRegister.DRIPPING_PEONY_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_PEONY_BATH.get(), ParticleRegister.LANDING_PEONY_BATH.get()));
        event.registerSpriteSet(ParticleRegister.DRIPPING_ROSE_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, ParticleRegister.FALLING_ROSE_BATH.get(), ParticleRegister.LANDING_ROSE_BATH.get()));

        // Falling
        event.registerSpriteSet(ParticleRegister.FALLING_HOT_WATER.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_HOT_WATER.get()));
        event.registerSpriteSet(ParticleRegister.FALLING_HONEY_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_HONEY_BATH.get()));
        event.registerSpriteSet(ParticleRegister.FALLING_MILK_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_MILK_BATH.get()));
        event.registerSpriteSet(ParticleRegister.FALLING_HERBAL_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_HERBAL_BATH.get()));
        event.registerSpriteSet(ParticleRegister.FALLING_PEONY_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_PEONY_BATH.get()));
        event.registerSpriteSet(ParticleRegister.FALLING_ROSE_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, ParticleRegister.LANDING_ROSE_BATH.get()));

        // Landing
        event.registerSpriteSet(ParticleRegister.LANDING_HOT_WATER.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
        event.registerSpriteSet(ParticleRegister.LANDING_HONEY_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
        event.registerSpriteSet(ParticleRegister.LANDING_MILK_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
        event.registerSpriteSet(ParticleRegister.LANDING_HERBAL_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
        event.registerSpriteSet(ParticleRegister.LANDING_PEONY_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));
        event.registerSpriteSet(ParticleRegister.LANDING_ROSE_BATH.get(), 
            sprite -> new CustomDripParticle.Factory(sprite, net.minecraft.world.level.material.Fluids.WATER, null, null));

        // Fly particle for extremely dirty players
        event.registerSpriteSet(ParticleRegister.FLY.get(), FlyParticle.Factory::new);
    }
}
