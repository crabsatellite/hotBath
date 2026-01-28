package com.crabmod.hotbath.waterlogging;

import com.crabmod.hotbath.HotBath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handlers for syncing waterlogging data on player join and dimension change.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID)
public class WaterloggingEvents {
    
    // Cleanup interval in ticks (every 5 minutes = 6000 ticks)
    private static final int CLEANUP_INTERVAL = 6000;
    private static int tickCounter = 0;
    
    /**
     * Sync all waterlogging data when a player logs in
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WaterloggingNetworking.syncAllToPlayer(player, level);
        }
    }
    
    /**
     * Sync waterlogging data when player changes dimension
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            // Clear client cache first (old dimension data)
            HotbathWaterloggingHelper.clearClientCache();
            // Then sync new dimension data
            WaterloggingNetworking.syncAllToPlayer(player, level);
        }
    }
    
    /**
     * Clear client cache when world unloads
     */
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            HotbathWaterloggingHelper.clearClientCache();
        }
    }
    
    /**
     * Periodic cleanup of stale waterlogging data on server
     */
    @SubscribeEvent
    public static void onServerLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            tickCounter++;
            if (tickCounter >= CLEANUP_INTERVAL) {
                tickCounter = 0;
                // Run cleanup in a low-priority way
                HotbathWaterloggingHelper.validateAndCleanup(serverLevel);
            }
        }
    }
}
