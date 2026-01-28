package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.events.enter_fluid_events.PeonyBathEvents;
import com.crabmod.hotbath.util.EffectRemovalHandler;
import com.crabmod.hotbath.util.HealthRegenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Peony Bath Block
 */
public class PeonyBathBlock extends AbstractHotbathBlock implements IInsideAreaTracker {
    public PeonyBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier, properties);
    }

    private static final int TICK_NUMBER = 20;
    private static final int LUCK_THRESHOLD = 50;
    public static final ResourceLocation ATTACK_SPEED_MODIFIER_NAME =
            ResourceLocation.fromNamespaceAndPath(
                    HotBath.MOD_ID, "peony_bath_attack_speed_modifier");

    public static final ResourceLocation KNOCKBACK_RESISTANCE_MODIFIER_NAME =
            ResourceLocation.fromNamespaceAndPath(
                    HotBath.MOD_ID, "peony_bath_knockback_resistance_modifier");

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

        HealthRegenHandler.regenHealth(0.25F, 2, player);

        if (result.stayedTicks() >= 15 * TICK_NUMBER) {
            applyAttributeModifier(
                    player,
                    Attributes.KNOCKBACK_RESISTANCE,
                    0.05,
                    KNOCKBACK_RESISTANCE_MODIFIER_NAME,
                    true,
                    AttributeModifier.Operation.ADD_VALUE);

            applyAttributeModifier(
                    player,
                    Attributes.ATTACK_SPEED,
                    0.10,
                    ATTACK_SPEED_MODIFIER_NAME,
                    true,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            EffectRemovalHandler.removeNegativeEffects(player);
            EffectRemovalHandler.removeBadOmen(player);
        }

        if (result.totalEnterCount() >= LUCK_THRESHOLD) {
            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.LUCK,
                            45 * TICK_NUMBER,
                            0,
                            false,
                            false,
                            true));
        }

        // Reset exit timer using memory cache instead of PersistentData
        PeonyBathEvents.resetExitTimer(player.getUUID());
    }

    public static void applyAttributeModifier(
            ServerPlayer player,
            Holder<Attribute> attribute,
            double value,
            ResourceLocation modifierName,
            boolean add,
            AttributeModifier.Operation operation) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);

        if (attributeInstance != null) {
            if (add) {
                AttributeModifier modifier = new AttributeModifier(modifierName, value, operation);
                if (!attributeInstance.hasModifier(modifierName)) {
                    attributeInstance.addTransientModifier(modifier);
                }
            } else {
                attributeInstance.removeModifier(modifierName);
            }
        }
    }
}
