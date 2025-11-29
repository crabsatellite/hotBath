package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.items.BathWaterBottleItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Handles thirst restoration when drinking bath water bottles with LSO loaded.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class LSOThirstHandler {

    @SubscribeEvent
    public static void onItemFinishUse(LivingEntityUseItemEvent.Finish event) {
        if (!LegendarySurvivalOverhaulIntegration.isLSOLoaded()) {
            return;
        }

        // Only process on server side
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // Only process for players
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack stack = event.getItem();
        if (stack.getItem() instanceof BathWaterBottleItem) {
            LSOApiHelper.addThirst(player);
        }
    }
}
