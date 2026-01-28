package com.crabmod.hotbath.waterlogging;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;

/**
 * Network handling for syncing waterlogging data from server to client.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class WaterloggingNetworking {
    
    public static final ResourceLocation SYNC_WATERLOGGING_ID = 
            ResourceLocation.fromNamespaceAndPath(HotBath.MOD_ID, "sync_waterlogging");
    
    public static final ResourceLocation SYNC_BULK_WATERLOGGING_ID = 
            ResourceLocation.fromNamespaceAndPath(HotBath.MOD_ID, "sync_bulk_waterlogging");
    
    /**
     * Packet payload for syncing single waterlogging data
     */
    public record SyncWaterloggingPayload(BlockPos pos, ResourceLocation fluidId) implements CustomPacketPayload {
        
        public static final Type<SyncWaterloggingPayload> TYPE = 
                new Type<>(SYNC_WATERLOGGING_ID);
        
        public static final StreamCodec<FriendlyByteBuf, SyncWaterloggingPayload> STREAM_CODEC = 
                StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncWaterloggingPayload::pos,
                    ResourceLocation.STREAM_CODEC, SyncWaterloggingPayload::fluidId,
                    SyncWaterloggingPayload::new
                );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Packet payload for syncing bulk waterlogging data (for chunk loading)
     */
    public record SyncBulkWaterloggingPayload(Map<Long, ResourceLocation> fluidMap) implements CustomPacketPayload {
        
        public static final Type<SyncBulkWaterloggingPayload> TYPE = 
                new Type<>(SYNC_BULK_WATERLOGGING_ID);
        
        public static final StreamCodec<FriendlyByteBuf, SyncBulkWaterloggingPayload> STREAM_CODEC = 
                new StreamCodec<>() {
                    @Override
                    public SyncBulkWaterloggingPayload decode(FriendlyByteBuf buf) {
                        int size = buf.readVarInt();
                        Map<Long, ResourceLocation> map = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            long posLong = buf.readLong();
                            ResourceLocation fluidId = buf.readResourceLocation();
                            map.put(posLong, fluidId);
                        }
                        return new SyncBulkWaterloggingPayload(map);
                    }
                    
                    @Override
                    public void encode(FriendlyByteBuf buf, SyncBulkWaterloggingPayload payload) {
                        buf.writeVarInt(payload.fluidMap().size());
                        for (Map.Entry<Long, ResourceLocation> entry : payload.fluidMap().entrySet()) {
                            buf.writeLong(entry.getKey());
                            buf.writeResourceLocation(entry.getValue());
                        }
                    }
                };
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(HotBath.MOD_ID);
        registrar.playToClient(
            SyncWaterloggingPayload.TYPE,
            SyncWaterloggingPayload.STREAM_CODEC,
            WaterloggingNetworking::handleSyncOnClient
        );
        registrar.playToClient(
            SyncBulkWaterloggingPayload.TYPE,
            SyncBulkWaterloggingPayload.STREAM_CODEC,
            WaterloggingNetworking::handleBulkSyncOnClient
        );
    }
    
    /**
     * Handle the sync packet on client
     */
    private static void handleSyncOnClient(SyncWaterloggingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload == null || payload.fluidId() == null || payload.pos() == null) return;
            
            // Use getOptional for safe lookup (handles mod removal)
            var optionalFluid = BuiltInRegistries.FLUID.getOptional(payload.fluidId());
            if (optionalFluid.isPresent()) {
                HotbathWaterloggingHelper.updateClientCache(payload.pos(), optionalFluid.get());
            } else {
                // Fluid no longer exists, remove from cache
                HotbathWaterloggingHelper.removeFromClientCache(payload.pos());
            }
        });
    }
    
    /**
     * Handle bulk sync packet on client (for chunk loading)
     */
    private static void handleBulkSyncOnClient(SyncBulkWaterloggingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload == null || payload.fluidMap() == null) return;
            
            for (Map.Entry<Long, ResourceLocation> entry : payload.fluidMap().entrySet()) {
                BlockPos pos = BlockPos.of(entry.getKey());
                ResourceLocation fluidId = entry.getValue();
                if (fluidId == null) continue;
                
                // Use getOptional for safe lookup
                var optionalFluid = BuiltInRegistries.FLUID.getOptional(fluidId);
                if (optionalFluid.isPresent()) {
                    HotbathWaterloggingHelper.updateClientCache(pos, optionalFluid.get());
                }
                // Don't add to cache if fluid doesn't exist
            }
        });
    }
    
    /**
     * Send waterlogging sync to all nearby players
     */
    public static void syncToAllPlayers(BlockPos pos, Fluid fluid) {
        if (pos == null || fluid == null) return;
        
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (fluidId == null) return;
        
        SyncWaterloggingPayload payload = new SyncWaterloggingPayload(pos, fluidId);
        PacketDistributor.sendToAllPlayers(payload);
    }
    
    /**
     * Send waterlogging clear to all players
     */
    public static void syncRemoveToAllPlayers(BlockPos pos) {
        if (pos == null) return;
        
        ResourceLocation emptyId = BuiltInRegistries.FLUID.getKey(net.minecraft.world.level.material.Fluids.EMPTY);
        SyncWaterloggingPayload payload = new SyncWaterloggingPayload(pos, emptyId);
        PacketDistributor.sendToAllPlayers(payload);
    }
    
    /**
     * Sync all waterlogging data to a specific player (on join).
     * For large data sets, this splits into multiple packets to avoid network issues.
     */
    public static void syncAllToPlayer(ServerPlayer player, ServerLevel level) {
        if (player == null || level == null) return;
        
        Map<Long, ResourceLocation> fluidMap = HotbathWaterloggingHelper.getAllStoredFluids(level);
        if (fluidMap.isEmpty()) return;
        
        // Split into chunks of 500 entries to avoid packet size issues
        final int CHUNK_SIZE = 500;
        if (fluidMap.size() <= CHUNK_SIZE) {
            SyncBulkWaterloggingPayload payload = new SyncBulkWaterloggingPayload(fluidMap);
            PacketDistributor.sendToPlayer(player, payload);
        } else {
            // Split large data into multiple packets
            Map<Long, ResourceLocation> chunk = new HashMap<>();
            int count = 0;
            for (Map.Entry<Long, ResourceLocation> entry : fluidMap.entrySet()) {
                chunk.put(entry.getKey(), entry.getValue());
                count++;
                
                if (count >= CHUNK_SIZE) {
                    SyncBulkWaterloggingPayload payload = new SyncBulkWaterloggingPayload(new HashMap<>(chunk));
                    PacketDistributor.sendToPlayer(player, payload);
                    chunk.clear();
                    count = 0;
                }
            }
            
            // Send remaining entries
            if (!chunk.isEmpty()) {
                SyncBulkWaterloggingPayload payload = new SyncBulkWaterloggingPayload(chunk);
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }
}
