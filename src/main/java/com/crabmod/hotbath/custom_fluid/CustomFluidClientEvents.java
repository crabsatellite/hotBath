package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.crabmod.hotbath.registers.EntityRegister;

/**
 * Client-side event handlers for custom fluid rendering.
 * Handles item color tinting and entity rendering registration.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CustomFluidClientEvents {

    /**
     * Registers item color handlers for custom fluid items.
     * This allows the items to display with the color of the fluid they contain.
     * 
     * Note: Smoke overlay visibility is now handled via model overrides (hot_steam predicate)
     * instead of color alpha channel, as Forge 1.20.1's translucent render type doesn't
     * properly support alpha from item tint colors.
     * 
     * Layer structure (matching vanilla potion patterns):
     * - Bucket: layer0=bucket, layer1=fluid overlay (tinted), (layer2=smoke via model override)
     * - Bottle: layer0=potion overlay (tinted), layer1=potion bottle, (layer2=smoke via model override)
     * - Splash: layer0=splash overlay (tinted), layer1=splash bottle, (layer2=smoke via model override)
     */
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // Custom fluid bucket - tint layer 1 (the fluid inside)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 1) {
                // Fluid layer - apply fluid color
                int color = CustomFluidNBTHelper.getFluidColor(stack);
                if (color != -1) {
                    return color;
                }
                return 0xFF4FC3F7; // Default light blue
            }
            return 0xFFFFFFFF;
        }, CustomFluidItems.CUSTOM_FLUID_BUCKET.get());

        // Custom fluid bottle - tint layer 0 (the fluid overlay like vanilla potion)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                // Fluid overlay layer - apply fluid color (like vanilla potion)
                int color = CustomFluidNBTHelper.getFluidColor(stack);
                if (color != -1) {
                    return color;
                }
                return 0xFF4FC3F7; // Default light blue
            }
            return 0xFFFFFFFF;
        }, CustomFluidItems.CUSTOM_FLUID_BOTTLE.get());

        // Splash custom fluid bottle - tint layer 0 (the fluid overlay like vanilla splash potion)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                // Fluid overlay layer - apply fluid color (like vanilla splash potion)
                int color = CustomFluidNBTHelper.getFluidColor(stack);
                if (color != -1) {
                    return color;
                }
                return 0xFF4FC3F7; // Default light blue
            }
            return 0xFFFFFFFF;
        }, CustomFluidItems.CUSTOM_FLUID_SPLASH_BOTTLE.get());
    }

    /**
     * Registers entity renderers for custom fluid projectiles.
     */
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegister.THROWN_CUSTOM_FLUID_BOTTLE.get(), ThrownItemRenderer::new);
    }
}
