package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.crabmod.hotbath.util.CustomFluidHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import sereneseasons.api.season.ISeasonState;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

/**
 * Event handler for Serene Seasons integration.
 * 
 * Features:
 * - Grant extra resistance buff when bathing in winter
 * - Prevent water from freezing near hot bath liquids
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
    
    // Radius to check for water blocks to prevent freezing
    private static final int ANTI_FREEZE_RADIUS = 3;
    
    // How often to check for ice melting (every 100 ticks = 5 seconds)
    private static final int ICE_MELT_INTERVAL = 100;
    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        
        // Check if player is in any hot bath block
        boolean isInBath = CustomFluidHandler.isPlayerInHotBathBlock(player);
        
        if (isInBath) {
            // Apply winter resistance buff every 2 seconds while in hot bath
            if (player.tickCount % BUFF_APPLY_INTERVAL == 0) {
                applyWinterBuff(player);
            }
        }
    }
    
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide()) return;
        if (!(event.level instanceof ServerLevel level)) return;
        
        // Only check periodically
        if (level.getGameTime() % ICE_MELT_INTERVAL != 0) return;
        
        // Check if it's winter and try to get season state
        try {
            ISeasonState seasonState = SeasonHelper.getSeasonState(level);
            if (seasonState == null) return;
            
            Season currentSeason = seasonState.getSeason();
            
            // Only melt ice in winter (when it would normally freeze)
            if (currentSeason == Season.WINTER) {
                meltIceNearHotBaths(level);
            }
        } catch (Exception e) {
            // Season system may not be active in this dimension
            LOGGER.debug("Could not get season state: {}", e.getMessage());
        }
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
        switch (subSeason) {
            case EARLY_WINTER:
                return EARLY_WINTER_AMPLIFIER;
            case MID_WINTER:
                return MID_WINTER_AMPLIFIER;
            case LATE_WINTER:
                return LATE_WINTER_AMPLIFIER;
            default:
                return 0;
        }
    }
    
    /**
     * Melt ice blocks near hot bath liquids.
     * This prevents water from freezing near hot springs.
     */
    private static void meltIceNearHotBaths(ServerLevel level) {
        // Iterate through loaded chunks and find hot bath blocks
        // For performance, we limit the search to players' nearby areas
        for (ServerPlayer player : level.players()) {
            BlockPos playerPos = player.blockPosition();
            
            // Search in a reasonable area around the player
            for (int x = -16; x <= 16; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -16; z <= 16; z++) {
                        BlockPos pos = playerPos.offset(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        
                        // Check if this is a hot bath block
                        if (state.getBlock() instanceof AbstractHotbathBlock) {
                            // Melt ice in radius around this hot bath
                            meltIceAroundPosition(level, pos);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Melt ice and snow blocks around a hot bath position.
     */
    private static void meltIceAroundPosition(ServerLevel level, BlockPos hotBathPos) {
        for (int x = -ANTI_FREEZE_RADIUS; x <= ANTI_FREEZE_RADIUS; x++) {
            for (int y = -ANTI_FREEZE_RADIUS; y <= ANTI_FREEZE_RADIUS; y++) {
                for (int z = -ANTI_FREEZE_RADIUS; z <= ANTI_FREEZE_RADIUS; z++) {
                    BlockPos checkPos = hotBathPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    // Melt ice to water
                    if (state.is(Blocks.ICE)) {
                        level.setBlockAndUpdate(checkPos, Blocks.WATER.defaultBlockState());
                        LOGGER.debug("Melted ice at {} near hot bath at {}", checkPos, hotBathPos);
                    }
                    // Remove snow layers
                    else if (state.is(Blocks.SNOW)) {
                        level.setBlockAndUpdate(checkPos, Blocks.AIR.defaultBlockState());
                        LOGGER.debug("Melted snow at {} near hot bath at {}", checkPos, hotBathPos);
                    }
                }
            }
        }
    }
}
