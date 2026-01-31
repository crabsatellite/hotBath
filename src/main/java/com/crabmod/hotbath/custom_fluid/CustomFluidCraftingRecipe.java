package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.registers.ItemRegister;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * Custom crafting recipe that produces a custom fluid bucket.
 * This recipe takes a hot water bucket + ingredients and produces a custom fluid bucket
 * with the appropriate data components.
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
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, HotBath.MOD_ID);
    
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CustomFluidCraftingRecipe>> SERIALIZER =
            RECIPE_SERIALIZERS.register("custom_fluid_crafting", Serializer::new);
    
    private final ResourceLocation fluidId;
    private final Item ingredient;
    private final int ingredientCount;
    
    public CustomFluidCraftingRecipe(ResourceLocation fluidId, Item ingredient, int ingredientCount) {
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
    public boolean matches(CraftingInput input, Level level) {
        boolean foundHotWaterBucket = false;
        int ingredientFound = 0;
        
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
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
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
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
    public ItemStack getResultItem(HolderLookup.Provider registries) {
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
        
        public static final MapCodec<CustomFluidCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("fluid_id").forGetter(CustomFluidCraftingRecipe::getFluidId),
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("ingredient").forGetter(CustomFluidCraftingRecipe::getIngredient),
                        Codec.INT.optionalFieldOf("ingredient_count", 4).forGetter(CustomFluidCraftingRecipe::getIngredientCount)
                ).apply(instance, CustomFluidCraftingRecipe::new)
        );
        
        public static final StreamCodec<RegistryFriendlyByteBuf, CustomFluidCraftingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC, CustomFluidCraftingRecipe::getFluidId,
                        ByteBufCodecs.registry(BuiltInRegistries.ITEM.key()), CustomFluidCraftingRecipe::getIngredient,
                        ByteBufCodecs.INT, CustomFluidCraftingRecipe::getIngredientCount,
                        CustomFluidCraftingRecipe::new
                );
        
        @Override
        public MapCodec<CustomFluidCraftingRecipe> codec() {
            return CODEC;
        }
        
        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CustomFluidCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
