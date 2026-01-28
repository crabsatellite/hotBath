package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.util.AdvancementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Hot Water Block
 */
public class HotWaterBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    public HotWaterBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier, properties);
    }

    private static final int TICK_NUMBER = 20;
    private static final int ENTERED_TRIGGER_COUNT = 100;
    private static final int STAYED_EFFECT_TRIGGER_TIME_SECONDS = 15;
    private static final String ADVANCEMENT_ID = "hotbath:foot_health";

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide) {
            return;
        }
        if (!(entity instanceof ServerPlayer player)) return;
        if (!player.isAlive()) return;

        InsideAreaResult result = trackInside(player);

        if (!result.shouldProcess()) {
            return;
        }

        if (result.isFirstEnter()) {
            if (result.totalEnterCount() >= ENTERED_TRIGGER_COUNT) {
                AdvancementHelper.tryAwardAdvancement(player, ADVANCEMENT_ID, "code_triggered");
            }
        }

        if (result.stayedTicks() >= STAYED_EFFECT_TRIGGER_TIME_SECONDS * TICK_NUMBER) {
            player.addEffect(
                new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    20 * TICK_NUMBER,
                    0,
                    false,
                    false,
                    true
                )
            );
        }
    }
}










