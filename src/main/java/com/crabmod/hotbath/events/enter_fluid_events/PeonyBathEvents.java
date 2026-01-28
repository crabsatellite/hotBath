package com.crabmod.hotbath.events.enter_fluid_events;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.fluid_blocks.PeonyBathBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = HotBath.MOD_ID)
public class PeonyBathEvents {
    private static final int TICK_NUMBER = 20;
    
    // Memory cache for peony bath exit timers - avoids NBT writes every tick
    private static final Map<UUID, Integer> EXITED_TIMERS = new ConcurrentHashMap<>();
    
    // Threshold values for attribute removal
    private static final int ATTACK_SPEED_REMOVAL_TICKS = 15 * TICK_NUMBER;
    private static final int KNOCKBACK_REMOVAL_TICKS = 30 * TICK_NUMBER;

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
        if (player.level().isClientSide()) return;

        UUID playerUUID = player.getUUID();
        int exitedTime = EXITED_TIMERS.getOrDefault(playerUUID, 0) + 1;
        EXITED_TIMERS.put(playerUUID, exitedTime);

        // Only process at exact threshold times to avoid repeated checks
        if (exitedTime == ATTACK_SPEED_REMOVAL_TICKS) {
            PeonyBathBlock.applyAttributeModifier(
                    serverPlayer,
                    Attributes.ATTACK_SPEED,
                    0.10,
                    PeonyBathBlock.ATTACK_SPEED_MODIFIER_NAME,
                    false,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        } else if (exitedTime == KNOCKBACK_REMOVAL_TICKS) {
            PeonyBathBlock.applyAttributeModifier(
                    serverPlayer,
                    Attributes.KNOCKBACK_RESISTANCE,
                    0.05,
                    PeonyBathBlock.KNOCKBACK_RESISTANCE_MODIFIER_NAME,
                    false,
                    AttributeModifier.Operation.ADD_VALUE);
            
            // Once both modifiers are removed, we can stop tracking
            // But keep the timer high so we don't re-trigger
        }
    }
    
    /**
     * Reset the exit timer when player enters peony bath.
     * Called from PeonyBathBlock.entityInside()
     */
    public static void resetExitTimer(UUID playerUUID) {
        EXITED_TIMERS.put(playerUUID, 0);
    }
    
    /**
     * Clean up player data when they log out.
     */
    public static void cleanup(UUID playerUUID) {
        EXITED_TIMERS.remove(playerUUID);
    }
}
