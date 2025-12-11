package com.crabmod.hotbath.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ToughAsNailsEventHandler {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        cleanup(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            cleanup(event.getEntity());
        }
    }

    private static void cleanup(Player player) {
        BathWaterBottleTANModifier.cleanup(player);
        HotBathTANPlayerModifier.cleanup(player);
    }
}
