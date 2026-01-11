package com.crabmod.hotbath.events.enter_fluid_events;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.fluid_blocks.PeonyBathBlock;
import com.crabmod.hotbath.util.CustomFluidHandler;
import com.crabmod.hotbath.util.EffectRemovalHandler;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static com.crabmod.hotbath.util.HealthRegenHandler.regenHealth;

@EventBusSubscriber(modid = HotBath.MOD_ID)
public class PeonyBathEvents {
    private static final int TICK_NUMBER = 20;
    private static final ResourceLocation ATTACK_SPEED_MODIFIER_NAME =
            ResourceLocation.fromNamespaceAndPath(HotBath.MOD_ID, "peony_bath_attack_speed_modifier");
    private static final ResourceLocation KNOCKBACK_RESISTANCE_MODIFIER_NAME =
            ResourceLocation.fromNamespaceAndPath(
                    HotBath.MOD_ID, "peony_bath_knockback_resistance_modifier");

    // Method to reset invalid attributes
    private static void resetInvalidAttributes(ServerPlayer player) {
        // Get the attack speed attribute instance
        AttributeInstance attackSpeedAttribute = player.getAttribute(Attributes.ATTACK_SPEED);

        // Check if the attack speed is negative, if so, reset it to the default value
        if (attackSpeedAttribute != null) {
            double currentAttackSpeed = attackSpeedAttribute.getBaseValue();
            if (currentAttackSpeed < 0) {
                attackSpeedAttribute.setBaseValue(4.0);
            }
        }
    }

    // Handle player login event and reset invalid attributes
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resetInvalidAttributes(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        CompoundTag data = player.getPersistentData();

        int exitedTime = data.getInt(PeonyBathBlock.PeonyExitedTimeKey) + 1;
        data.putInt(PeonyBathBlock.PeonyExitedTimeKey, exitedTime);

        if (exitedTime == 15 * TICK_NUMBER) {
            PeonyBathBlock.applyAttributeModifier(
                    serverPlayer,
                    Attributes.ATTACK_SPEED,
                    0.10,
                    ATTACK_SPEED_MODIFIER_NAME,
                    false,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }

        if (exitedTime == 30 * TICK_NUMBER) {
            PeonyBathBlock.applyAttributeModifier(
                    serverPlayer,
                    Attributes.KNOCKBACK_RESISTANCE,
                    0.05,
                    KNOCKBACK_RESISTANCE_MODIFIER_NAME,
                    false,
                    AttributeModifier.Operation.ADD_VALUE);
        }
    }
}
