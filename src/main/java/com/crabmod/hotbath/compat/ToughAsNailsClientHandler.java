package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.custom_fluid.CustomFluidBottleItem;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.CustomFluidNBTHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side event handler for ToughAsNails integration.
 * Adds thirst restoration icons to custom fluid bottle tooltips.
 */
public class ToughAsNailsClientHandler {

    // ToughAsNails thirst icons texture
    private static final ResourceLocation TAN_ICONS = new ResourceLocation("toughasnails", "textures/gui/icons.png");

    /**
     * Event handler for game events (FORGE bus)
     */
    @Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {
        /**
         * Adds thirst icon tooltip component for custom fluid bottles when ToughAsNails is loaded.
         */
        @SubscribeEvent
        public static void onRenderTooltip(RenderTooltipEvent.GatherComponents event) {
            // Only add if TAN is loaded
            if (!ToughAsNailsIntegration.isToughAsNailsLoaded()) {
                return;
            }

            ItemStack stack = event.getItemStack();

            // Skip items in any TAN tag (thirst, hydration, drinks, etc.)
            // TAN's own tooltip handler already covers these items
            if (stack.getTags().anyMatch(tag -> tag.location().getNamespace().equals("toughasnails"))) {
                return;
            }

            // Check if it's a custom fluid bottle
            if (stack.getItem() instanceof CustomFluidBottleItem) {
                CustomFluidDefinition definition = CustomFluidNBTHelper.getFluidDefinition(stack);
                if (definition != null && definition.thirst() > 0) {
                    // Add the thirst tooltip component
                    event.getTooltipElements().add(
                        com.mojang.datafixers.util.Either.right(
                            new ThirstTooltipComponent(definition.thirst())
                        )
                    );
                }
            }
        }
    }

    /**
     * Event handler for mod events (MOD bus) - registers tooltip component factory
     */
    @Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerTooltipFactory(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(ThirstTooltipComponent.class, ThirstClientTooltipComponent::new);
        }
    }

    /**
     * TooltipComponent data record that holds the thirst amount.
     */
    public record ThirstTooltipComponent(int amount) implements TooltipComponent {
    }

    /**
     * Client tooltip component that renders ToughAsNails thirst droplet icons.
     * Based on ToughAsNails' own ThirstClientTooltipComponent implementation.
     */
    private static class ThirstClientTooltipComponent implements ClientTooltipComponent {
        private final int amount;

        public ThirstClientTooltipComponent(ThirstTooltipComponent data) {
            this.amount = data.amount();
        }

        @Override
        public int getHeight() {
            return 9;
        }

        @Override
        public int getWidth(Font font) {
            return (this.amount / 2) * 9;
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics gui) {
            gui.pose().pushPose();

            for (int i = 0; i < Mth.ceil(this.amount / 2.0F); i++) {
                int dropletHalf = i * 2 + 1;

                int startX = x + i * 8 - 1;
                int startY = y;

                // Draw background droplet outline
                gui.blit(TAN_ICONS, startX, startY, 9, 32, 9, 9, 256, 256);

                if (this.amount > dropletHalf) {
                    // Full droplet
                    gui.blit(TAN_ICONS, startX, startY, 4 * 9, 32, 9, 9, 256, 256);
                } else if (this.amount == dropletHalf) {
                    // Half droplet
                    gui.blit(TAN_ICONS, startX, startY, 9, 32 + 9, 9, 9, 256, 256);
                }
            }

            gui.pose().popPose();
        }
    }
}
