package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.registers.ItemRegister;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

/**
 * Custom crafting recipe that produces a custom fluid bucket.
 * This recipe takes a hot water bucket + ingredients and produces a custom fluid bucket
 * with the appropriate NBT data.
 * 
 * <p>Example JSON recipe:
 * <pre>
 * {
 *   "type": "hotbath:custom_fluid_crafting",
 *   "fluid_id": "hotbath:blazing_bath",
 *   "ingredient": "minecraft:blaze_powder",
 *   "ingredient_count": 4
 * }
 * </pre>
 */
public class CustomFluidCraftingRecipe implements CraftingRecipe {
    
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, HotBath.MOD_ID);
    
    public static final RegistryObject<RecipeSerializer<CustomFluidCraftingRecipe>> SERIALIZER =
            RECIPE_SERIALIZERS.register("custom_fluid_crafting", Serializer::new);
    
    private final ResourceLocation id;
    private final ResourceLocation fluidId;
    private final Item ingredient;
    private final int ingredientCount;
    
    public CustomFluidCraftingRecipe(ResourceLocation id, ResourceLocation fluidId, Item ingredient, int ingredientCount) {
        this.id = id;
        this.fluidId = fluidId;
        this.ingredient = ingredient;
        this.ingredientCount = ingredientCount;
    }
    
    public ResourceLocation getFluidId() {
        return fluidId;
    }
    
    public Item getIngredient() {
        return ingredient;
    }
    
    public int getIngredientCount() {
        return ingredientCount;
    }
    
    @Override
    public boolean matches(CraftingContainer container, Level level) {
        boolean foundHotWaterBucket = false;
        int ingredientFound = 0;
        
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            
            if (stack.is(ItemRegister.HOT_WATER_BUCKET.get())) {
                if (foundHotWaterBucket) {
                    return false; // Only one hot water bucket allowed
                }
                foundHotWaterBucket = true;
            } else if (stack.is(ingredient)) {
                ingredientFound++;
            } else {
                return false; // Unknown item in recipe
            }
        }
        
        return foundHotWaterBucket && ingredientFound == ingredientCount;
    }
    
    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        Optional<CustomFluidDefinition> definition = CustomFluidRegistry.getDefinition(fluidId);
        if (definition.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return CustomFluidAPI.createBucket(definition.get());
    }
    
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredientCount + 1;
    }
    
    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        Optional<CustomFluidDefinition> definition = CustomFluidRegistry.getDefinition(fluidId);
        if (definition.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return CustomFluidAPI.createBucket(definition.get());
    }
    
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ItemRegister.HOT_WATER_BUCKET.get()));
        for (int i = 0; i < ingredientCount; i++) {
            ingredients.add(Ingredient.of(ingredient));
        }
        return ingredients;
    }
    
    @Override
    public ResourceLocation getId() {
        return id;
    }
    
    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }
    
    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }
    
    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
    
    public static class Serializer implements RecipeSerializer<CustomFluidCraftingRecipe> {
        
        @Override
        public CustomFluidCraftingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String fluidIdStr = GsonHelper.getAsString(json, "fluid_id");
            ResourceLocation fluidId = new ResourceLocation(fluidIdStr);
            
            String ingredientStr = GsonHelper.getAsString(json, "ingredient");
            ResourceLocation ingredientId = new ResourceLocation(ingredientStr);
            Item ingredient = BuiltInRegistries.ITEM.get(ingredientId);
            if (ingredient == null) {
                throw new JsonParseException("Unknown item: " + ingredientStr);
            }
            
            int ingredientCount = GsonHelper.getAsInt(json, "ingredient_count", 4);
            
            return new CustomFluidCraftingRecipe(recipeId, fluidId, ingredient, ingredientCount);
        }
        
        @Override
        public CustomFluidCraftingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            ResourceLocation fluidId = buffer.readResourceLocation();
            Item ingredient = buffer.readById(BuiltInRegistries.ITEM);
            int ingredientCount = buffer.readInt();
            return new CustomFluidCraftingRecipe(recipeId, fluidId, ingredient, ingredientCount);
        }
        
        @Override
        public void toNetwork(FriendlyByteBuf buffer, CustomFluidCraftingRecipe recipe) {
            buffer.writeResourceLocation(recipe.fluidId);
            buffer.writeId(BuiltInRegistries.ITEM, recipe.ingredient);
            buffer.writeInt(recipe.ingredientCount);
        }
    }
}
