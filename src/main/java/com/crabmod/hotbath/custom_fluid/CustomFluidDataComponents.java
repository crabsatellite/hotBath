package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

/**
 * Data components for storing custom fluid information on items.
 * This allows a single item type (bucket, bottle, etc.) to represent
 * any custom fluid defined in data packs.
 * 
 * <p>The fluid ID is stored as a string (ResourceLocation) in the item's
 * data components, which allows for dynamic fluid types without needing
 * to register separate items for each fluid.</p>
 */
public class CustomFluidDataComponents {
    
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = 
            DeferredRegister.createDataComponents(HotBath.MOD_ID);

    /**
     * Stores the custom fluid ID (as ResourceLocation string) on an item.
     * Example: "hotbath:golden_bath"
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CUSTOM_FLUID_ID = 
            DATA_COMPONENTS.registerComponentType(
                    "custom_fluid_id",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    /**
     * Stores the custom fluid color as an ARGB integer.
     * This is cached from the fluid definition for quick access during rendering.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CUSTOM_FLUID_COLOR = 
            DATA_COMPONENTS.registerComponentType(
                    "custom_fluid_color",
                    builder -> builder
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
            );

    /**
     * Stores the custom fluid display name key for localization.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CUSTOM_FLUID_NAME = 
            DATA_COMPONENTS.registerComponentType(
                    "custom_fluid_name",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
            );

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }

    /**
     * Gets the custom fluid ID from an item stack.
     * 
     * @param stack The item stack
     * @return The fluid ID, or null if not present
     */
    public static ResourceLocation getFluidId(net.minecraft.world.item.ItemStack stack) {
        String id = stack.get(CUSTOM_FLUID_ID.get());
        if (id != null && !id.isEmpty()) {
            try {
                return ResourceLocation.parse(id);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the custom fluid color from an item stack.
     * If the color component is not set but the fluid ID is, looks up the color from the registry.
     * 
     * @param stack The item stack
     * @return The color as ARGB, or -1 if not present
     */
    public static int getFluidColor(net.minecraft.world.item.ItemStack stack) {
        // First try to get the cached color component
        Integer color = stack.get(CUSTOM_FLUID_COLOR.get());
        if (color != null) {
            return color;
        }
        
        // Fallback: look up color from fluid definition by ID
        // This handles cases where only custom_fluid_id is set (e.g., via /give command)
        ResourceLocation fluidId = getFluidId(stack);
        if (fluidId != null) {
            return CustomFluidRegistry.getDefinition(fluidId)
                    .map(def -> 0xFF000000 | def.color())
                    .orElse(-1);
        }
        
        return -1;
    }

    /**
     * Gets the custom fluid definition from an item stack.
     * 
     * @param stack The item stack
     * @return The fluid definition, or null if not found
     */
    public static CustomFluidDefinition getFluidDefinition(net.minecraft.world.item.ItemStack stack) {
        ResourceLocation id = getFluidId(stack);
        if (id != null) {
            return CustomFluidRegistry.getDefinition(id).orElse(null);
        }
        return null;
    }

    /**
     * Sets the custom fluid on an item stack.
     * 
     * @param stack The item stack to modify
     * @param definition The fluid definition
     */
    public static void setFluid(net.minecraft.world.item.ItemStack stack, CustomFluidDefinition definition) {
        stack.set(CUSTOM_FLUID_ID.get(), definition.id().toString());
        stack.set(CUSTOM_FLUID_COLOR.get(), 0xFF000000 | definition.color());
        stack.set(CUSTOM_FLUID_NAME.get(), "item.hotbath.custom_fluid." + definition.id().getPath());
    }

    /**
     * Creates an item stack with the specified custom fluid.
     * 
     * @param item The item to create a stack of
     * @param definition The fluid definition
     * @return A new item stack with the fluid data
     */
    public static net.minecraft.world.item.ItemStack createStack(
            net.minecraft.world.item.Item item, 
            CustomFluidDefinition definition) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
        setFluid(stack, definition);
        return stack;
    }

    /**
     * Creates an item stack with the specified custom fluid by ID.
     * 
     * @param item The item to create a stack of
     * @param fluidId The fluid ID
     * @return A new item stack with the fluid data, or empty if fluid not found
     */
    public static net.minecraft.world.item.ItemStack createStack(
            net.minecraft.world.item.Item item,
            ResourceLocation fluidId) {
        return CustomFluidRegistry.getDefinition(fluidId)
                .map(def -> createStack(item, def))
                .orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }
}
