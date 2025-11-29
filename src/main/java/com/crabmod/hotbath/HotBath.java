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
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.registers.ItemRegister;
import com.crabmod.hotbath.registers.ParticleRegister;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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
        HotbathFluidType.register(modEventBus);
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
//    modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

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
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HOT_WATER_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HOT_WATER_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HONEY_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HONEY_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.MILK_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.MILK_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.PEONY_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.PEONY_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.ROSE_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.ROSE_BATH_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HERBAL_BATH_FLUID.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(
                    FluidsRegister.HERBAL_BATH_FLOWING.get(), RenderType.translucent());
        }
    }
}