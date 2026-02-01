package com.crabmod.hotbath.mixin.twilightforest;

import com.crabmod.hotbath.compat.CompatManager;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.client.event.ColorHandler;
import twilightforest.components.item.PotionFlaskComponent;
import twilightforest.init.TFDataComponents;
import twilightforest.init.TFItems;

/**
 * Client-side Mixin to fix Flask item color rendering for HotBath fluids.
 * 
 * The issue: Twilight Forest's ColorHandler checks if potion().potion().isEmpty()
 * and returns -1 (no color) if there's no base potion. HotBath fluids use customColor
 * instead of a base potion, so they appear gray.
 * 
 * This Mixin registers an additional color handler that properly checks for customColor.
 */
@Mixin(ColorHandler.class)
public class TFColorHandlerMixin {

    /**
     * Inject after the original registerItemColors method to override the flask color handling.
     * We inject at TAIL to run after the original registration, then re-register with our fix.
     */
    @Inject(method = "registerItemColors", at = @At("TAIL"), remap = false)
    private static void hotbath$fixFlaskColorForCustomFluids(RegisterColorHandlersEvent.Item event, CallbackInfo ci) {
        try {
            // Re-register the color handler for flasks with our fixed version
            // This will override the previous registration
            event.register((stack, index) -> {
                if (index > 0) return -1;
                
                var contents = stack.getOrDefault(TFDataComponents.POTION_FLASK_CONTENTS, PotionFlaskComponent.EMPTY);
                
                // Check for customColor first (HotBath fluids)
                if (contents.potion().customColor().isPresent()) {
                    return FastColor.ARGB32.opaque(contents.potion().customColor().get());
                }
                
                // Fall back to original behavior: check for base potion
                if (contents.potion().potion().isEmpty()) {
                    return -1;
                }
                
                return FastColor.ARGB32.opaque(contents.potion().getColor());
            }, TFItems.BRITTLE_FLASK.get(), TFItems.GREATER_FLASK.get());
            
        } catch (Throwable e) {
            CompatManager.reportRuntimeError("twilightforest", "TFColorHandlerMixin.fixFlaskColorForCustomFluids", e);
        }
    }
}
