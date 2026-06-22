package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.util.EffectRemovalHandler;
import com.crabmod.hotbath.util.HealthRegenHandler;
import com.crabmod.hotbath.util.HungerRegenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

// ** Honey Bath Block */
public class HoneyBathBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    public HoneyBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier, properties);
    }

    private static final int TICK_NUMBER = 20;
    private static final int STAYED_EFFECT_TRIGGER_TIME_SECONDS = 15;

    @Override
    protected void applyBathEffects(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) {
            return;
        }

        if (!(entity instanceof ServerPlayer player)) return;
        if (!player.isAlive()) return;

        InsideAreaResult result = trackInside(player);

        if (!result.shouldProcess()) {
            return;
        }

        int stayedTicks = result.stayedTicks();

        HealthRegenHandler.regenHealth(0.25F, 2, player);
        HungerRegenHandler.regenHunger(1, 4, player);

        player.addEffect(
                new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        10 * TICK_NUMBER,
                        0,
                        false,
                        false,
                        true
                )
        );

        if (stayedTicks >= STAYED_EFFECT_TRIGGER_TIME_SECONDS * TICK_NUMBER) {
            EffectRemovalHandler.removeNegativeEffectsExceptSlowAndUnluck(player);

            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.ABSORPTION,
                            20 * TICK_NUMBER,
                            1,
                            false,
                            false,
                            true
                    )
            );
        }
    }
}
