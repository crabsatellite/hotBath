package com.crabmod.hotbath.waterlogging;

import com.crabmod.hotbath.HotBath;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Network handling for syncing waterlogging data from server to client.
 * Uses Forge SimpleChannel for networking.
 */
public class WaterloggingNetworking {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HotBath.MOD_ID, "waterlogging"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    /**
     * Register all packets
     */
    public static void register() {
        CHANNEL.registerMessage(packetId++, SyncWaterloggingPacket.class,
                SyncWaterloggingPacket::encode,
                SyncWaterloggingPacket::decode,
                SyncWaterloggingPacket::handle);
        CHANNEL.registerMessage(packetId++, SyncBulkWaterloggingPacket.class,
                SyncBulkWaterloggingPacket::encode,
                SyncBulkWaterloggingPacket::decode,
                SyncBulkWaterloggingPacket::handle);
    }
    
    /**
     * Packet for syncing single waterlogging fluid type
     */
    public static class SyncWaterloggingPacket {
        private final BlockPos pos;
        private final ResourceLocation fluidId;
        
        public SyncWaterloggingPacket(BlockPos pos, ResourceLocation fluidId) {
            this.pos = pos;
            this.fluidId = fluidId;
        }
        
        public static void encode(SyncWaterloggingPacket packet, FriendlyByteBuf buf) {
            buf.writeBlockPos(packet.pos);
            buf.writeResourceLocation(packet.fluidId);
        }
        
        public static SyncWaterloggingPacket decode(FriendlyByteBuf buf) {
            return new SyncWaterloggingPacket(
                    buf.readBlockPos(),
                    buf.readResourceLocation()
            );
        }
        
        public static void handle(SyncWaterloggingPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (packet == null || packet.fluidId == null || packet.pos == null) return;
                
                // Client side handling - safely handle unregistered fluids
                if (ForgeRegistries.FLUIDS.containsKey(packet.fluidId)) {
                    Fluid fluid = ForgeRegistries.FLUIDS.getValue(packet.fluidId);
                    if (fluid != null) {
                        HotbathWaterloggingHelper.updateClientCache(packet.pos, fluid);
                    }
                } else {
                    // Fluid no longer exists, remove from cache
                    HotbathWaterloggingHelper.removeFromClientCache(packet.pos);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Packet for syncing bulk waterlogging data (for player join/dimension change)
     */
    public static class SyncBulkWaterloggingPacket {
        private final Map<Long, ResourceLocation> fluidMap;
        
        public SyncBulkWaterloggingPacket(Map<Long, ResourceLocation> fluidMap) {
            this.fluidMap = fluidMap;
        }
        
        public static void encode(SyncBulkWaterloggingPacket packet, FriendlyByteBuf buf) {
            buf.writeVarInt(packet.fluidMap.size());
            for (Map.Entry<Long, ResourceLocation> entry : packet.fluidMap.entrySet()) {
                buf.writeLong(entry.getKey());
                buf.writeResourceLocation(entry.getValue());
            }
        }
        
        public static SyncBulkWaterloggingPacket decode(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            Map<Long, ResourceLocation> map = new HashMap<>();
            for (int i = 0; i < size; i++) {
                long posLong = buf.readLong();
                ResourceLocation fluidId = buf.readResourceLocation();
                map.put(posLong, fluidId);
            }
            return new SyncBulkWaterloggingPacket(map);
        }
        
        public static void handle(SyncBulkWaterloggingPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (packet == null || packet.fluidMap == null) return;
                
                // Client side handling
                for (Map.Entry<Long, ResourceLocation> entry : packet.fluidMap.entrySet()) {
                    BlockPos pos = BlockPos.of(entry.getKey());
                    ResourceLocation fluidId = entry.getValue();
                    if (fluidId == null) continue;
                    
                    // Safely handle unregistered fluids
                    if (ForgeRegistries.FLUIDS.containsKey(fluidId)) {
                        Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
                        if (fluid != null) {
                            HotbathWaterloggingHelper.updateClientCache(pos, fluid);
                        }
                    }
                    // Don't add to cache if fluid doesn't exist
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Send waterlogging sync to all players
     */
    public static void syncToAllPlayers(BlockPos pos, Fluid fluid) {
        if (pos == null || fluid == null) return;
        
        ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid);
        if (fluidId != null) {
            SyncWaterloggingPacket packet = new SyncWaterloggingPacket(pos, fluidId);
            CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
        }
    }
    
    /**
     * Send waterlogging clear to all players
     */
    public static void syncRemoveToAllPlayers(BlockPos pos) {
        if (pos == null) return;
        
        ResourceLocation emptyId = ForgeRegistries.FLUIDS.getKey(Fluids.EMPTY);
        if (emptyId != null) {
            SyncWaterloggingPacket packet = new SyncWaterloggingPacket(pos, emptyId);
            CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
        }
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
            SyncBulkWaterloggingPacket packet = new SyncBulkWaterloggingPacket(fluidMap);
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        } else {
            // Split large data into multiple packets
            Map<Long, ResourceLocation> chunk = new HashMap<>();
            int count = 0;
            for (Map.Entry<Long, ResourceLocation> entry : fluidMap.entrySet()) {
                chunk.put(entry.getKey(), entry.getValue());
                count++;
                
                if (count >= CHUNK_SIZE) {
                    SyncBulkWaterloggingPacket packet = new SyncBulkWaterloggingPacket(new HashMap<>(chunk));
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
                    chunk.clear();
                    count = 0;
                }
            }
            
            // Send remaining entries
            if (!chunk.isEmpty()) {
                SyncBulkWaterloggingPacket packet = new SyncBulkWaterloggingPacket(chunk);
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
