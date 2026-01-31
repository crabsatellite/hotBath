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
    
    public static final ResourceLocation SYNC_CUSTOM_FLUID_ID_ID = 
            ResourceLocation.fromNamespaceAndPath(HotBath.MOD_ID, "sync_custom_fluid_id");
    
    public static final ResourceLocation SYNC_BULK_CUSTOM_FLUID_ID_ID = 
            ResourceLocation.fromNamespaceAndPath(HotBath.MOD_ID, "sync_bulk_custom_fluid_id");
    
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
    
    /**
     * Packet payload for syncing custom fluid ID (for dynamic custom fluids in waterlogged blocks)
     */
    public record SyncCustomFluidIdPayload(BlockPos pos, ResourceLocation customFluidId) implements CustomPacketPayload {
        
        public static final Type<SyncCustomFluidIdPayload> TYPE = 
                new Type<>(SYNC_CUSTOM_FLUID_ID_ID);
        
        public static final StreamCodec<FriendlyByteBuf, SyncCustomFluidIdPayload> STREAM_CODEC = 
                StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SyncCustomFluidIdPayload::pos,
                    ResourceLocation.STREAM_CODEC, SyncCustomFluidIdPayload::customFluidId,
                    SyncCustomFluidIdPayload::new
                );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Packet payload for bulk syncing custom fluid IDs
     */
    public record SyncBulkCustomFluidIdPayload(Map<Long, ResourceLocation> customFluidIdMap) implements CustomPacketPayload {
        
        public static final Type<SyncBulkCustomFluidIdPayload> TYPE = 
                new Type<>(SYNC_BULK_CUSTOM_FLUID_ID_ID);
        
        public static final StreamCodec<FriendlyByteBuf, SyncBulkCustomFluidIdPayload> STREAM_CODEC = 
                new StreamCodec<>() {
                    @Override
                    public SyncBulkCustomFluidIdPayload decode(FriendlyByteBuf buf) {
                        int size = buf.readVarInt();
                        Map<Long, ResourceLocation> map = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            long posLong = buf.readLong();
                            ResourceLocation customId = buf.readResourceLocation();
                            map.put(posLong, customId);
                        }
                        return new SyncBulkCustomFluidIdPayload(map);
                    }
                    
                    @Override
                    public void encode(FriendlyByteBuf buf, SyncBulkCustomFluidIdPayload payload) {
                        buf.writeVarInt(payload.customFluidIdMap().size());
                        for (Map.Entry<Long, ResourceLocation> entry : payload.customFluidIdMap().entrySet()) {
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
        registrar.playToClient(
            SyncCustomFluidIdPayload.TYPE,
            SyncCustomFluidIdPayload.STREAM_CODEC,
            WaterloggingNetworking::handleCustomFluidIdSyncOnClient
        );
        registrar.playToClient(
            SyncBulkCustomFluidIdPayload.TYPE,
            SyncBulkCustomFluidIdPayload.STREAM_CODEC,
            WaterloggingNetworking::handleBulkCustomFluidIdSyncOnClient
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
     * Handle custom fluid ID sync packet on client
     */
    private static void handleCustomFluidIdSyncOnClient(SyncCustomFluidIdPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload == null || payload.pos() == null) return;
            
            HotbathWaterloggingHelper.updateClientCustomFluidIdCache(payload.pos(), payload.customFluidId());
        });
    }
    
    /**
     * Handle bulk custom fluid ID sync packet on client
     */
    private static void handleBulkCustomFluidIdSyncOnClient(SyncBulkCustomFluidIdPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload == null || payload.customFluidIdMap() == null) return;
            
            for (Map.Entry<Long, ResourceLocation> entry : payload.customFluidIdMap().entrySet()) {
                BlockPos pos = BlockPos.of(entry.getKey());
                ResourceLocation customId = entry.getValue();
                HotbathWaterloggingHelper.updateClientCustomFluidIdCache(pos, customId);
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
     * Send custom fluid ID sync to all players (for dynamic custom fluids)
     */
    public static void syncCustomFluidIdToAllPlayers(BlockPos pos, ResourceLocation customFluidId) {
        if (pos == null || customFluidId == null) return;
        
        SyncCustomFluidIdPayload payload = new SyncCustomFluidIdPayload(pos, customFluidId);
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
        
        // Also clear custom fluid ID
        SyncCustomFluidIdPayload customPayload = new SyncCustomFluidIdPayload(pos, null);
        // Note: null is handled by client to remove from cache
    }
    
    /**
     * Sync all waterlogging data to a specific player (on join).
     * For large data sets, this splits into multiple packets to avoid network issues.
     */
    public static void syncAllToPlayer(ServerPlayer player, ServerLevel level) {
        if (player == null || level == null) return;
        
        // Sync fluid types
        Map<Long, ResourceLocation> fluidMap = HotbathWaterloggingHelper.getAllStoredFluids(level);
        if (!fluidMap.isEmpty()) {
            syncMapToPlayer(player, fluidMap, true);
        }
        
        // Sync custom fluid IDs (for dynamic custom fluids)
        Map<Long, ResourceLocation> customIdMap = HotbathWaterloggingHelper.getAllStoredCustomFluidIds(level);
        if (!customIdMap.isEmpty()) {
            syncMapToPlayer(player, customIdMap, false);
        }
    }
    
    /**
     * Helper method to sync a map of data to a player, splitting into chunks if needed.
     */
    private static void syncMapToPlayer(ServerPlayer player, Map<Long, ResourceLocation> map, boolean isFluidType) {
        final int CHUNK_SIZE = 500;
        
        if (map.size() <= CHUNK_SIZE) {
            if (isFluidType) {
                PacketDistributor.sendToPlayer(player, new SyncBulkWaterloggingPayload(map));
            } else {
                PacketDistributor.sendToPlayer(player, new SyncBulkCustomFluidIdPayload(map));
            }
        } else {
            // Split large data into multiple packets
            Map<Long, ResourceLocation> chunk = new HashMap<>();
            int count = 0;
            for (Map.Entry<Long, ResourceLocation> entry : map.entrySet()) {
                chunk.put(entry.getKey(), entry.getValue());
                count++;
                
                if (count >= CHUNK_SIZE) {
                    if (isFluidType) {
                        PacketDistributor.sendToPlayer(player, new SyncBulkWaterloggingPayload(new HashMap<>(chunk)));
                    } else {
                        PacketDistributor.sendToPlayer(player, new SyncBulkCustomFluidIdPayload(new HashMap<>(chunk)));
                    }
                    chunk.clear();
                    count = 0;
                }
            }
            
            // Send remaining entries
            if (!chunk.isEmpty()) {
                if (isFluidType) {
                    PacketDistributor.sendToPlayer(player, new SyncBulkWaterloggingPayload(chunk));
                } else {
                    PacketDistributor.sendToPlayer(player, new SyncBulkCustomFluidIdPayload(chunk));
                }
            }
        }
    }
}
