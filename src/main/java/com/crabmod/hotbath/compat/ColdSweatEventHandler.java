package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

/**
 * Event handler for Cold Sweat integration
 */
public class ColdSweatEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onTempModifierRegister(TempModifierRegisterEvent event) {
        try {
            LOGGER.info("Registering Hot Bath temperature modifiers with Cold Sweat...");
            event.register(new ResourceLocation("hotbath:immersion"), HotBathImmersionModifier::new);
            event.register(new ResourceLocation("hotbath:bottle"), BathWaterBottleColdSweatModifier::new);
            LOGGER.info("Successfully registered Hot Bath temperature modifiers!");
        } catch (Exception e) {
            LOGGER.error("Failed to register Hot Bath modifiers with Cold Sweat: {}", e.getMessage(), e);
        }
    }

    @SubscribeEvent
    public static void onDefaultModifiers(com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent event) {
        event.addModifier(
                com.momosoftworks.coldsweat.api.util.Temperature.Trait.WORLD,
                new HotBathImmersionModifier(),
                com.momosoftworks.coldsweat.api.util.Placement.Duplicates.BY_CLASS,
                com.momosoftworks.coldsweat.api.util.Placement.AFTER_LAST
        );
        
        event.addModifier(
                com.momosoftworks.coldsweat.api.util.Temperature.Trait.WORLD,
                new BathWaterBottleColdSweatModifier(),
                com.momosoftworks.coldsweat.api.util.Placement.Duplicates.BY_CLASS,
                com.momosoftworks.coldsweat.api.util.Placement.AFTER_LAST
        );
    }
}










