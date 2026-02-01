package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.fluid_details.BaseFluidType;
import com.crabmod.hotbath.registers.ParticleRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for dynamically created custom fluids.
 * This class manages the runtime registration and lookup of custom fluids
 * created from data pack definitions.
 * 
 * <p>Note: Due to Minecraft's registry freeze, custom fluids defined in data packs
 * cannot be registered as true fluids at runtime. Instead, this registry provides
 * a lookup system for custom fluid properties that can be applied to a generic
 * custom fluid block.</p>
 * 
 * <p>For full fluid registration (with buckets, etc.), fluids must be defined
 * in code during mod initialization.</p>
 */
public class CustomFluidRegistry {
    
    private static final Map<ResourceLocation, CustomFluidDefinition> REGISTERED_FLUIDS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RuntimeFluidData> RUNTIME_FLUID_DATA = new ConcurrentHashMap<>();

    /**
     * Called when custom fluids are reloaded from data packs.
     * Updates the internal registry with new definitions.
     * 
     * @param definitions The loaded fluid definitions
     */
    public static void onFluidsReloaded(Collection<CustomFluidDefinition> definitions) {
        REGISTERED_FLUIDS.clear();
        RUNTIME_FLUID_DATA.clear();
        
        for (CustomFluidDefinition definition : definitions) {
            REGISTERED_FLUIDS.put(definition.id(), definition);
            RUNTIME_FLUID_DATA.put(definition.id(), createRuntimeData(definition));
            HotBath.LOGGER.debug("Registered custom fluid: {}", definition.id());
        }
        
        HotBath.LOGGER.info("Custom fluid registry updated with {} fluids", REGISTERED_FLUIDS.size());
    }

    /**
     * Creates runtime fluid data from a definition.
     * This includes pre-computed values used during rendering and gameplay.
     */
    private static RuntimeFluidData createRuntimeData(CustomFluidDefinition definition) {
        // Extract RGB components from the color
        int color = definition.color();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        
        // Apply opacity to alpha channel
        float opacity = definition.opacity();
        int alpha = (int)(opacity * 255) & 0xFF;
        int colorWithAlpha = (alpha << 24) | (color & 0x00FFFFFF);
        
        return new RuntimeFluidData(
                definition,
                new Vector3f(r * 0.5f, g * 0.5f, b * 0.5f), // Fog color (darker version)
                colorWithAlpha
        );
    }

    /**
     * Gets a custom fluid definition by ID.
     */
    public static Optional<CustomFluidDefinition> getDefinition(ResourceLocation id) {
        return Optional.ofNullable(REGISTERED_FLUIDS.get(id));
    }

    /**
     * Gets runtime fluid data by ID.
     */
    public static Optional<RuntimeFluidData> getRuntimeData(ResourceLocation id) {
        return Optional.ofNullable(RUNTIME_FLUID_DATA.get(id));
    }

    /**
     * Gets all registered fluid definitions.
     */
    public static Collection<CustomFluidDefinition> getAllDefinitions() {
        return REGISTERED_FLUIDS.values();
    }

    /**
     * Gets all registered runtime fluid data.
     */
    public static Collection<RuntimeFluidData> getAllRuntimeData() {
        return RUNTIME_FLUID_DATA.values();
    }

    /**
     * Checks if a custom fluid is registered.
     */
    public static boolean isRegistered(ResourceLocation id) {
        return REGISTERED_FLUIDS.containsKey(id);
    }
    
    /**
     * Clears client-side registry. Called when receiving sync from server.
     */
    public static void clearClientSide() {
        REGISTERED_FLUIDS.clear();
        RUNTIME_FLUID_DATA.clear();
        HotBath.LOGGER.debug("Cleared client-side custom fluid registry");
    }
    
    /**
     * Registers a fluid definition on the client side (received from server sync).
     */
    public static void registerClientSide(CustomFluidDefinition definition) {
        REGISTERED_FLUIDS.put(definition.id(), definition);
        RUNTIME_FLUID_DATA.put(definition.id(), createRuntimeData(definition));
        HotBath.LOGGER.debug("Client-side registered custom fluid: {}", definition.id());
    }

    /**
     * Creates FluidType.Properties based on a custom fluid definition.
     * This is used when creating fluid types for code-registered fluids.
     */
    public static FluidType.Properties createFluidTypeProperties(CustomFluidDefinition definition) {
        return FluidType.Properties.create()
                .lightLevel(definition.luminosity())
                .density(definition.density())
                .viscosity(definition.viscosity())
                .canExtinguish(true)
                .supportsBoating(true)
                .fallDistanceModifier(0.0F)
                .canDrown(true)
                .canSwim(true)
                .canConvertToSource(true)
                .pathType(PathType.WATER)
                .adjacentPathType(PathType.WATER_BORDER)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                .canHydrate(true);
    }

    /**
     * Creates a BaseFluidType for a custom fluid definition.
     * This is used for fluid rendering with the grayscale texture and tint color.
     */
    public static BaseFluidType createFluidType(CustomFluidDefinition definition) {
        RuntimeFluidData runtimeData = createRuntimeData(definition);
        
        return new BaseFluidType(
                definition.textureStill(),
                definition.textureFlowing(),
                ResourceLocation.parse("block/water_overlay"),
                runtimeData.tintColor(),
                runtimeData.fogColor(),
                createFluidTypeProperties(definition),
                ParticleRegister.DRIPPING_HOT_WATER, // Default drip particle
                definition.showBubbles() ? ParticleRegister.HOT_WATER_BUBBLE : null,
                definition.showParticles() ? ParticleRegister.HOT_WATER_SPLASH : null,
                () -> null // Fluid supplier - will be set properly when registered
        );
    }

    /**
     * Runtime data for a custom fluid, including pre-computed rendering values.
     */
    public record RuntimeFluidData(
            CustomFluidDefinition definition,
            Vector3f fogColor,
            int tintColor
    ) {
        /**
         * Gets the color as ARGB integer.
         */
        public int getColorARGB() {
            return tintColor;
        }

        /**
         * Gets RGB components as an array [r, g, b] in range 0-1.
         */
        public float[] getColorRGB() {
            int color = definition.color();
            return new float[]{
                    ((color >> 16) & 0xFF) / 255.0f,
                    ((color >> 8) & 0xFF) / 255.0f,
                    (color & 0xFF) / 255.0f
            };
        }
    }
}
