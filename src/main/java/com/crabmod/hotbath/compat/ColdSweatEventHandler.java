package com.crabmod.hotbath.compat;

import com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Event handler for Cold Sweat integration
 */
public class ColdSweatEventHandler {

    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void onTempModifierRegister(TempModifierRegisterEvent event) {
        CompatManager.safeEventCall("cold_sweat", "onTempModifierRegister", () -> {
            event.register(new ResourceLocation("hotbath:immersion"), HotBathImmersionModifier::new);
            event.register(new ResourceLocation("hotbath:bottle"), BathWaterBottleColdSweatModifier::new);
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










