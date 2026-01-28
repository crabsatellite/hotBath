package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.util.CustomFluidHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import vectorwing.farmersdelight.common.registry.ModEffects;

/**
 * Event handler for Farmer's Delight integration.
 * 
 * Features:
 * - Grant Comfort effect when bathing in hot water
 *   Comfort provides slow regeneration (1 HP every 4 seconds) regardless of hunger
 */
public class FarmersDelightEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // How often to apply Comfort effect (every 40 ticks = 2 seconds)
    private static final int EFFECT_APPLY_INTERVAL = 40;
    
    // Comfort effect duration when in bath (5 seconds, will be refreshed)
    private static final int COMFORT_DURATION = 100;
    
    // Comfort effect amplifier (0 = level I)
    private static final int COMFORT_AMPLIFIER = 0;
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Check if player is in any hot bath block
        boolean isInBath = CustomFluidHandler.isPlayerInHotBathBlock(player);
        
        if (isInBath) {
            // Apply Comfort effect every 2 seconds while in hot bath
            if (player.tickCount % EFFECT_APPLY_INTERVAL == 0) {
                grantComfortEffect(player);
            }
        }
    }
    
    /**
     * Grant the Comfort effect to the player while bathing.
     * Comfort provides slow regeneration (1 HP every 4 seconds) regardless of hunger level.
     * This represents the relaxing, healing nature of a hot bath.
     */
    private static void grantComfortEffect(ServerPlayer player) {
        try {
            // Get the Comfort effect from Farmer's Delight
            Holder<MobEffect> comfortEffect = ModEffects.COMFORT;
            if (comfortEffect != null) {
                // Only apply if player doesn't already have a longer duration
                MobEffectInstance currentEffect = player.getEffect(comfortEffect);
                if (currentEffect == null || currentEffect.getDuration() < COMFORT_DURATION) {
                    player.addEffect(new MobEffectInstance(
                        comfortEffect,
                        COMFORT_DURATION,
                        COMFORT_AMPLIFIER,
                        false,  // Not ambient
                        true,   // Show particles
                        true    // Show icon
                    ));
                    LOGGER.debug("Granted Comfort effect to player {} in hot bath", player.getName().getString());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not apply Comfort effect: {}", e.getMessage());
        }
    }
}
