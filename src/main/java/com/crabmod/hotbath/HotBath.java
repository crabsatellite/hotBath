package com.crabmod.hotbath;

import com.crabmod.hotbath.compat.ColdSweatCompat;
import com.crabmod.hotbath.compat.ColdSweatIntegration;
import com.crabmod.hotbath.compat.LegendarySurvivalOverhaulIntegration;
import com.crabmod.hotbath.compat.LSOCompat;
import com.crabmod.hotbath.compat.ToughAsNailsCompat;
import com.crabmod.hotbath.compat.ToughAsNailsIntegration;
import com.crabmod.hotbath.fluid_details.HotbathFluidType;
import com.crabmod.hotbath.item.ItemGroup;
import com.crabmod.hotbath.registers.BlocksRegister;
import com.crabmod.hotbath.registers.EntityRegister;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.registers.ItemRegister;
import com.crabmod.hotbath.registers.ParticleRegister;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(HotBath.MOD_ID)
public class HotBath {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "hotbath";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public HotBath(ModContainer modContainer, IEventBus modEventBus) {
        ItemGroup.register(modEventBus);
        FluidsRegister.register(modEventBus);
        BlocksRegister.register(modEventBus);
        ItemRegister.register(modEventBus);
        ParticleRegister.register(modEventBus);
        EntityRegister.register(modEventBus);
        HotbathFluidType.register(modEventBus);
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
//    modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.HOT_WATER_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HOT_WATER_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.HONEY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HONEY_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.MILK_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_MILK_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.HERBAL_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HERBAL_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.PEONY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_PEONY_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.ROSE_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_ROSE_BATH_BOTTLE.get().getDefaultInstance());
        });

        if (ColdSweatIntegration.isColdSweatLoaded()) {
            LOGGER.info("Cold Sweat detected! Temperature integration enabled.");
            try {
                ColdSweatCompat.init();
                LOGGER.info("Cold Sweat event handler registered successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to register Cold Sweat event handler: {}", e.getMessage(), e);
            }
        }

        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            LOGGER.info("Tough As Nails detected! Temperature integration enabled.");
            try {
                ToughAsNailsCompat.init();
                LOGGER.info("Tough As Nails integration registered successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize Tough As Nails integration: {}", e.getMessage(), e);
            }
        }

        if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            LOGGER.info("Legendary Survival Overhaul detected! Integration enabled.");
            try {
                LSOCompat.init();
                LOGGER.info("LSO integration registered successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to register LSO integration: {}", e.getMessage(), e);
            }
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class
    // annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityRenderers.register(EntityRegister.THROWN_BATH_WATER.get(), ThrownItemRenderer::new);
        }

        @SubscribeEvent
        public static void registerParticles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ParticleRegister.HOT_WATER_SPLASH.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.HONEY_WATER_SPLASH.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.MILK_WATER_SPLASH.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.HERBAL_WATER_SPLASH.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.PEONY_WATER_SPLASH.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
            event.registerSpriteSet(ParticleRegister.ROSE_WATER_SPLASH.get(), net.minecraft.client.particle.SpellParticle.Provider::new);
        }
    }
}