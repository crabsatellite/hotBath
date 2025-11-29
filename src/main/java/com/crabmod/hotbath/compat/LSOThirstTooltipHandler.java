package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.items.BathWaterBottleItem;
import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import sfiomn.legendarysurvivaloverhaul.client.tooltips.HydrationTooltipComponent;

import java.util.List;

/**
 * Adds LSO-style thirst tooltip (graphical water droplets) to bath water bottles when LSO is loaded.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID, value = Dist.CLIENT)
public class LSOThirstTooltipHandler {

    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.GatherComponents event) {
        if (!LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof BathWaterBottleItem) {
            // Add hydration tooltip component using LSO's system
            HydrationTooltipComponent hydrationComponent = new HydrationTooltipComponent(
                LSOApiHelper.getHydration(),
                LSOApiHelper.getSaturation()
            );
            
            List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();
            tooltipElements.add(Either.right(hydrationComponent));
        }
    }
}
