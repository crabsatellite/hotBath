package com.crabmod.hotbath.compat;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class ToughAsNailsEventHandler {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CompatManager.safeEventCall("toughasnails", "onPlayerLogout", () -> {
            cleanup(event.getEntity());
        });
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.Clone event) {
        CompatManager.safeEventCall("toughasnails", "onPlayerRespawn", () -> {
            if (event.isWasDeath()) {
                cleanup(event.getEntity());
            }
        });
    }

    private static void cleanup(Player player) {
        BathWaterBottleTANModifier.cleanup(player);
        HotBathTANPlayerModifier.cleanup(player);
    }
}
