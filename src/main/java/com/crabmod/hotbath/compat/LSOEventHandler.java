package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Event handler for Legendary Survival Overhaul integration.
 * Handles tick-based updates for temperature modifiers.
 */
public class LSOEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        
        // Only process on server side
        if (player.level().isClientSide()) {
            return;
        }

        // Tick the immersion modifier to apply/remove hot bath effects
        HotBathImmersionLSOModifier.tick(player);
    }
}










