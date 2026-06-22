package com.crabmod.hotbath.gametest;

import com.mojang.authlib.GameProfile;
import com.crabmod.hotbath.fluid_blocks.IInsideAreaTracker;
import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder("hotbath_waterlogging")
@PrefixGameTestTemplate(false)
public class WaterloggedBathEffectsGameTest {
    private static final BlockPos WATERLOGGED_BLOCK_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty_1x1", timeoutTicks = 180)
    public static void waterlogged_stairs_apply_herbal_bath_effects(GameTestHelper helper) {
        BlockState waterloggedStairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true);
        helper.setBlock(WATERLOGGED_BLOCK_POS, waterloggedStairs);

        BlockPos absolutePos = helper.absolutePos(WATERLOGGED_BLOCK_POS);
        HotbathWaterloggingHelper.storeFluidType(
                helper.getLevel(),
                absolutePos,
                FluidsRegister.HERBAL_BATH_FLUID.get()
        );

        ServerPlayer player = new EffectOnlyServerPlayer(helper.getLevel());
        IInsideAreaTracker.cleanupPlayer(player.getUUID());
        Vec3 playerPos = Vec3.atBottomCenterOf(absolutePos).add(0.0D, 0.1D, 0.0D);
        player.moveTo(playerPos.x, playerPos.y, playerPos.z, 0.0F, 0.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);

        helper.onEachTick(() -> {
            player.tickCount++;
            player.moveTo(playerPos.x, playerPos.y, playerPos.z, 0.0F, 0.0F);
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        });
        helper.succeedWhen(() -> helper.assertLivingEntityHasMobEffect(player, MobEffects.DAMAGE_RESISTANCE, 0));
    }

    private static final class EffectOnlyServerPlayer extends ServerPlayer {
        private EffectOnlyServerPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "waterlog-test"), ClientInformation.createDefault());
        }

        @Override
        protected void onEffectAdded(MobEffectInstance effectInstance, Entity entity) {
        }

        @Override
        protected void onEffectUpdated(MobEffectInstance effectInstance, boolean forced, Entity entity) {
        }

        @Override
        protected void onEffectRemoved(MobEffectInstance effectInstance) {
        }
    }
}
