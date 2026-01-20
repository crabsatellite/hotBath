package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.util.EffectRemovalHandler;
import com.crabmod.hotbath.util.HealthRegenHandler;
import com.crabmod.hotbath.util.HungerRegenHandler;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Milk Bath Block
 */
public class MilkBathBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    public MilkBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier, properties);
    }

    private static final int TICK_NUMBER = 20;
    private static final int ENTERED_COUNT_TRIGGER_NUMBER = 100;
    private static final int STAYED_EFFECT_TRIGGER_TIME_SECONDS = 15;
    private static final String ADVANCEMENT_ID = "hotbath:milk_skin";

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
            int entered = result.totalEnterCount();

            if (entered >= ENTERED_COUNT_TRIGGER_NUMBER) {
                Advancement advancement =
                    Objects.requireNonNull(player.getServer())
                        .getAdvancements()
                        .getAdvancement(Objects.requireNonNull(ResourceLocation.tryParse(ADVANCEMENT_ID)));

                if (advancement != null) {
                    player.getAdvancements().award(advancement, "code_triggered");
                }
            }
        }

        int stayedTicks = result.stayedTicks();

        HungerRegenHandler.regenHunger(1, 15, player);
        HealthRegenHandler.regenHealth(0.25F, 2, player);

        if (stayedTicks >= STAYED_EFFECT_TRIGGER_TIME_SECONDS * TICK_NUMBER) {
            EffectRemovalHandler.removeNegativeEffectsExceptUnluck(player);
        }
    }
}










