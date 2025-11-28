package com.crabmod.hotbath;

import com.crabmod.hotbath.compat.ColdSweatIntegration;
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
    private static final Logger LOGGER = LogUtils.getLogger();

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
        }
    }

//  Duplicated register
//  private void addCreative(BuildCreativeModeTabContentsEvent event) {
//    if (event.getTab() == ItemGroup.HOT_BATH_TAB.get()) {
//      event.accept(ItemRegister.HERBAL_BATH_BUCKET.get());
//      event.accept(ItemRegister.HONEY_BATH_BUCKET.get());
//      event.accept(ItemRegister.HOT_WATER_BUCKET.get());
//      event.accept(ItemRegister.MILK_BATH_BUCKET.get());
//      event.accept(ItemRegister.PEONY_BATH_BUCKET.get());
//      event.accept(ItemRegister.ROSE_BATH_BUCKET.get());
//      event.accept(ItemRegister.BATH_HERB.get());
//    }
//  }

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

//  // Register the setup method for modloading
//  @ObjectHolder(registryName = "minecraft:block", value = "hotbath:hot_water_block")
//  public static final HotWaterBlock HOT_WATER_BLOCK = null;
//
//  @ObjectHolder(registryName = "minecraft:block", value = "hotbath:herbal_bath_block")
//  public static final HerbalBathBlock HERBAL_BATH_BLOCK = null;
//
//  @ObjectHolder(registryName = "minecraft:block", value = "hotbath:honey_bath_block")
//  public static final HoneyBathBlock HONEY_BATH_BLOCK = null;
//
//  @ObjectHolder(registryName = "minecraft:block", value = "hotbath:milk_bath_block")
//  public static final MilkBathBlock MILK_BATH_BLOCK = null;
//
//  @ObjectHolder(registryName = "minecraft:block", value = "hotbath:peony_bath_block")
//  public static final PeonyBathBlock PEONY_BATH_BLOCK = null;
//
//  @ObjectHolder(registryName = "minecraft:block", value = "hotbath:rose_bath_block")
//  public static final RoseBathBlock ROSE_BATH_BLOCK = null;
}
