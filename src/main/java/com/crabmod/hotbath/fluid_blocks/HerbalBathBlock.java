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

        if (!(entity instanceof ServerPlayer player)) {
            // Check if entity is undead (using MobType check for 1.20 compatibility)
            if (entity instanceof LivingEntity livingEntity && livingEntity.getMobType() == MobType.UNDEAD) {
                if (entity.tickCount % 20 == 0) {
                    entity.hurt(level.damageSources().magic(), 0.5F);
                }
            }
            return;
        }

        if (!player.isAlive()) {
            return;
        }
        InsideAreaResult result = trackInside(player);

        if (!result.shouldProcess()) {
            return;
        }

        HealthRegenHandler.regenHealth(0.25F, 2, player);

        // Only check advancement on first enter to avoid redundant checks
        if (result.isFirstEnter() && result.totalEnterCount() >= ENTERED_TRIGGER_COUNT) {
            AdvancementHelper.tryAwardAdvancement(player, ADVANCEMENT_ID, "code_triggered");
        }

        if (result.stayedTicks() >= EFFECT_TRIGGER_SECONDS * TICK_NUMBER) {
            ResistanceBoostHandler.applyResistanceBoost(10, player);
        }

        if (result.stayedTicks() >= 15 * TICK_NUMBER) {
            EffectRemovalHandler.removeNegativeEffects(player);
        }
    }
}











