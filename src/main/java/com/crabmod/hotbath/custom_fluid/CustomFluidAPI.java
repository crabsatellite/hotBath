package com.crabmod.hotbath.custom_fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Public API for the HotBath custom fluid extension system.
 * This class provides simple interfaces for other mods and data packs to interact
 * with custom bath fluids.
 * 
 * <h2>Data Pack Usage</h2>
 * <p>To create a custom bath fluid via data pack, create a JSON file at:
 * {@code data/<namespace>/hotbath/custom_fluids/<name>.json}</p>
 * 
 * <h3>JSON Structure</h3>
 * <pre>
 * {
 *   "id": "yourmod:your_bath",              // Required: Unique identifier
 *   "color": 16766720,                       // Optional: RGB color as integer (default: cyan)
 *   "temperature": 40.0,                     // Optional: Temperature in Celsius (default: 40)
 *   "viscosity": 1000,                       // Optional: Flow resistance (default: 1000)
 *   "density": 1000,                         // Optional: Fluid density (default: 1000)
 *   "luminosity": 2,                         // Optional: Light level 0-15 (default: 2)
 *   "show_particles": true,                  // Optional: Show splash particles (default: true)
 *   "show_bubbles": true,                    // Optional: Show bubble particles (default: true)
 *   "show_steam": true,                      // Optional: Show steam particles (default: true)
 *   "trigger_time_seconds": 15,              // Optional: Time before effects apply (default: 15)
 *   "effects": [                             // Optional: List of effects to apply
 *     {
 *       "effect": "minecraft:regeneration",  // Effect ID
 *       "duration": 200,                      // Duration in ticks
 *       "amplifier": 1,                       // Effect level (0 = Level I)
 *       "ambient": true,                      // Whether particles are more translucent
 *       "show_particles": false,              // Whether to show effect particles
 *       "show_icon": true                     // Whether to show in HUD
 *     }
 *   ]
 * }
 * </pre>
 * 
 * <h2>Color Calculation</h2>
 * <p>The color is an RGB integer. Calculate it as: (red * 65536) + (green * 256) + blue</p>
 * <p>Example: Gold = (255 * 65536) + (215 * 256) + 0 = 16766720</p>
 * 
 * <h2>Programmatic Usage</h2>
 * <pre>
 * // Check if a custom fluid is registered
 * boolean exists = CustomFluidAPI.isFluidRegistered(
 *     ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));
 * 
 * // Get a fluid definition
 * Optional&lt;CustomFluidDefinition&gt; fluid = CustomFluidAPI.getFluidDefinition(
 *     ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));
 * 
 * // Apply effects from a custom fluid to a player
 * CustomFluidAPI.applyFluidEffects(player, 
 *     ResourceLocation.fromNamespaceAndPath("hotbath", "golden_bath"));
 * 
 * // Create a custom fluid programmatically
 * CustomFluidDefinition custom = CustomFluidAPI.builder(
 *         ResourceLocation.fromNamespaceAndPath("mymod", "my_bath"))
 *     .color(0xFF00FF)
 *     .temperature(50.0f)
 *     .build();
 * </pre>
 */
public final class CustomFluidAPI {
    
    private CustomFluidAPI() {
        // Utility class - no instantiation
    }

    /**
     * Checks if a custom fluid is registered.
     * 
     * @param id The resource location of the fluid
     * @return true if the fluid is registered
     */
    public static boolean isFluidRegistered(ResourceLocation id) {
        return CustomFluidRegistry.isRegistered(id);
    }

    /**
     * Gets a custom fluid definition by ID.
     * 
     * @param id The resource location of the fluid
     * @return Optional containing the definition if found
     */
    public static Optional<CustomFluidDefinition> getFluidDefinition(ResourceLocation id) {
        return CustomFluidRegistry.getDefinition(id);
    }

    /**
     * Gets all registered custom fluid definitions.
     * 
     * @return Collection of all fluid definitions
     */
    public static Collection<CustomFluidDefinition> getAllFluids() {
        return CustomFluidRegistry.getAllDefinitions();
    }

