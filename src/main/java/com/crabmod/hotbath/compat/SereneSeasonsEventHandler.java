package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;

/**
 * Event handler for Serene Seasons integration.
 * During winter, bathing in hot baths grants extra effects:
 * - Early/Late Winter: Resistance I (10 seconds)
 * - Mid Winter: Resistance II + Regeneration I (10 seconds)
 * Effects are refreshed every 2 seconds while bathing.
 */
public class SereneSeasonsEventHandler {

    private static final int CHECK_INTERVAL_TICKS = 40; // 2 seconds
    private static final int EFFECT_DURATION_TICKS = 200; // 10 seconds

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        CompatManager.safeEventCall("sereneseasons", "onPlayerTick", () -> {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;

            if (player.level().isClientSide()) return;
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            if (!serverPlayer.isAlive()) return;

            // Only check every 2 seconds
            if (serverPlayer.tickCount % CHECK_INTERVAL_TICKS != 0) return;

            // Check if player is in a hot bath
            Level level = serverPlayer.level();
            BlockPos playerPos = serverPlayer.blockPosition();
            BlockState blockState = level.getBlockState(playerPos);

            if (!(blockState.getBlock() instanceof AbstractHotbathBlock hotbathBlock)) return;
            if (!hotbathBlock.isHotBath(level, playerPos)) return;

            // Check season
            Season.SubSeason subSeason = SeasonHelper.getSeasonState(level).getSubSeason();

            switch (subSeason) {
                case EARLY_WINTER, LATE_WINTER -> {
                    // Resistance I
                    serverPlayer.addEffect(new MobEffectInstance(
                            MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, 0, false, true));
                }
                case MID_WINTER -> {
                    // Resistance II + Regeneration I
                    serverPlayer.addEffect(new MobEffectInstance(
                            MobEffects.DAMAGE_RESISTANCE, EFFECT_DURATION_TICKS, 1, false, true));
                    serverPlayer.addEffect(new MobEffectInstance(
                            MobEffects.REGENERATION, EFFECT_DURATION_TICKS, 0, false, true));
                }
                default -> {
                    // Not winter - no extra effects
                }
            }
        });
    }
}
