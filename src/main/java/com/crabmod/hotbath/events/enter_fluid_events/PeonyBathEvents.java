package com.crabmod.hotbath.events.enter_fluid_events;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.util.CustomFluidHandler;
import com.crabmod.hotbath.util.EffectRemovalHandler;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import static com.crabmod.hotbath.util.HealthRegenHandler.regenHealth;

@SuppressWarnings("deprecation")
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class PeonyBathEvents {
    static final String PEONY_BATH_ENTERED_NUMBER = "PeonyBathEnteredNumber";
    static final String PEONY_BATH_STAYED_TIME = "PeonyBathStayedTime";
    static final String HAS_ENTERED_PEONY_BATH = "HasEnteredPeonyBath";
    static final String PEONY_BATH_EXITED_TIME = "PeonyBathExitedTime";
    private static final int TICK_NUMBER = 20;
    private static final int PEONY_BATH_ENTERED_COUNT_TRIGGER_NUMBER = 100;
    private static final int PEONY_BATH_STAYED_EFFECT_TRIGGER_TIME_SECONDS = 5;
    private static final int KNOCKBACK_RESISTANCE_DURATION = 30 * TICK_NUMBER;
    private static final int ATTACK_SPEED_DURATION = 15 * TICK_NUMBER;
    private static final int LUCK_DURATION = 45 * TICK_NUMBER;
    private static final int LUCK_THRESHOLD = 50;
    @SuppressWarnings("removal")
    private static final ResourceLocation ATTACK_SPEED_MODIFIER_NAME =
            new ResourceLocation(HotBath.MOD_ID, "peony_bath_attack_speed_modifier");
    @SuppressWarnings("removal")
    private static final ResourceLocation KNOCKBACK_RESISTANCE_MODIFIER_NAME =
            new ResourceLocation(
                    HotBath.MOD_ID, "peony_bath_knockback_resistance_modifier");

    @SubscribeEvent
    public static void enterPeonyBathEvents(LivingEvent.LivingTickEvent event) {
        enterFluidEvents(
                event,
                PEONY_BATH_ENTERED_COUNT_TRIGGER_NUMBER,
                PEONY_BATH_STAYED_EFFECT_TRIGGER_TIME_SECONDS,
                PEONY_BATH_ENTERED_NUMBER,
                PEONY_BATH_STAYED_TIME,
                HAS_ENTERED_PEONY_BATH);
    }

    public static void enterFluidEvents(
            LivingEvent.LivingTickEvent event,
            int enteredCountTriggerNumber,
            int stayedEffectTriggerTime,
            String enteredNumberInPeonyBath,
            String peonyBathStayedTime,
            String hasEnteredPeonyBath) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag playerData = player.getPersistentData();
            boolean isInPeonyBath = CustomFluidHandler.isPlayerInPeonyBathBlock(player);

            if (isInPeonyBath && player.isAlive()) {
                if (!playerData.getBoolean(hasEnteredPeonyBath)) {
                    int enteredCount = playerData.getInt(enteredNumberInPeonyBath) + 1;
                    playerData.putInt(enteredNumberInPeonyBath, enteredCount);
                    playerData.putBoolean(hasEnteredPeonyBath, true);
                }

                int hotBathTime = playerData.getInt(peonyBathStayedTime) + 1;
                playerData.putInt(peonyBathStayedTime, hotBathTime);

                regenHealth(0.25F, 2, player);

                if (playerData.getInt(peonyBathStayedTime) >= 15 * TICK_NUMBER) {
                    applyAttributeModifier(
                            player,
                            Attributes.KNOCKBACK_RESISTANCE,
                            0.05,
                            KNOCKBACK_RESISTANCE_MODIFIER_NAME,
                            true,
                            AttributeModifier.Operation.ADDITION);
                    applyAttributeModifier(
                            player,
                            Attributes.ATTACK_SPEED,
                            0.10,
                            ATTACK_SPEED_MODIFIER_NAME,
                            true,
                            AttributeModifier.Operation.MULTIPLY_TOTAL);
                    EffectRemovalHandler.removeNegativeEffects(player);
                    EffectRemovalHandler.removeBadOmen(player);
                }

                if (playerData.getInt(enteredNumberInPeonyBath) >= LUCK_THRESHOLD) {
                    player.addEffect(
                            new MobEffectInstance(MobEffects.LUCK, LUCK_DURATION, 0, false, false, true));
                }

                playerData.putInt(PEONY_BATH_EXITED_TIME, 0);
            } else {
                if (playerData.getBoolean(hasEnteredPeonyBath)) {
                    playerData.putBoolean(hasEnteredPeonyBath, false);
                }

                playerData.putInt(PEONY_BATH_EXITED_TIME, playerData.getInt(PEONY_BATH_EXITED_TIME) + 1);

                if (playerData.getInt(PEONY_BATH_EXITED_TIME) >= 15 * TICK_NUMBER) {
                    // Remove attack speed modifier
                    applyAttributeModifier(
                            player,
                            Attributes.ATTACK_SPEED,
                            0.10,
                            ATTACK_SPEED_MODIFIER_NAME,
                            false,
                            AttributeModifier.Operation.MULTIPLY_TOTAL);
                }

                if (playerData.getInt(PEONY_BATH_EXITED_TIME) >= 30 * TICK_NUMBER) {
                    // Remove knockback resistance modifier
                    applyAttributeModifier(
                            player,
                            Attributes.KNOCKBACK_RESISTANCE,
                            0.05,
                            KNOCKBACK_RESISTANCE_MODIFIER_NAME,
                            false,
                            AttributeModifier.Operation.ADDITION);
                }

                playerData.putInt(peonyBathStayedTime, 0);
            }
        }
    }

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

    private static void applyAttributeModifier(
            ServerPlayer player,
            Attribute attribute,
            double value,
            ResourceLocation modifierName,
            boolean add,
            AttributeModifier.Operation operation) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);
        java.util.UUID uuid = java.util.UUID.nameUUIDFromBytes(modifierName.toString().getBytes());

        if (attributeInstance != null) {
            if (add) {
                AttributeModifier modifier = new AttributeModifier(uuid, modifierName.toString(), value, operation);
                if (!attributeInstance.hasModifier(modifier)) {
                    attributeInstance.addTransientModifier(modifier);
                }
            } else {
                attributeInstance.removeModifier(uuid);
            }
        }
    }
}










