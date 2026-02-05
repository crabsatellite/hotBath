package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.crabmod.hotbath.registers.EntityRegister;

import java.util.Map;

/**
 * Client-side event handlers for custom fluid rendering.
 * Handles item color tinting and entity rendering registration.
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class CustomFluidClientEvents {

    /**
     * Registers item color handlers for custom fluid items.
     * This allows the items to display with the color of the fluid they contain.
     * 
     * Note: Smoke overlay visibility is now handled via model overrides (hot_steam predicate)
     * instead of color alpha channel, as Forge 1.20.1's translucent render type doesn't
     * properly support alpha from item tint colors.
     * 
     * Layer structure (matching vanilla potion patterns):
     * - Bucket: layer0=bucket, layer1=fluid overlay (tinted), (layer2=smoke via model override)
     * - Bottle: layer0=potion overlay (tinted), layer1=potion bottle, (layer2=smoke via model override)
     * - Splash: layer0=splash overlay (tinted), layer1=splash bottle, (layer2=smoke via model override)
     */
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        // Custom fluid bucket - tint layer 1 (the fluid inside)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 1) {
                // Fluid layer - apply fluid color
                int color = CustomFluidNBTHelper.getFluidColor(stack);
                if (color != -1) {
                    return color;
                }
                return 0xFF4FC3F7; // Default light blue
            }
            return 0xFFFFFFFF;
        }, CustomFluidItems.CUSTOM_FLUID_BUCKET.get());

        // Custom fluid bottle - tint layer 0 (the fluid overlay like vanilla potion)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                // Fluid overlay layer - apply fluid color (like vanilla potion)
                int color = CustomFluidNBTHelper.getFluidColor(stack);
                if (color != -1) {
                    return color;
                }
                return 0xFF4FC3F7; // Default light blue
            }
            return 0xFFFFFFFF;
        }, CustomFluidItems.CUSTOM_FLUID_BOTTLE.get());

        // Splash custom fluid bottle - tint layer 0 (the fluid overlay like vanilla splash potion)
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) {
                // Fluid overlay layer - apply fluid color (like vanilla splash potion)
                int color = CustomFluidNBTHelper.getFluidColor(stack);
                if (color != -1) {
                    return color;
                }
                return 0xFF4FC3F7; // Default light blue
            }
            return 0xFFFFFFFF;
        }, CustomFluidItems.CUSTOM_FLUID_SPLASH_BOTTLE.get());
    }

    /**
     * Registers entity renderers for custom fluid projectiles.
     */
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegister.THROWN_CUSTOM_FLUID_BOTTLE.get(), ThrownItemRenderer::new);
    }
    
    // Light update scheduling state
    private static volatile boolean pendingLightUpdate = false;
    private static int pendingTickDelay = 0;
    
    /**
     * Schedules light updates for all custom fluid blocks after registry sync.
     */
    public static void updateAllCustomFluidLights() {
        pendingLightUpdate = true;
        pendingTickDelay = 20; // 1 second delay
    }
    
    /**
     * Performs the actual light update for all loaded custom fluid blocks.
     */
    private static void performLightUpdate() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        
        if (level == null || player == null) {
            return;
        }
        
        pendingLightUpdate = false;
        int updatedCount = 0;
        int renderDistance = minecraft.options.renderDistance().get();
        ChunkPos playerChunkPos = new ChunkPos(player.blockPosition());
        
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(playerChunkPos.x + dx, playerChunkPos.z + dz);
                if (chunk == null) continue;
                
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (entry.getValue() instanceof CustomFluidBlockEntity customBe && customBe.getFluidId() != null) {
                        level.getLightEngine().checkBlock(entry.getKey());
                        updatedCount++;
                    }
                }
            }
        }
        
        if (updatedCount > 0) {
            HotBath.LOGGER.debug("Updated light for {} custom fluid blocks", updatedCount);
        }
    }
    
    /**
     * Client tick handler for processing pending light updates.
     */
    @Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class LightUpdateHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !pendingLightUpdate) {
                return;
            }
            
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }
            
            if (pendingTickDelay > 0) {
                pendingTickDelay--;
                return;
            }
            
            performLightUpdate();
        }
    }
}
