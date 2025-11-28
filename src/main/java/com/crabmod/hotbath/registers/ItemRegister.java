package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegister {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, HotBath.MOD_ID);

    public static final DeferredHolder<Item, Item> HERBAL_BATH_BUCKET =
            ITEMS.register(
                    "herbal_bath_bucket",
                    () ->
                            new BucketItem(
                                    FluidsRegister.HERBAL_BATH_FLUID.get(),
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> HONEY_BATH_BUCKET =
            ITEMS.register(
                    "honey_bath_bucket",
                    () ->
                            new BucketItem(
                                    FluidsRegister.HONEY_BATH_FLUID.get(),
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> HOT_WATER_BUCKET = ITEMS.register("hot_water_bucket",
            () -> new BucketItem(FluidsRegister.HOT_WATER_FLUID.get(),
                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> MILK_BATH_BUCKET =
            ITEMS.register(
                    "milk_bath_bucket",
                    () ->
                            new BucketItem(
                                    FluidsRegister.MILK_BATH_FLUID.get(),
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> PEONY_BATH_BUCKET =
            ITEMS.register(
                    "peony_bath_bucket",
                    () ->
                            new BucketItem(
                                    FluidsRegister.PEONY_BATH_FLUID.get(),
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> ROSE_BATH_BUCKET =
            ITEMS.register(
                    "rose_bath_bucket",
                    () ->
                            new BucketItem(
                                    FluidsRegister.ROSE_BATH_FLUID.get(),
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> BATH_HERB =
            ITEMS.register("bath_herb", () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
