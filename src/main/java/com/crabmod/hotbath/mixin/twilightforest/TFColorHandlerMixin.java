package com.crabmod.hotbath.mixin.twilightforest;

import com.crabmod.hotbath.compat.CompatManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.client.ColorHandler;
import twilightforest.init.TFItems;

/**
 * Client-side Mixin to fix Flask item color rendering for HotBath fluids.
 * 
 * The issue: Twilight Forest's ColorHandler uses PotionUtils.getColor(stack)
 * which doesn't recognize HotBath's custom fluids stored via NBT.
 * 
 * This Mixin registers an additional color handler that properly checks for CustomColor NBT.
 * 
 * For 1.20.1 Forge version
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
            event.register((stack, tintIndex) -> {
                if (tintIndex > 0) return -1;
                
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    // Check for HotBath custom color first
                    if (tag.getBoolean("IsHotBathContent") && tag.contains("CustomColor")) {
                        int color = tag.getInt("CustomColor");
                        // Return color with full opacity
                        return color | 0xFF000000;
                    }
                }
                
                // Fall back to original behavior: use PotionUtils.getColor
                return net.minecraft.world.item.alchemy.PotionUtils.getColor(stack);
            }, TFItems.BRITTLE_FLASK.get(), TFItems.GREATER_FLASK.get());
            
        } catch (Throwable e) {
            CompatManager.reportRuntimeError("twilightforest", "TFColorHandlerMixin.fixFlaskColorForCustomFluids", e);
        }
    }
}
