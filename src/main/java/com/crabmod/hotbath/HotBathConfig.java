package com.crabmod.hotbath;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Configuration file for HotBath mod.
 * Controls various features like dirtiness system, waterlogging and compatibility mode.
 * 
 * Feature Safety Notes:
 * - Dirtiness: Safe to toggle. Player data is preserved and simply ignored when disabled.
 * - Mod Integrations: Safe to toggle. Event handlers are just skipped when disabled.
 * - Waterlogging: Safe to toggle. When disabled, existing waterlogged blocks with hotbath
 *   fluids will display as vanilla water, but data is preserved for when re-enabled.
 * - Compatibility Mode: Disables ALL above features for maximum stability.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HotBathConfig {
    
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    // ==================== COMPATIBILITY MODE ====================
    private static final ForgeConfigSpec.BooleanValue COMPATIBILITY_MODE = BUILDER
            .comment("=== COMPATIBILITY MODE ===",
                     "Enable or disable compatibility mode.",
                     "When enabled, ALL advanced features are disabled (waterlogging, mod integrations, dirtiness).",
                     "Only basic fluid properties will be available.",
                     "Use this if you experience crashes or compatibility issues with other mods.",
                     "NOTE: This overrides all other settings below.",
                     "SAVE SAFETY: Your world data is preserved, just not used until re-enabled.",
                     "Default: false")
            .define("compatibilityMode", false);
    
    // ==================== INDIVIDUAL FEATURE TOGGLES ====================
    
    // Waterlogging system - affects world data, but safe to toggle
    private static final ForgeConfigSpec.BooleanValue ENABLE_WATERLOGGING = BUILDER
            .comment("=== WATERLOGGING SYSTEM ===",
                     "Enable or disable the waterlogging system.",
                     "When enabled, hotbath fluids can be placed in waterloggable blocks (stairs, slabs, etc.).",
                     "SAVE SAFETY: Disabling this will NOT corrupt your world!",
                     "  - Existing waterlogged blocks will simply display as vanilla water.",
                     "  - The hotbath fluid data is preserved in the world save.",
                     "  - Re-enabling will restore the correct fluid display.",
                     "NOTE: Requires game restart to apply mixin changes.",
                     "Default: true")
            .define("enableWaterlogging", true);
    
    // Dirtiness system - only affects player data
    private static final ForgeConfigSpec.BooleanValue ENABLE_DIRTINESS_SYSTEM = BUILDER
            .comment("=== DIRTINESS SYSTEM ===",
                     "Enable or disable the dirtiness system.",
                     "When enabled, players will gradually get dirty and need to bathe to clean themselves.",
                     "SAVE SAFETY: Disabling this will NOT affect your world!",
                     "  - Player dirtiness data is preserved but simply ignored.",
                     "  - Re-enabling will restore previous dirtiness values.",
                     "Default: true")
            .define("enableDirtinessSystem", true);
    
    // Mod integrations - no persistent data
    private static final ForgeConfigSpec.BooleanValue ENABLE_MOD_INTEGRATIONS = BUILDER
            .comment("=== MOD INTEGRATIONS ===",
                     "Enable or disable mod integrations (Cold Sweat, LSO, Alex's Mobs, etc.).",
                     "When disabled, hotbath will not interact with other mods' systems.",
                     "SAVE SAFETY: This has NO impact on world data - can be toggled freely.",
                     "Default: true")
            .define("enableModIntegrations", true);
    
    public static final ForgeConfigSpec SPEC = BUILDER.build();
    
    // Cached config values for runtime access
    private static boolean compatibilityMode = false;
    private static boolean enableWaterlogging = true;
    private static boolean enableDirtinessSystem = true;
    private static boolean enableModIntegrations = true;
    
    /**
     * Check if compatibility mode is enabled.
     * When enabled, all mixins, mod integrations, and complex features are disabled.
     * @return true if compatibility mode is enabled
     */
    public static boolean isCompatibilityModeEnabled() {
        return compatibilityMode;
    }
    
    /**
     * Check if waterlogging system is enabled.
     * Returns false if compatibility mode is enabled.
     * 
     * NOTE: This only affects runtime behavior. Mixin loading is controlled
     * separately by HotBathMixinPlugin reading the config file directly.
     * 
     * @return true if waterlogging is enabled
     */
    public static boolean isWaterloggingEnabled() {
        if (compatibilityMode) {
            return false;
        }
        return enableWaterlogging;
    }
    
    /**
     * Check if the dirtiness system is enabled.
     * Returns false if compatibility mode is enabled.
     * @return true if the dirtiness system is enabled
     */
    public static boolean isDirtinessEnabled() {
        if (compatibilityMode) {
            return false;
        }
        return enableDirtinessSystem;
    }
    
    /**
     * Check if mod integrations are enabled.
     * Returns false if compatibility mode is enabled.
     * @return true if mod integrations are enabled
     */
    public static boolean isModIntegrationsEnabled() {
        if (compatibilityMode) {
            return false;
        }
        return enableModIntegrations;
    }
    
    /**
     * Called when the config is loaded or reloaded.
     */
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            compatibilityMode = COMPATIBILITY_MODE.get();
            enableWaterlogging = ENABLE_WATERLOGGING.get();
            enableDirtinessSystem = ENABLE_DIRTINESS_SYSTEM.get();
            enableModIntegrations = ENABLE_MOD_INTEGRATIONS.get();
            
            HotBath.LOGGER.info("HotBath config loaded:");
            HotBath.LOGGER.info("  - Compatibility mode: {}", compatibilityMode);
            HotBath.LOGGER.info("  - Waterlogging: {}", isWaterloggingEnabled());
            HotBath.LOGGER.info("  - Dirtiness system: {}", isDirtinessEnabled());
            HotBath.LOGGER.info("  - Mod integrations: {}", isModIntegrationsEnabled());
            
            if (compatibilityMode) {
                HotBath.LOGGER.info("Compatibility mode is ON - all advanced features are disabled.");
            }
        }
    }
}
