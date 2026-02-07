package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.util.CustomFluidHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

/**
 * Event handler for Serene Seasons integration.
 * 
 * Features:
 * - Grant extra resistance buff when bathing in winter
 * 
 * Note: Ice/snow melting is now handled by IceSnowMeltHandler (core feature, works all seasons)
 */
public class SereneSeasonsEventHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // How often to apply winter buff (every 40 ticks = 2 seconds)
    private static final int BUFF_APPLY_INTERVAL = 40;
    
    // Winter resistance buff duration (10 seconds, will be refreshed)
    private static final int WINTER_BUFF_DURATION = 200;
    
    // Resistance amplifier based on sub-season
    // Early Winter: Level I, Mid Winter: Level II, Late Winter: Level I
    private static final int EARLY_WINTER_AMPLIFIER = 0;
    private static final int MID_WINTER_AMPLIFIER = 1;
    private static final int LATE_WINTER_AMPLIFIER = 0;
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        CompatManager.safeEventCall("sereneseasons", "onPlayerTick", () -> {
            if (event.getEntity().level().isClientSide()) return;
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            
            boolean isInBath = CustomFluidHandler.isPlayerInHotBath(player);
            
            if (isInBath) {
                if (player.tickCount % BUFF_APPLY_INTERVAL == 0) {
                    applyWinterBuff(player);
                }
            }
        });
    }
    
    /**
     * Apply winter resistance buff to player when bathing.
     * The buff is stronger during mid-winter.
     */
    private static void applyWinterBuff(ServerPlayer player) {
        try {
            ISeasonState seasonState = SeasonHelper.getSeasonState(player.level());
            if (seasonState == null) return;
            
            Season currentSeason = seasonState.getSeason();
            
            // Only apply buff in winter
            if (currentSeason != Season.WINTER) return;
            
            Season.SubSeason subSeason = seasonState.getSubSeason();
            int amplifier = getWinterAmplifier(subSeason);
            
            // Apply Resistance effect
            MobEffectInstance currentResistance = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (currentResistance == null || currentResistance.getDuration() < WINTER_BUFF_DURATION) {
                player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    WINTER_BUFF_DURATION,
                    amplifier,
                    false,  // Not ambient
                    true,   // Show particles
                    true    // Show icon
                ));
                LOGGER.debug("Granted winter resistance (level {}) to player {} in hot bath", 
                    amplifier + 1, player.getName().getString());
            }
            
            // Also apply Regeneration as extra healing in deep winter
            if (subSeason == Season.SubSeason.MID_WINTER) {
                MobEffectInstance currentRegen = player.getEffect(MobEffects.REGENERATION);
                if (currentRegen == null || currentRegen.getDuration() < WINTER_BUFF_DURATION) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.REGENERATION,
                        WINTER_BUFF_DURATION,
                        0,      // Level I
                        false,  // Not ambient
                        true,   // Show particles
                        true    // Show icon
                    ));
                    LOGGER.debug("Granted regeneration to player {} in mid-winter hot bath", 
                        player.getName().getString());
                }
            }
            
        } catch (Exception e) {
            LOGGER.debug("Could not apply winter buff: {}", e.getMessage());
        }
    }
    
    /**
     * Get the resistance amplifier based on sub-season.
     */
    private static int getWinterAmplifier(Season.SubSeason subSeason) {
        return switch (subSeason) {
            case EARLY_WINTER -> EARLY_WINTER_AMPLIFIER;
            case MID_WINTER -> MID_WINTER_AMPLIFIER;
            case LATE_WINTER -> LATE_WINTER_AMPLIFIER;
            default -> 0;
        };
    }
}
