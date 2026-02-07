package com.crabmod.hotbath.compat;

import com.mojang.logging.LogUtils;
import com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import org.slf4j.Logger;

/**
 * Event handler for Cold Sweat integration
 */
public class ColdSweatEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onTempModifierRegister(TempModifierRegisterEvent event) {
        CompatManager.safeEventCall("cold_sweat", "onTempModifierRegister", () -> {
            LOGGER.info("Registering Hot Bath temperature modifiers with Cold Sweat...");
            event.register(ResourceLocation.parse("hotbath:immersion"), HotBathImmersionModifier::new);
            event.register(ResourceLocation.parse("hotbath:bottle"), BathWaterBottleColdSweatModifier::new);
            LOGGER.info("Successfully registered Hot Bath temperature modifiers!");
        });
    }

    @SubscribeEvent
    public static void onDefaultModifiers(DefaultTempModifiersEvent event) {
        CompatManager.safeEventCall("cold_sweat", "onDefaultModifiers", () -> {
            event.addModifier(
                    Temperature.Trait.WORLD,
                    new HotBathImmersionModifier(),
                    Placement.LAST.noDuplicates(Matcher.SAME_CLASS)
            );
            
            event.addModifier(
                    Temperature.Trait.WORLD,
                    new BathWaterBottleColdSweatModifier(),
                    Placement.LAST.noDuplicates(Matcher.SAME_CLASS)
            );
        });
    }
}
