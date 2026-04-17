package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side event handler for Legendary Survival Overhaul integration.
 * Adds hydration icons to custom fluid bottle tooltips.
 */
public class LSOClientHandler {

    // LSO hydration icons texture
    private static final ResourceLocation LSO_ICONS = new ResourceLocation("legendarysurvivaloverhaul", "textures/gui/overlay.png");
    private static final int THIRST_TEXTURE_WIDTH = 9;
    private static final int THIRST_TEXTURE_HEIGHT = 9;

    /**
     * Event handler for game events (FORGE bus)
     *
     * NOTE: Custom fluid bottles are registered in LSO's consumable data system
     * (data/hotbath/legendarysurvivaloverhaul/thirst/consumables/custom_fluid_bottle.json),
     * so LSO handles their thirst tooltip natively. This handler only adds tooltips for
     * custom fluid bottles that LSO doesn't already cover (i.e., none currently).
     * Kept for potential future use with items outside LSO's data system.
     */
    @Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onRenderTooltip(RenderTooltipEvent.GatherComponents event) {
            // LSO handles custom_fluid_bottle via its consumable data system,
            // so we no longer need to add a duplicate tooltip here.
        }
    }

    /**
     * Event handler for mod events (MOD bus) - registers tooltip component factory
     */
    @Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerTooltipFactory(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(HydrationTooltipComponent.class, HydrationClientTooltipComponent::new);
        }
    }

    /**
     * TooltipComponent data record that holds the hydration amount.
     */
    public record HydrationTooltipComponent(int hydration) implements TooltipComponent {
    }

    /**
     * Client tooltip component that renders LSO hydration droplet icons.
     * Based on LSO's own HydrationClientTooltipComponent implementation.
     */
    private static class HydrationClientTooltipComponent implements ClientTooltipComponent {
        private final int hydration;
        private final int iconCount;

        public HydrationClientTooltipComponent(HydrationTooltipComponent data) {
            this.hydration = data.hydration();
            this.iconCount = Math.min((int) Math.ceil(Math.abs(hydration) / 2.0f), 10);
        }

        @Override
        public int getHeight() {
            return 14;
        }

        @Override
        public int getWidth(Font font) {
            return iconCount * THIRST_TEXTURE_WIDTH;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics gui) {
            // Calculate the rightmost position (LSO draws right-to-left)
            int left = x + (iconCount - 1) * THIRST_TEXTURE_WIDTH;
            int top = y + 2;
            
            // UV offset for positive hydration (full droplet at x=9)
            int xOffsetTexture = THIRST_TEXTURE_WIDTH;
            
            // Draw the hydration bubbles (right to left like LSO)
            for (int i = 0; i < iconCount; i++) {
                int halfIcon = i * 2 + 1;
                int drawX = left - i * THIRST_TEXTURE_WIDTH;
                
                if (halfIcon < Math.abs(hydration)) {
                    // Full droplet
                    gui.blit(LSO_ICONS, drawX, top, xOffsetTexture, 0, THIRST_TEXTURE_WIDTH, THIRST_TEXTURE_HEIGHT, 256, 256);
                } else if (halfIcon == Math.abs(hydration)) {
                    // Half droplet (xOffset + 9)
                    gui.blit(LSO_ICONS, drawX, top, xOffsetTexture + THIRST_TEXTURE_WIDTH, 0, THIRST_TEXTURE_WIDTH, THIRST_TEXTURE_HEIGHT, 256, 256);
                }
            }
        }
    }
}
