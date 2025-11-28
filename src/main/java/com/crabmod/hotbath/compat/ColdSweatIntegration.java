package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * Integration with Cold Sweat mod
 * Provides temperature effects for hot bath blocks
 * Uses Cold Sweat's event system to register block temperatures
 */
@EventBusSubscriber(modid = "hotbath")
public class ColdSweatIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COLD_SWEAT_MOD_ID = "cold_sweat";

    /**
     * Check if Cold Sweat mod is loaded
     */
    public static boolean isColdSweatLoaded() {
        return ModList.get().isLoaded(COLD_SWEAT_MOD_ID);
    }

    /**
     * Event handler for Cold Sweat's TempModifierRegisterEvent
     * This is called when Cold Sweat is ready to accept temperature modifier registrations
     */
    @SubscribeEvent
    public static void onTempModifierRegister(TempModifierRegisterEvent event) {
        if (!isColdSweatLoaded()) {
            return;
        }

        try {
            LOGGER.info("Registering Hot Bath immersion modifier with Cold Sweat...");

            // Register the immersion modifier
            // This handles the fixed temperature when inside the bath
            event.register(ResourceLocation.parse("hotbath:immersion"), HotBathImmersionModifier::new);

            LOGGER.info("Successfully registered Hot Bath immersion modifier!");
        } catch (Exception e) {
            LOGGER.error("Failed to register Hot Bath modifier with Cold Sweat: {}", e.getMessage(), e);
        }
    }

    /**
     * Add the Hot Bath immersion modifier to players
     */
    @SubscribeEvent
    public static void onDefaultModifiers(com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent event) {
        if (!isColdSweatLoaded()) {
            return;
        }
        
        // Add the modifier to the WORLD trait
        // Use BY_CLASS to avoid duplicates
        // Place it AFTER_LAST to ensure it overrides other modifiers if necessary (though our logic handles override internally)
        event.addModifier(
            com.momosoftworks.coldsweat.api.util.Temperature.Trait.WORLD,
            new HotBathImmersionModifier(),
            com.momosoftworks.coldsweat.api.util.Placement.Duplicates.BY_CLASS,
            com.momosoftworks.coldsweat.api.util.Placement.AFTER_LAST
        );
    }
}
