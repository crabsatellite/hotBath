package com.crabmod.hotbath;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration file for HotBath mod.
 * Controls various features like dirtiness system.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class HotBathConfig {
    
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // Dirtiness system configuration
    private static final ModConfigSpec.BooleanValue ENABLE_DIRTINESS_SYSTEM = BUILDER
            .comment("Enable or disable the dirtiness system.",
                     "When enabled, players will gradually get dirty and need to bathe to clean themselves.",
                     "Default: true")
            .define("enableDirtinessSystem", true);
    
    public static final ModConfigSpec SPEC = BUILDER.build();
    
    // Cached config values for runtime access
    private static boolean enableDirtinessSystem = true;
    
    /**
     * Check if the dirtiness system is enabled.
     * @return true if the dirtiness system is enabled
     */
    public static boolean isDirtinessEnabled() {
        return enableDirtinessSystem;
    }
    
    /**
     * Called when the config is loaded or reloaded.
     */
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            enableDirtinessSystem = ENABLE_DIRTINESS_SYSTEM.get();
            HotBath.LOGGER.info("HotBath config loaded. Dirtiness system enabled: {}", enableDirtinessSystem);
        }
    }
}
