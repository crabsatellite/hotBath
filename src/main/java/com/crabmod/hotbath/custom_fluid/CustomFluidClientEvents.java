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
    
    /**
     * Flag indicating that light updates are pending.
     * This is set when updateAllCustomFluidLights() is called but the level is not ready.
     * The LightUpdateHandler will check this flag on each client tick.
     */
    private static volatile boolean pendingLightUpdate = false;
    private static int pendingTickDelay = 0;
    private static final int LIGHT_UPDATE_DELAY_TICKS = 20; // Wait 1 second after level is ready
    
    /**
     * Requests an update of light emission for all loaded custom fluid blocks.
     * The actual update will be performed after the level is fully loaded.
     * This fixes the issue where custom fluids with luminosity > 0 would lose their light
     * after quitting and rejoining the game.
     */
    public static void updateAllCustomFluidLights() {
        pendingLightUpdate = true;
        pendingTickDelay = LIGHT_UPDATE_DELAY_TICKS;
        HotBath.LOGGER.debug("Scheduled custom fluid light update");
    }
    
    /**
     * Actually performs the light update for all custom fluid blocks.
     * Called from the tick handler when the level is ready.
     */
    static void performLightUpdate() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        
        if (level == null || player == null) {
            return; // Still not ready, will retry on next tick
        }
        
        pendingLightUpdate = false;
        pendingTickDelay = 0;
        
        int updatedCount = 0;
        
        // Get render distance to determine how many chunks to check
        int renderDistance = minecraft.options.renderDistance().get();
        ChunkPos playerChunkPos = new ChunkPos(player.blockPosition());
        
        // Iterate through chunks within render distance
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int chunkX = playerChunkPos.x + dx;
                int chunkZ = playerChunkPos.z + dz;
                
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                
                Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
                for (Map.Entry<BlockPos, BlockEntity> entry : blockEntities.entrySet()) {
                    if (entry.getValue() instanceof CustomFluidBlockEntity customBe) {
                        // Update light for all custom fluid blocks that have a fluid ID
                        // We don't check luminosity here because the registry is now populated
                        // and getLightEmission() will return the correct value
                        if (customBe.getFluidId() != null) {
                            level.getLightEngine().checkBlock(entry.getKey());
                            updatedCount++;
                        }
                    }
                }
            }
        }
        
        if (updatedCount > 0) {
            HotBath.LOGGER.debug("Updated light for {} custom fluid blocks after sync", updatedCount);
        }
    }
    
    /**
     * Client tick event handler for processing pending light updates.
     * This is in a separate inner class registered to the FORGE event bus.
     */
    @Mod.EventBusSubscriber(modid = HotBath.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class LightUpdateHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            
            if (!pendingLightUpdate) {
                return;
            }
            
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return; // Level not ready yet, keep waiting
            }
            
            // Countdown the delay to give chunks time to fully load
            if (pendingTickDelay > 0) {
                pendingTickDelay--;
                return;
            }
            
            // Level is ready and delay has passed, perform the update
            performLightUpdate();
        }
    }
}
