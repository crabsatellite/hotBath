package com.crabmod.hotbath.events.enter_fluid_events;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.fluid_blocks.PeonyBathBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

@SuppressWarnings("deprecation")
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class PeonyBathEvents {
    private static final int TICK_NUMBER = 20;

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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        CompoundTag data = player.getPersistentData();

        int exitedTime = data.getInt(PeonyBathBlock.PeonyExitedTimeKey) + 1;
        data.putInt(PeonyBathBlock.PeonyExitedTimeKey, exitedTime);

        if (exitedTime == 15 * TICK_NUMBER) {
            PeonyBathBlock.applyAttributeModifier(
                serverPlayer,
                Attributes.ATTACK_SPEED,
                0.10,
                PeonyBathBlock.ATTACK_SPEED_MODIFIER_NAME,
                false,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        if (exitedTime == 30 * TICK_NUMBER) {
            PeonyBathBlock.applyAttributeModifier(
                serverPlayer,
                Attributes.KNOCKBACK_RESISTANCE,
                0.05,
                PeonyBathBlock.KNOCKBACK_RESISTANCE_MODIFIER_NAME,
                false,
                AttributeModifier.Operation.ADDITION);
        }
    }
}










