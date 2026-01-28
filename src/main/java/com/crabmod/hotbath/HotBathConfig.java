package com.crabmod.hotbath;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Configuration file for HotBath mod.
 * Controls various features like dirtiness system.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HotBathConfig {
    
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    // Dirtiness system configuration
    private static final ForgeConfigSpec.BooleanValue ENABLE_DIRTINESS_SYSTEM = BUILDER
            .comment("Enable or disable the dirtiness system.",
                     "When enabled, players will gradually get dirty and need to bathe to clean themselves.",
                     "Default: true")
            .define("enableDirtinessSystem", true);
    
    public static final ForgeConfigSpec SPEC = BUILDER.build();
    
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
