package com.crabmod.hotbath.compat;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides temperature effects for Hot Bath blocks in Cold Sweat mod
 * This class handles the actual integration with Cold Sweat's API
 */
public class ColdSweatBlockTemperatureProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Store block temperature configurations
    private static final Map<ResourceLocation, BlockTemperatureConfig> BLOCK_TEMPS = new HashMap<>();

    /**
     * Configuration for block temperature effect
     */
    public static class BlockTemperatureConfig {
        public final double temperature;
        public final double range;
        public final boolean requiresContact;
        public final boolean affectsBody;

        public BlockTemperatureConfig(double temperature, double range, boolean requiresContact, boolean affectsBody) {
            this.temperature = temperature;
            this.range = range;
            this.requiresContact = requiresContact;
            this.affectsBody = affectsBody;
        }
    }

    /**
     * Register a warm block (positive temperature)
     */
    public static void registerWarmBlock(ResourceLocation blockId, double temperature, double range, 
                                        boolean requiresContact, boolean affectsBody) {
        BLOCK_TEMPS.put(blockId, new BlockTemperatureConfig(temperature, range, requiresContact, affectsBody));
        
        try {
            // Note: In Cold Sweat 2.4+, block temperatures are configured via JSON files
            // The JSON file in data/hotbath/coldsweat/block_temperatures.json handles the registration
            // This code just tracks the configuration for runtime temperature application
            LOGGER.debug("Registered warm block: {} with temp={}, range={}", blockId, temperature, range);
            
        } catch (Exception e) {
            LOGGER.warn("Could not register block {} with Cold Sweat, using fallback method", blockId, e);
        }
    }

    /**
     * Apply temperature effect directly to player
     * Called when player is inside the bath liquid
     */
    public static void applyPlayerTemperature(Player player, ResourceLocation blockId) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        BlockTemperatureConfig config = BLOCK_TEMPS.get(blockId);
        if (config == null) {
            return;
        }

        try {
            // Apply temperature directly to player's body temperature
            // This provides immediate warmth when bathing
            double currentTemp = Temperature.get(player, Temperature.Trait.BODY);
            double targetTemp = config.temperature * 10; // Scale to Cold Sweat's temperature range
            
            // Gradually adjust temperature (don't instantly set it)
            double adjustment = Math.signum(targetTemp - currentTemp) * Math.min(Math.abs(targetTemp - currentTemp), 0.5);
            
            if (Math.abs(adjustment) > 0.01) {
                Temperature.add(player, Temperature.Trait.BODY, adjustment);
            }
            
        } catch (Exception e) {
            LOGGER.debug("Could not apply direct temperature effect: {}", e.getMessage());
        }
    }

    /**
     * Check if player is in contact with a hot bath block
     */
    public static boolean isPlayerInHotBath(Player player) {
        if (player == null) {
            return false;
        }

        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        
        // Check the block the player is standing in and around
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    ResourceLocation blockId = level.registryAccess().registryOrThrow(
                        net.minecraft.core.registries.Registries.BLOCK
                    ).getKey(state.getBlock());
                    
                    if (blockId != null && BLOCK_TEMPS.containsKey(blockId)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * Get temperature effect for a specific block
     */
    public static double getBlockTemperature(ResourceLocation blockId) {
        BlockTemperatureConfig config = BLOCK_TEMPS.get(blockId);
        return config != null ? config.temperature : 0.0;
    }
}
