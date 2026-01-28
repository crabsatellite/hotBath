package com.crabmod.hotbath.dirtiness;

import com.crabmod.hotbath.HotBath;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Network handling for syncing dirtiness data from server to client.
 * Uses Forge SimpleChannel for networking.
 */
public class DirtinessNetworking {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HotBath.MOD_ID, "dirtiness"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    /**
     * Register all packets
     */
    public static void register() {
        CHANNEL.registerMessage(packetId++, SyncDirtinessPacket.class,
                SyncDirtinessPacket::encode,
                SyncDirtinessPacket::decode,
                SyncDirtinessPacket::handle);
        
        CHANNEL.registerMessage(packetId++, SyncOtherPlayerDirtinessPacket.class,
                SyncOtherPlayerDirtinessPacket::encode,
                SyncOtherPlayerDirtinessPacket::decode,
                SyncOtherPlayerDirtinessPacket::handle);
    }
    
    /**
     * Packet for syncing local player's dirtiness
     */
    public static class SyncDirtinessPacket {
        private final float dirtiness;
        private final long dirtSeed;
        private final boolean hasFlies;
        
        public SyncDirtinessPacket(float dirtiness, long dirtSeed, boolean hasFlies) {
            this.dirtiness = dirtiness;
            this.dirtSeed = dirtSeed;
            this.hasFlies = hasFlies;
        }
        
        public static void encode(SyncDirtinessPacket packet, FriendlyByteBuf buf) {
            buf.writeFloat(packet.dirtiness);
            buf.writeVarLong(packet.dirtSeed);
            buf.writeBoolean(packet.hasFlies);
        }
        
        public static SyncDirtinessPacket decode(FriendlyByteBuf buf) {
            return new SyncDirtinessPacket(
                    buf.readFloat(),
                    buf.readVarLong(),
                    buf.readBoolean()
            );
        }
        
        public static void handle(SyncDirtinessPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Client side handling
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    DirtinessClientData.setDirtiness(
                            mc.player.getUUID(),
                            packet.dirtiness,
                            packet.dirtSeed,
                            packet.hasFlies
                    );
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Packet for syncing other player's dirtiness (for flies visibility)
     */
    public static class SyncOtherPlayerDirtinessPacket {
        private final UUID playerId;
        private final float dirtiness;
        private final long dirtSeed;
        private final boolean hasFlies;
        
        public SyncOtherPlayerDirtinessPacket(UUID playerId, float dirtiness, long dirtSeed, boolean hasFlies) {
            this.playerId = playerId;
            this.dirtiness = dirtiness;
            this.dirtSeed = dirtSeed;
            this.hasFlies = hasFlies;
        }
        
        public static void encode(SyncOtherPlayerDirtinessPacket packet, FriendlyByteBuf buf) {
            buf.writeUUID(packet.playerId);
            buf.writeFloat(packet.dirtiness);
            buf.writeVarLong(packet.dirtSeed);
            buf.writeBoolean(packet.hasFlies);
        }
        
        public static SyncOtherPlayerDirtinessPacket decode(FriendlyByteBuf buf) {
            return new SyncOtherPlayerDirtinessPacket(
                    buf.readUUID(),
                    buf.readFloat(),
                    buf.readVarLong(),
                    buf.readBoolean()
            );
        }
        
        public static void handle(SyncOtherPlayerDirtinessPacket packet, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // Store other player's dirtiness in client-side cache
                DirtinessClientData.setDirtiness(
                        packet.playerId,
                        packet.dirtiness,
                        packet.dirtSeed,
                        packet.hasFlies
                );
            });
            ctx.get().setPacketHandled(true);
        }
    }
    
    /**
     * Send dirtiness sync packet to a specific player
     */
    public static void syncToClient(ServerPlayer player) {
        DirtinessCapability.get(player).ifPresent(data -> {
            long gameTime = player.level().getGameTime();
            float dirtiness = data.getDirtiness(gameTime);
            boolean hasFlies = data.shouldSpawnFlies(gameTime);
            
            // Send to self
            CHANNEL.sendTo(
                    new SyncDirtinessPacket(dirtiness, data.getDirtSeed(), hasFlies),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
            
            // Broadcast to nearby players so they can see flies/dirt overlay
            CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY.with(() -> player),
                    new SyncOtherPlayerDirtinessPacket(player.getUUID(), dirtiness, data.getDirtSeed(), hasFlies)
            );
        });
    }
}
