package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Manages loading and access to custom fluid definitions from data packs.
 * Loads JSON files from data/&lt;namespace&gt;/hotbath/custom_fluids/
 * 
 * <p>This class implements the SimpleJsonResourceReloadListener to automatically
 * reload fluid definitions when data packs are reloaded.</p>
 * 
 * <p>Usage example:
 * <pre>
 * // Get a specific fluid definition
 * Optional&lt;CustomFluidDefinition&gt; fluid = CustomFluidManager.getInstance()
 *     .getFluidDefinition(new ResourceLocation("mymod", "golden_bath"));
 * 
 * // Get all loaded fluids
 * Collection&lt;CustomFluidDefinition&gt; allFluids = CustomFluidManager.getInstance().getAllFluidDefinitions();
 * </pre>
 */
public class CustomFluidManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "hotbath/custom_fluids";
    
    private static CustomFluidManager INSTANCE;
    
    private Map<ResourceLocation, CustomFluidDefinition> fluidDefinitions = ImmutableMap.of();

    public CustomFluidManager() {
        super(GSON, DIRECTORY);
    }

    /**
     * Gets the singleton instance of the CustomFluidManager.
     * Note: The instance is set when the reload listener is registered.
     */
    public static CustomFluidManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomFluidManager();
        }
        return INSTANCE;
    }

    /**
     * Sets the singleton instance. Called during registration.
     */
    public static void setInstance(CustomFluidManager manager) {
        INSTANCE = manager;
    }

    @Override
    protected void apply(
            @NotNull Map<ResourceLocation, JsonElement> resources,
            @NotNull ResourceManager resourceManager,
            @NotNull ProfilerFiller profiler) {
        
        // Clear old translations before loading new ones
        CustomFluidTranslationManager.clearAll();
        
        ImmutableMap.Builder<ResourceLocation, CustomFluidDefinition> builder = ImmutableMap.builder();
        
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement json = entry.getValue();
            
            try {
                CustomFluidDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(error -> 
                                HotBath.LOGGER.warn("Failed to parse custom fluid {}: {}", location, error))
                        .ifPresent(definition -> {
                            builder.put(definition.id(), definition);
                            // Register translations from the fluid definition
                            definition.registerTranslations();
                            HotBath.LOGGER.debug("Loaded custom fluid: {}", definition.id());
                        });
            } catch (Exception e) {
                HotBath.LOGGER.error("Exception while parsing custom fluid {}: {}", location, e.getMessage());
            }
        }
        
        this.fluidDefinitions = builder.build();
        HotBath.LOGGER.info("Loaded {} custom fluid definitions with {} translations", 
                fluidDefinitions.size(), CustomFluidTranslationManager.getTranslationCount());
        
        // Notify the registry to rebuild dynamic fluids
        CustomFluidRegistry.onFluidsReloaded(fluidDefinitions.values());
    }

    /**
     * Gets a fluid definition by its ID.
     * 
     * @param id The resource location ID of the fluid
     * @return Optional containing the fluid definition if found
     */
    public Optional<CustomFluidDefinition> getFluidDefinition(ResourceLocation id) {
        return Optional.ofNullable(fluidDefinitions.get(id));
    }

    /**
     * Gets all loaded fluid definitions.
     * 
     * @return Collection of all fluid definitions
     */
    public Collection<CustomFluidDefinition> getAllFluidDefinitions() {
        return fluidDefinitions.values();
    }

    /**
     * Checks if a fluid definition exists.
     * 
     * @param id The resource location ID to check
     * @return true if the fluid exists
     */
    public boolean hasFluidDefinition(ResourceLocation id) {
        return fluidDefinitions.containsKey(id);
    }

    /**
     * Gets the total number of loaded fluid definitions.
     * 
     * @return The count of loaded fluids
     */
    public int getFluidCount() {
        return fluidDefinitions.size();
    }
}
