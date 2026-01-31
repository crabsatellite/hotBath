package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers the base custom fluid items.
 * These items use NBT to store the specific fluid type,
 * allowing a single item registration to represent any custom fluid.
 * 
 * <p>The items are:</p>
 * <ul>
 *   <li>Custom Fluid Bucket - Places the fluid in the world</li>
 *   <li>Custom Fluid Bottle - Drinkable, applies effects from the fluid definition</li>
 *   <li>Splash Custom Fluid Bottle - Throwable, applies effects to entities</li>
 * </ul> * 
 * <p><b>Command usage (1.20 Forge):</b></p>
 * <pre>
 * /give @p hotbath:custom_fluid_bucket{HotbathCustomFluid:{FluidId:"hotbath:golden_bath"}} 1
 * /give @p hotbath:custom_fluid_bottle{HotbathCustomFluid:{FluidId:"hotbath:golden_bath"}} 16
 * /give @p hotbath:splash_custom_fluid_bottle{HotbathCustomFluid:{FluidId:"hotbath:golden_bath"}} 16
 * </pre> */
public class CustomFluidItems {
    
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HotBath.MOD_ID);

    /**
     * A bucket that can contain any custom fluid.
     * The fluid type is stored in the item's NBT.
     */
    public static final RegistryObject<CustomFluidBucketItem> CUSTOM_FLUID_BUCKET =
            ITEMS.register("custom_fluid_bucket",
                    () -> new CustomFluidBucketItem(new Item.Properties().stacksTo(1)));

    /**
     * A drinkable bottle that can contain any custom fluid.
     * Applies effects from the fluid definition when consumed.
     */
    public static final RegistryObject<CustomFluidBottleItem> CUSTOM_FLUID_BOTTLE =
            ITEMS.register("custom_fluid_bottle",
                    () -> new CustomFluidBottleItem(new Item.Properties().stacksTo(16)));

    /**
     * A throwable splash bottle that can contain any custom fluid.
     * Applies effects to nearby entities when thrown.
     */
    public static final RegistryObject<SplashCustomFluidBottleItem> CUSTOM_FLUID_SPLASH_BOTTLE =
            ITEMS.register("splash_custom_fluid_bottle",
                    () -> new SplashCustomFluidBottleItem(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
