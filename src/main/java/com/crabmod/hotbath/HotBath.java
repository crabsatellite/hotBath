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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(HotBath.MOD_ID)
public class HotBath {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "hotbath";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public HotBath() {
        this(FMLJavaModLoadingContext.get());
    }

    public HotBath(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ItemGroup.register(modEventBus);
        FluidsRegister.register(modEventBus);
        BlocksRegister.register(modEventBus);
        ItemRegister.register(modEventBus);
        ParticleRegister.register(modEventBus);
        EntityRegister.register(modEventBus);
        HotbathFluidType.register(modEventBus);
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::enqueueIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
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

        if (LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            LOGGER.info("Legendary Survival Overhaul detected! Integration enabled.");
            try {
                LSOCompat.init();
                LOGGER.info("LSO integration registered successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to register LSO integration: {}", e.getMessage(), e);
            }
        }
        
        event.enqueueWork(() -> {
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.HOT_WATER_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HOT_WATER_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.HONEY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HONEY_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.MILK_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_MILK_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.HERBAL_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HERBAL_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.PEONY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_PEONY_BATH_BOTTLE.get().getDefaultInstance());
            BrewingRecipeRegistry.addRecipe(Ingredient.of(ItemRegister.ROSE_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_ROSE_BATH_BOTTLE.get().getDefaultInstance());
        });
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        if (ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            LOGGER.info("Tough As Nails detected! Temperature integration enabled.");
            try {
                ToughAsNailsCompat.init();
                LOGGER.info("Tough As Nails integration registered successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize Tough As Nails integration: {}", e.getMessage(), e);
            }
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
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

    /*
    @SubscribeEvent
    public void onBrewingRecipeRegister(net.minecraftforge.event.brewing.RegisterBrewingRecipesEvent event) {
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.HOT_WATER_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HOT_WATER_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.HONEY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HONEY_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.MILK_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_MILK_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.HERBAL_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_HERBAL_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.PEONY_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_PEONY_BATH_BOTTLE.get().getDefaultInstance());
        event.getBuilder().addRecipe(Ingredient.of(ItemRegister.ROSE_BATH_BOTTLE.get()), Ingredient.of(Items.GUNPOWDER), ItemRegister.SPLASH_ROSE_BATH_BOTTLE.get().getDefaultInstance());
    }
    */
}









