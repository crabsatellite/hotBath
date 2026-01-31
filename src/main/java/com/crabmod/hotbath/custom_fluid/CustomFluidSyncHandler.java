package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles events related to custom fluid synchronization between server and client.
 * Syncs custom fluid definitions to clients when they join the server or when
 * data packs are reloaded.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID)
public class CustomFluidSyncHandler {
    
    /**
     * Sync custom fluid definitions to player when they log in.
     * This ensures clients see custom fluids in creative menu even when
     * the fluids are only defined in server-side data packs.
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Delay sync slightly to ensure player is fully connected
            player.getServer().execute(() -> {
                CustomFluidNetworking.syncToClient(player);
            });
        }
    }
    
    /**
     * Sync custom fluid definitions when data packs are synced (player join or /reload).
     * This is the recommended way to sync data pack content in Forge.
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            // Sync to specific player (player joining)
            CustomFluidNetworking.syncToClient(event.getPlayer());
        } else {
            // Sync to all players (data pack reload via /reload)
            CustomFluidNetworking.syncToAllClients(event.getPlayerList().getPlayers());
        }
    }
}