    /**
     * Applies the effects of a custom fluid to a player.
     * 
     * @param player The player to apply effects to
     * @param fluidId The resource location of the fluid
     * @return true if effects were applied successfully
     */
    public static boolean applyFluidEffects(ServerPlayer player, ResourceLocation fluidId) {
        Optional<CustomFluidDefinition> definition = getFluidDefinition(fluidId);
        if (definition.isEmpty()) {
            return false;
        }
        
        List<MobEffectInstance> effects = definition.get().createEffectInstances();
        for (MobEffectInstance effect : effects) {
            player.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }
        return !effects.isEmpty();
    }

    /**
     * Gets the color of a custom fluid.
     * 
     * @param fluidId The resource location of the fluid
     * @return The color as an ARGB integer, or -1 if not found
     */
    public static int getFluidColor(ResourceLocation fluidId) {
        return CustomFluidRegistry.getRuntimeData(fluidId)
                .map(CustomFluidRegistry.RuntimeFluidData::tintColor)
                .orElse(-1);
    }

    /**
     * Gets the temperature of a custom fluid in Celsius.
     * 
     * @param fluidId The resource location of the fluid
     * @return The temperature, or Float.NaN if not found
     */
    public static float getFluidTemperature(ResourceLocation fluidId) {
        return getFluidDefinition(fluidId)
                .map(CustomFluidDefinition::temperature)
                .orElse(Float.NaN);
    }

    /**
     * Gets the trigger time for a custom fluid in seconds.
     * 
     * @param fluidId The resource location of the fluid
     * @return The trigger time in seconds, or -1 if not found
     */
    public static int getFluidTriggerTime(ResourceLocation fluidId) {
        return getFluidDefinition(fluidId)
                .map(CustomFluidDefinition::triggerTimeSeconds)
                .orElse(-1);
    }

    /**
     * Creates a builder for programmatically defining custom fluids.
     * Note: Programmatically created fluids are not automatically registered.
     * 
     * @param id The resource location for the fluid
     * @return A new builder instance
     */
    public static CustomFluidDefinition.Builder builder(ResourceLocation id) {
        return new CustomFluidDefinition.Builder(id);
    }

    /**
     * Creates an effect entry for use in fluid definitions.
     * 
     * @param effectId The resource location of the mob effect
     * @param duration Duration in ticks
     * @param amplifier Effect level (0 = Level I)
     * @return A new effect entry
     */
    public static CustomFluidDefinition.EffectEntry createEffect(
            ResourceLocation effectId,
            int duration,
            int amplifier) {
        return new CustomFluidDefinition.EffectEntry(
                effectId, duration, amplifier, false, true, true
        );
    }

    /**
     * Creates an effect entry with full customization.
     * 
     * @param effectId The resource location of the mob effect
     * @param duration Duration in ticks
     * @param amplifier Effect level (0 = Level I)
     * @param ambient Whether the effect is ambient
     * @param showParticles Whether to show effect particles
     * @param showIcon Whether to show the effect icon
     * @return A new effect entry
     */
    public static CustomFluidDefinition.EffectEntry createEffect(
            ResourceLocation effectId,
            int duration,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon) {
        return new CustomFluidDefinition.EffectEntry(
                effectId, duration, amplifier, ambient, showParticles, showIcon
        );
    }

    /**
     * Utility method to calculate RGB color from components.
     * 
     * @param red Red component (0-255)
     * @param green Green component (0-255)
     * @param blue Blue component (0-255)
     * @return The combined RGB color value
     */
    public static int rgb(int red, int green, int blue) {
        return (red << 16) | (green << 8) | blue;
    }

    /**
     * Utility method to calculate RGB color with alpha from components.
     * 
     * @param alpha Alpha component (0-255)
     * @param red Red component (0-255)
     * @param green Green component (0-255)
     * @param blue Blue component (0-255)
     * @return The combined ARGB color value
     */
    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    // ==================== Item Creation Methods ====================

