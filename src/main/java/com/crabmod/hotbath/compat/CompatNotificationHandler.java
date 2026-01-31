package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Event handler for compat manager notifications.
 * Notifies players about disabled compat modules when they join.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class CompatNotificationHandler {
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Notify player about any disabled compats
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
