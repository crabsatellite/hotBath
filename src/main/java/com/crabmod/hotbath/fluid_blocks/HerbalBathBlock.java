package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.util.AdvancementHelper;
import com.crabmod.hotbath.util.EffectRemovalHandler;
import com.crabmod.hotbath.util.HealthRegenHandler;
import com.crabmod.hotbath.util.ResistanceBoostHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Herbal Bath Block
 */
public class HerbalBathBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    public HerbalBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier, properties);
    }

    private static final int TICK_NUMBER = 20;
    static final String ADVANCEMENT_ID = "hotbath:chronic_invalid";
    private static final int ENTERED_TRIGGER_COUNT = 100;
    private static final int EFFECT_TRIGGER_SECONDS = 5;

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide) {
            return;
        }

        // Unified logic: track ALL living entities with the same tracker
        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isAlive()) {
            return;
        }

        // Undead take damage in herbal bath
        if (livingEntity.getMobType() == MobType.UNDEAD) {
            if (entity.tickCount % 20 == 0) {
                entity.hurt(level.damageSources().magic(), 0.5F);
            }
        }

        // Same tracking logic for players and mobs
        InsideAreaResult result = trackInside(entity);

        if (!result.shouldProcess()) {
            return;
        }

        // Negative effect removal for ALL living entities (after 15 seconds)
        if (result.stayedTicks() >= 15 * TICK_NUMBER) {
            EffectRemovalHandler.removeNegativeEffects(livingEntity);
        }

        // Player-only features below
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        HealthRegenHandler.regenHealth(0.25F, 2, player);

        if (result.isFirstEnter() && result.totalEnterCount() >= ENTERED_TRIGGER_COUNT) {
            AdvancementHelper.tryAwardAdvancement(player, ADVANCEMENT_ID, "code_triggered");
        }

        if (result.stayedTicks() >= EFFECT_TRIGGER_SECONDS * TICK_NUMBER) {
            ResistanceBoostHandler.applyResistanceBoost(10, player);
        }
    }
}











