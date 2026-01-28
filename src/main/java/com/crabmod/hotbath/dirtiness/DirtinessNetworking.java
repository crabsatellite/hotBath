package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

/**
 * Network handling for syncing dirtiness data from server to client.
 * Uses MOD bus for payload registration.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DirtinessNetworking {
    
    public static final ResourceLocation SYNC_DIRTINESS_ID = 
            ResourceLocation.fromNamespaceAndPath(HotBath.MOD_ID, "sync_dirtiness");
    
    public static final ResourceLocation SYNC_OTHER_PLAYER_ID = 
            ResourceLocation.fromNamespaceAndPath(HotBath.MOD_ID, "sync_other_dirtiness");
    
    /**
     * Packet payload for syncing dirtiness data
     */
    public record SyncDirtinessPayload(float dirtiness, long dirtSeed, boolean hasFlies) implements CustomPacketPayload {
        
        public static final Type<SyncDirtinessPayload> TYPE = 
                new Type<>(SYNC_DIRTINESS_ID);
        
        public static final StreamCodec<FriendlyByteBuf, SyncDirtinessPayload> STREAM_CODEC = 
                StreamCodec.composite(
                    ByteBufCodecs.FLOAT, SyncDirtinessPayload::dirtiness,
                    ByteBufCodecs.VAR_LONG, SyncDirtinessPayload::dirtSeed,
                    ByteBufCodecs.BOOL, SyncDirtinessPayload::hasFlies,
                    SyncDirtinessPayload::new
                );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Packet payload for syncing other player's dirtiness data (for flies visibility)
     */
    public record SyncOtherPlayerDirtinessPayload(UUID playerId, float dirtiness, long dirtSeed, boolean hasFlies) implements CustomPacketPayload {
        
        public static final Type<SyncOtherPlayerDirtinessPayload> TYPE = 
                new Type<>(SYNC_OTHER_PLAYER_ID);
        
        public static final StreamCodec<FriendlyByteBuf, SyncOtherPlayerDirtinessPayload> STREAM_CODEC = 
                StreamCodec.composite(
                    net.minecraft.core.UUIDUtil.STREAM_CODEC, SyncOtherPlayerDirtinessPayload::playerId,
                    ByteBufCodecs.FLOAT, SyncOtherPlayerDirtinessPayload::dirtiness,
                    ByteBufCodecs.VAR_LONG, SyncOtherPlayerDirtinessPayload::dirtSeed,
                    ByteBufCodecs.BOOL, SyncOtherPlayerDirtinessPayload::hasFlies,
                    SyncOtherPlayerDirtinessPayload::new
                );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(HotBath.MOD_ID);
        registrar.playToClient(
            SyncDirtinessPayload.TYPE,
            SyncDirtinessPayload.STREAM_CODEC,
            DirtinessNetworking::handleSyncOnClient
        );
        registrar.playToClient(
            SyncOtherPlayerDirtinessPayload.TYPE,
            SyncOtherPlayerDirtinessPayload.STREAM_CODEC,
            DirtinessNetworking::handleSyncOtherPlayerOnClient
        );
    }
    
    /**
     * Handle the sync packet on client
     */
    private static void handleSyncOnClient(SyncDirtinessPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Store in client-side cache
            if (context.player() != null) {
                DirtinessClientData.setDirtiness(
                    context.player().getUUID(), 
                    payload.dirtiness(), 
                    payload.dirtSeed(),
                    payload.hasFlies()
                );
            }
        });
    }
    
    /**
     * Handle the sync packet for other players on client
     */
    private static void handleSyncOtherPlayerOnClient(SyncOtherPlayerDirtinessPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Store other player's dirtiness in client-side cache
            DirtinessClientData.setDirtiness(
                payload.playerId(), 
                payload.dirtiness(), 
                payload.dirtSeed(),
                payload.hasFlies()
            );
        });
    }
    
    /**
     * Send dirtiness sync packet to a specific player
     */
    public static void syncToClient(ServerPlayer player) {
        DirtinessData data = player.getData(DirtinessAttachment.DIRTINESS);
        long gameTime = player.level().getGameTime();
        float dirtiness = data.getDirtiness(gameTime);
        boolean hasFlies = data.shouldSpawnFlies(gameTime);
        
        // Send to self
        PacketDistributor.sendToPlayer(
            player,
            new SyncDirtinessPayload(dirtiness, data.getDirtSeed(), hasFlies)
        );
        
        // Broadcast to nearby players so they can see flies/dirt overlay
        PacketDistributor.sendToPlayersTrackingEntity(
            player,
            new SyncOtherPlayerDirtinessPayload(player.getUUID(), dirtiness, data.getDirtSeed(), hasFlies)
        );
    }
}