    /**
     * Creates a bucket item stack containing the specified custom fluid.
     * 
     * @param fluidId The resource location of the custom fluid
     * @return An ItemStack of the custom fluid bucket, or empty if fluid not found
     */
    public static ItemStack createBucket(ResourceLocation fluidId) {
        return CustomFluidDataComponents.createStack(
                CustomFluidItems.CUSTOM_FLUID_BUCKET.get(), fluidId);
    }

    /**
     * Creates a bucket item stack containing the specified custom fluid.
     * 
     * @param definition The custom fluid definition
     * @return An ItemStack of the custom fluid bucket
     */
    public static ItemStack createBucket(CustomFluidDefinition definition) {
        return CustomFluidDataComponents.createStack(
                CustomFluidItems.CUSTOM_FLUID_BUCKET.get(), definition);
    }

    /**
     * Creates a drinkable bottle item stack containing the specified custom fluid.
     * 
     * @param fluidId The resource location of the custom fluid
     * @return An ItemStack of the custom fluid bottle, or empty if fluid not found
     */
    public static ItemStack createBottle(ResourceLocation fluidId) {
        return CustomFluidDataComponents.createStack(
                CustomFluidItems.CUSTOM_FLUID_BOTTLE.get(), fluidId);
    }

    /**
     * Creates a drinkable bottle item stack containing the specified custom fluid.
     * 
     * @param definition The custom fluid definition
     * @return An ItemStack of the custom fluid bottle
     */
    public static ItemStack createBottle(CustomFluidDefinition definition) {
        return CustomFluidDataComponents.createStack(
                CustomFluidItems.CUSTOM_FLUID_BOTTLE.get(), definition);
    }

    /**
     * Creates a splash bottle item stack containing the specified custom fluid.
     * 
     * @param fluidId The resource location of the custom fluid
     * @return An ItemStack of the splash custom fluid bottle, or empty if fluid not found
     */
    public static ItemStack createSplashBottle(ResourceLocation fluidId) {
        return CustomFluidDataComponents.createStack(
                CustomFluidItems.CUSTOM_FLUID_SPLASH_BOTTLE.get(), fluidId);
    }

    /**
     * Creates a splash bottle item stack containing the specified custom fluid.
     * 
     * @param definition The custom fluid definition
     * @return An ItemStack of the splash custom fluid bottle
     */
    public static ItemStack createSplashBottle(CustomFluidDefinition definition) {
        return CustomFluidDataComponents.createStack(
                CustomFluidItems.CUSTOM_FLUID_SPLASH_BOTTLE.get(), definition);
    }

    /**
     * Gets the custom fluid definition from an item stack (bucket, bottle, or splash bottle).
     * 
     * @param stack The item stack to check
     * @return Optional containing the fluid definition if present
     */
    public static Optional<CustomFluidDefinition> getFluidFromItem(ItemStack stack) {
        CustomFluidDefinition def = CustomFluidDataComponents.getFluidDefinition(stack);
        return Optional.ofNullable(def);
    }

    /**
     * Checks if an item stack contains a custom fluid.
     * 
     * @param stack The item stack to check
     * @return true if the stack contains custom fluid data
     */
    public static boolean hasCustomFluid(ItemStack stack) {
        return CustomFluidDataComponents.getFluidId(stack) != null;
    }

    /**
     * Gets a custom fluid definition by its color.
     * This is useful when only the color is available (e.g., from Twilight Forest Flask).
     * Note: If multiple fluids have the same color, only one will be returned.
     * 
     * @param color The color value (RGB without alpha)
     * @return Optional containing the first matching fluid definition
     */
    public static Optional<CustomFluidDefinition> getDefinitionByColor(int color) {
        // Normalize color to ensure consistent matching (remove alpha if present)
        int colorWithoutAlpha = color & 0x00FFFFFF;
        
        for (CustomFluidDefinition definition : getAllFluids()) {
            int defColor = definition.color() & 0x00FFFFFF;
            if (defColor == colorWithoutAlpha) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }
}
