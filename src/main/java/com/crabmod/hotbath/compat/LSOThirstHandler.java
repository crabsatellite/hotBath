package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.custom_fluid.CustomFluidBottleItem;
import com.crabmod.hotbath.custom_fluid.CustomFluidDataComponents;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.items.BathWaterBottleItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Handles thirst restoration when drinking bath water bottles with LSO loaded.
 */
public class LSOThirstHandler {

    @SubscribeEvent
    public static void onItemFinishUse(LivingEntityUseItemEvent.Finish event) {
        CompatManager.safeEventCall("legendarysurvivaloverhaul", "onItemFinishUse", () -> {
            // Only process on server side
            if (event.getEntity().level().isClientSide()) {
                return;
            }

            // Only process for players
            if (!(event.getEntity() instanceof Player player)) {
                return;
            }

            ItemStack stack = event.getItem();
            // Handle built-in bath water bottles (use default thirst)
            if (stack.getItem() instanceof BathWaterBottleItem) {
                LSOApiHelper.addThirst(player);
            }
            // Handle custom fluid bottles (use definition's thirst value)
            else if (stack.getItem() instanceof CustomFluidBottleItem) {
                CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
                if (definition != null && definition.thirst() > 0) {
                    LSOApiHelper.addThirst(player, definition.thirst());
                }
            }
        });
    }
}
