package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Event handler for compat manager notifications.
 * Notifies players about disabled compat modules when they join.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class CompatNotificationHandler {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Delay notification slightly to ensure player is fully loaded
            // Use a simple tick counter approach
            CompatManager.notifyPlayer(player);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompatManager.clearPlayerNotifications(player.getUUID());
        }
    }
}
