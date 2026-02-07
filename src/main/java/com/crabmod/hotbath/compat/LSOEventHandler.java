package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.events.enter_fluid_events.PeonyBathEvents;
import com.crabmod.hotbath.fluid_blocks.IInsideAreaTracker;
import com.crabmod.hotbath.util.AdvancementHelper;
import com.crabmod.hotbath.util.HealthRegenHandler;
import com.crabmod.hotbath.util.HungerRegenHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/**
 * Event handler for Legendary Survival Overhaul integration.
 * Handles tick-based updates for temperature modifiers.
 */
public class LSOEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        CompatManager.safeEventCall("legendarysurvivaloverhaul", "onPlayerTick", () -> {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            
            // Only process on server side
            if (player.level().isClientSide()) {
                return;
            }

            // Tick the immersion modifier to apply/remove hot bath effects
            HotBathImmersionLSOModifier.tick(player);
        });
    }
    
    /**
     * Clean up player data when they log out to prevent memory leaks.
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CompatManager.safeEventCall("legendarysurvivaloverhaul", "onPlayerLogout", () -> {
            Player player = event.getEntity();
            UUID playerUUID = player.getUUID();
            
            HotBathImmersionLSOModifier.cleanup(player);
            LSOApiHelper.cleanupPlayerCache(player);
            cleanupAllCaches(playerUUID);
        });
    }
    
    /**
     * Clean up player data when they die and respawn.
     * This prevents stale bath timers from persisting after death.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompatManager.safeEventCall("legendarysurvivaloverhaul", "onPlayerClone", () -> {
            // Only clean up on death (not dimension change)
            if (event.isWasDeath()) {
                Player original = event.getOriginal();
                UUID playerUUID = original.getUUID();
                
                HotBathImmersionLSOModifier.cleanup(original);
                LSOApiHelper.cleanupPlayerCache(original);
                cleanupAllCaches(playerUUID);
            }
        });
    }
    
    /**
     * Centralized cleanup for all memory caches.
     */
    private static void cleanupAllCaches(UUID playerUUID) {
        HealthRegenHandler.cleanup(playerUUID);
        HungerRegenHandler.cleanup(playerUUID);
        AdvancementHelper.cleanup(playerUUID);
        IInsideAreaTracker.cleanupPlayer(playerUUID);
        PeonyBathEvents.cleanup(playerUUID);
    }
}











