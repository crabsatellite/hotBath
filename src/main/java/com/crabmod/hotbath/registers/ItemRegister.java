package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.items.BathBucketItem;
import com.crabmod.hotbath.items.BathWaterBottleItem;
import com.crabmod.hotbath.items.BathWaterEffects;
import com.crabmod.hotbath.items.DescriptiveItem;
import com.crabmod.hotbath.items.SplashBathWaterBottleItem;
import com.crabmod.hotbath.items.ThrownBathWater;
import net.minecraft.core.registries.BuiltInRegistries;
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
                            new BathBucketItem(
                                    FluidsRegister.HERBAL_BATH_FLUID,
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> HONEY_BATH_BUCKET =
            ITEMS.register(
                    "honey_bath_bucket",
                    () ->
                            new BathBucketItem(
                                    FluidsRegister.HONEY_BATH_FLUID,
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> HOT_WATER_BUCKET = ITEMS.register("hot_water_bucket",
            () -> new BathBucketItem(FluidsRegister.HOT_WATER_FLUID,
                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> MILK_BATH_BUCKET =
            ITEMS.register(
                    "milk_bath_bucket",
                    () ->
                            new BathBucketItem(
                                    FluidsRegister.MILK_BATH_FLUID,
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> PEONY_BATH_BUCKET =
            ITEMS.register(
                    "peony_bath_bucket",
                    () ->
                            new BathBucketItem(
                                    FluidsRegister.PEONY_BATH_FLUID,
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> ROSE_BATH_BUCKET =
            ITEMS.register(
                    "rose_bath_bucket",
                    () ->
                            new BathBucketItem(
                                    FluidsRegister.ROSE_BATH_FLUID,
                                    new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> BATH_HERB =
            ITEMS.register("bath_herb", () -> new DescriptiveItem(new Item.Properties()));

    // Bath Water Bottles
    public static final DeferredHolder<Item, Item> HOT_WATER_BOTTLE =
            ITEMS.register("hot_water_bottle",
                    () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::hotWaterEffect));

    public static final DeferredHolder<Item, Item> HONEY_BATH_BOTTLE =
            ITEMS.register("honey_bath_bottle",
                    () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::honeyBathEffect));

    public static final DeferredHolder<Item, Item> MILK_BATH_BOTTLE =
            ITEMS.register("milk_bath_bottle",
                    () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::milkBathEffect));

    public static final DeferredHolder<Item, Item> HERBAL_BATH_BOTTLE =
            ITEMS.register("herbal_bath_bottle",
                    () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::herbalBathEffect));

    public static final DeferredHolder<Item, Item> PEONY_BATH_BOTTLE =
            ITEMS.register("peony_bath_bottle",
                    () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::peonyBathEffect));

    public static final DeferredHolder<Item, Item> ROSE_BATH_BOTTLE =
            ITEMS.register("rose_bath_bottle",
                    () -> new BathWaterBottleItem(new Item.Properties().stacksTo(16), BathWaterEffects::roseBathEffect));

    // Splash Bath Water Bottles
    public static final DeferredHolder<Item, Item> SPLASH_HOT_WATER_BOTTLE =
            ITEMS.register("splash_hot_water_bottle",
                    () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), 
                            BathWaterEffects::hotWaterEffect, ParticleRegister.HOT_WATER_SPLASH));

    public static final DeferredHolder<Item, Item> SPLASH_HONEY_BATH_BOTTLE =
            ITEMS.register("splash_honey_bath_bottle",
                    () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), 
                            BathWaterEffects::honeyBathEffect, ParticleRegister.HONEY_WATER_SPLASH));

    public static final DeferredHolder<Item, Item> SPLASH_MILK_BATH_BOTTLE =
            ITEMS.register("splash_milk_bath_bottle",
                    () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), 
                            BathWaterEffects::milkBathEffect, ParticleRegister.MILK_WATER_SPLASH));

    public static final DeferredHolder<Item, Item> SPLASH_HERBAL_BATH_BOTTLE =
            ITEMS.register("splash_herbal_bath_bottle",
                    () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), 
                            BathWaterEffects::herbalBathEffect, ParticleRegister.HERBAL_WATER_SPLASH));

    public static final DeferredHolder<Item, Item> SPLASH_PEONY_BATH_BOTTLE =
            ITEMS.register("splash_peony_bath_bottle",
                    () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), 
                            BathWaterEffects::peonyBathEffect, ParticleRegister.PEONY_WATER_SPLASH));

    public static final DeferredHolder<Item, Item> SPLASH_ROSE_BATH_BOTTLE =
            ITEMS.register("splash_rose_bath_bottle",
                    () -> new SplashBathWaterBottleItem(new Item.Properties().stacksTo(16), 
                            BathWaterEffects::roseBathEffect, ParticleRegister.ROSE_WATER_SPLASH));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
