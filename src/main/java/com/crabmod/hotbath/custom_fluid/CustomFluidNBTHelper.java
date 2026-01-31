package com.crabmod.hotbath.custom_fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Helper class for storing and retrieving custom fluid data from item stacks.
 * Uses NBT tags instead of NeoForge 1.21's DataComponents system.
 * 
 * <p>This allows a single item type (bucket, bottle, etc.) to represent
 * any custom fluid defined in data packs.</p>
 */
public class CustomFluidNBTHelper {
    
    public static final String TAG_CUSTOM_FLUID = "HotbathCustomFluid";
    public static final String TAG_FLUID_ID = "FluidId";
    public static final String TAG_FLUID_COLOR = "FluidColor";
    public static final String TAG_FLUID_NAME = "FluidName";

    /**
     * Sets the custom fluid ID on an item stack.
     * 
     * @param stack The item stack
     * @param fluidId The fluid ID to set
     */
    public static void setFluidId(ItemStack stack, ResourceLocation fluidId) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_CUSTOM_FLUID);
        tag.putString(TAG_FLUID_ID, fluidId.toString());
        
        // Also cache the color and name for quick access
        CustomFluidRegistry.getDefinition(fluidId).ifPresent(definition -> {
            tag.putInt(TAG_FLUID_COLOR, definition.color());
            tag.putString(TAG_FLUID_NAME, definition.getTranslationKey());
        });
    }

    /**
     * Gets the custom fluid ID from an item stack.
     * 
     * @param stack The item stack
     * @return The fluid ID, or null if not present
     */
    public static ResourceLocation getFluidId(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_CUSTOM_FLUID);
        if (tag != null && tag.contains(TAG_FLUID_ID)) {
            String id = tag.getString(TAG_FLUID_ID);
            if (!id.isEmpty()) {
                try {
                    return new ResourceLocation(id);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Gets the custom fluid color from an item stack.
     * 
     * @param stack The item stack
     * @return The color as RGB, or -1 if not present
     */
    public static int getFluidColor(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_CUSTOM_FLUID);
        if (tag != null && tag.contains(TAG_FLUID_COLOR)) {
            return tag.getInt(TAG_FLUID_COLOR);
        }
        
        // Fallback: get from definition if ID is present
        ResourceLocation id = getFluidId(stack);
        if (id != null) {
            return CustomFluidRegistry.getRuntimeData(id)
                    .map(CustomFluidRegistry.RuntimeFluidData::tintColor)
                    .orElse(-1);
        }
        
        return -1;
    }

    /**
     * Gets the custom fluid translation key from an item stack.
     * 
     * @param stack The item stack
     * @return The translation key, or null if not present
     */
    public static String getFluidNameKey(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_CUSTOM_FLUID);
        if (tag != null && tag.contains(TAG_FLUID_NAME)) {
            return tag.getString(TAG_FLUID_NAME);
        }
        
        // Fallback: get from definition if ID is present
        ResourceLocation id = getFluidId(stack);
        if (id != null) {
            return CustomFluidRegistry.getDefinition(id)
                    .map(CustomFluidDefinition::getTranslationKey)
                    .orElse(null);
        }
        
        return null;
    }

    /**
     * Gets the custom fluid definition from an item stack.
     * 
     * @param stack The item stack
     * @return Optional containing the fluid definition if found
     */
    public static Optional<CustomFluidDefinition> getFluidDefinitionOptional(ItemStack stack) {
        ResourceLocation id = getFluidId(stack);
        if (id != null) {
            return CustomFluidRegistry.getDefinition(id);
        }
        return Optional.empty();
    }

    /**
     * Gets the custom fluid definition from an item stack.
     * 
     * @param stack The item stack
     * @return The fluid definition, or null if not found
     */
    public static CustomFluidDefinition getFluidDefinition(ItemStack stack) {
        return getFluidDefinitionOptional(stack).orElse(null);
    }

    /**
     * Checks if an item stack has custom fluid data.
     * 
     * @param stack The item stack
     * @return true if the stack has custom fluid data
     */
    public static boolean hasFluidData(ItemStack stack) {
        return getFluidId(stack) != null;
    }

    /**
     * Creates a new item stack with the specified fluid data.
     * 
     * @param stack The base item stack
     * @param fluidId The fluid ID to set
     * @return A copy of the stack with fluid data set
     */
    public static ItemStack withFluidId(ItemStack stack, ResourceLocation fluidId) {
        ItemStack copy = stack.copy();
        setFluidId(copy, fluidId);
        return copy;
    }

    /**
     * Removes custom fluid data from an item stack.
     * 
     * @param stack The item stack
     */
    public static void clearFluidData(ItemStack stack) {
        stack.removeTagKey(TAG_CUSTOM_FLUID);
    }

    /**
     * Creates a new item stack with the specified fluid data.
     * 
     * @param item The item to create
     * @param fluidId The fluid ID to set
     * @return A new stack with fluid data set, or empty if fluid not found
     */
    public static ItemStack createStack(net.minecraft.world.item.Item item, ResourceLocation fluidId) {
        if (!CustomFluidRegistry.isRegistered(fluidId)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        setFluidId(stack, fluidId);
        return stack;
    }

    /**
     * Creates a new item stack with the specified fluid data.
     * 
     * @param item The item to create
     * @param definition The fluid definition
     * @return A new stack with fluid data set
     */
    public static ItemStack createStack(net.minecraft.world.item.Item item, CustomFluidDefinition definition) {
        ItemStack stack = new ItemStack(item);
        setFluidId(stack, definition.id());
        return stack;
    }
}
