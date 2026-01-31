package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers the base custom fluid items.
 * These items use data components to store the specific fluid type,
 * allowing a single item registration to represent any custom fluid.
 * 
 * <p>The items are:</p>
 * <ul>
 *   <li>Custom Fluid Bucket - Places the fluid in the world</li>
 *   <li>Custom Fluid Bottle - Drinkable, applies effects from the fluid definition</li>
 *   <li>Splash Custom Fluid Bottle - Throwable, applies effects to nearby entities</li>
 * </ul>
 */
public class CustomFluidItems {
    
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, HotBath.MOD_ID);

    /**
     * A bucket that can contain any custom fluid.
     * The fluid type is stored in the item's data components.
     */
    public static final DeferredHolder<Item, CustomFluidBucketItem> CUSTOM_FLUID_BUCKET =
            ITEMS.register("custom_fluid_bucket",
                    () -> new CustomFluidBucketItem(new Item.Properties().stacksTo(1)));

    /**
     * A drinkable bottle that can contain any custom fluid.
     * Applies effects from the fluid definition when consumed.
     */
    public static final DeferredHolder<Item, CustomFluidBottleItem> CUSTOM_FLUID_BOTTLE =
            ITEMS.register("custom_fluid_bottle",
                    () -> new CustomFluidBottleItem(new Item.Properties().stacksTo(16)));

    /**
     * A throwable splash bottle that can contain any custom fluid.
     * Applies effects to nearby entities when thrown.
     */
    public static final DeferredHolder<Item, SplashCustomFluidBottleItem> CUSTOM_FLUID_SPLASH_BOTTLE =
            ITEMS.register("splash_custom_fluid_bottle",
                    () -> new SplashCustomFluidBottleItem(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
