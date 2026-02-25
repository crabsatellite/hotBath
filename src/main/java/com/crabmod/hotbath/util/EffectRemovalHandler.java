package com.crabmod.hotbath.util;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class EffectRemovalHandler {
    public static void removeNegativeEffectsExceptUnluck(ServerPlayer player) {
        List<MobEffectInstance> activeEffects = new ArrayList<>(player.getActiveEffects());

        for (MobEffectInstance effectInstance : activeEffects) {
            MobEffect effectHolder = effectInstance.getEffect();

            if (isHarmfulEffect(effectHolder) && effectHolder != MobEffects.UNLUCK) {
                player.removeEffect(effectHolder);
            }
        }
    }

    private static boolean isHarmfulEffect(MobEffect effect) {
        return effect.getCategory() == MobEffectCategory.HARMFUL && effect != MobEffects.BAD_OMEN;
    }

    public static void removeNegativeEffectsExceptSlowAndUnluck(ServerPlayer player) {
        List<MobEffectInstance> activeEffects = new ArrayList<>(player.getActiveEffects());

        for (MobEffectInstance effectInstance : activeEffects) {
            MobEffect effectHolder = effectInstance.getEffect();

            if (isHarmfulEffect(effectHolder)
                    && effectHolder != MobEffects.UNLUCK
                    && effectHolder != MobEffects.MOVEMENT_SLOWDOWN) {
                player.removeEffect(effectInstance.getEffect());
            }
        }
    }

    public static void removeNegativeEffects(LivingEntity entity) {
        List<MobEffectInstance> activeEffects = new ArrayList<>(entity.getActiveEffects());

        for (MobEffectInstance effectInstance : activeEffects) {
            MobEffect effect = effectInstance.getEffect();

            if (isHarmfulEffect(effect)) {
                entity.removeEffect(effect);
                // ServerPlayer.onEffectRemoved() already sends ClientboundRemoveMobEffectPacket,
                // but non-player entities do NOT sync effect removal to clients.
                // Manually broadcast the removal packet so clients update their visual state
                // (e.g., Alex's Caves IRRADIATED glow is rendered client-side via hasEffect()).
                if (!(entity instanceof ServerPlayer) && entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.getChunkSource().broadcastAndSend(entity,
                            new ClientboundRemoveMobEffectPacket(entity.getId(), effect));
                }
            }
        }
    }

    public static void removeBadOmen(ServerPlayer player) {
        player.removeEffect(MobEffects.BAD_OMEN);
    }
}










