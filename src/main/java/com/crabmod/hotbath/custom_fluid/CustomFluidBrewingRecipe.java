package com.crabmod.hotbath.custom_fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

import java.util.Optional;

/**
 * Dynamic brewing recipe that converts custom fluid bottles to splash bottles.
 * This recipe handles all custom fluids loaded from data packs.
 * 
 * <p>Recipe: Custom Fluid Bottle + Gunpowder = Splash Custom Fluid Bottle
 */
public class CustomFluidBrewingRecipe implements IBrewingRecipe {
    
    @Override
    public boolean isInput(ItemStack input) {
        // Check if the input is a custom fluid bottle
        if (input.isEmpty()) {
            return false;
        }
        
        // Check if it's our custom fluid bottle item
        if (!(input.getItem() instanceof CustomFluidBottleItem)) {
            return false;
        }
        
        // Verify it has a valid fluid ID
        ResourceLocation fluidId = CustomFluidDataComponents.getFluidId(input);
        return fluidId != null && CustomFluidRegistry.getDefinition(fluidId).isPresent();
    }
    
    @Override
    public boolean isIngredient(ItemStack ingredient) {
        // Gunpowder is used to create splash potions
        return ingredient.is(Items.GUNPOWDER);
    }
    
    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        // Validate input and ingredient
        if (!isInput(input) || !isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }
        
        // Get the fluid ID from the input bottle
        ResourceLocation fluidId = CustomFluidDataComponents.getFluidId(input);
        if (fluidId == null) {
            return ItemStack.EMPTY;
        }
        
        // Get the fluid definition
        Optional<CustomFluidDefinition> definitionOpt = CustomFluidRegistry.getDefinition(fluidId);
        if (definitionOpt.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        // Create the splash bottle with the same fluid
        return CustomFluidAPI.createSplashBottle(definitionOpt.get());
    }
}
