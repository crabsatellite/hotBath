package com.crabmod.hotbath.events.enter_fluid_events;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.util.CustomFluidHandler;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.Objects;

@EventBusSubscriber(modid = HotBath.MOD_ID)
public class HotWaterEvents {
    private static final int TICK_NUMBER = 20;
    static final String HOT_WATER_ENTERED_NUMBER = "HotWaterEnteredNumber";
    static final String HOT_WATER_STAYED_TIME = "HotWaterStayedTime";
    static final String HAS_ENTERED_HOT_WATER = "HasEnteredHotWater";
    static final String HOT_WATER_ADVANCEMENT_ID = "hotbath:foot_health";
    private static final int HOT_WATER_ENTERED_COUNT_TRIGGER_NUMBER = 100;
    private static final int HOT_WATER_STAYED_EFFECT_TRIGGER_TIME_SECONDS = 15;

    // enter hot water block event
    @SubscribeEvent
    public static void enterHotWaterEvents(LivingEvent.LivingTickEvent event) {
        enterFluidEvents(
                event,
                HOT_WATER_ENTERED_COUNT_TRIGGER_NUMBER,
                HOT_WATER_STAYED_EFFECT_TRIGGER_TIME_SECONDS,
                HOT_WATER_ENTERED_NUMBER,
                HOT_WATER_STAYED_TIME,
                HAS_ENTERED_HOT_WATER,
                HOT_WATER_ADVANCEMENT_ID);
    }

    public static void enterFluidEvents(
            LivingEvent.LivingTickEvent event,
            int enteredCountTriggerNumber,
            int stayedEffectTriggerTime,
            String hotWaterEnteredNumber,
            String hotWaterStayedTime,
            String hasEnteredHotWater,
            String hotWaterAdvancementId) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag playerData = player.getPersistentData();
            boolean isInHotWater = CustomFluidHandler.isPlayerInHotWaterBlock(player);

            if (isInHotWater && player.isAlive()) {
                if (!playerData.getBoolean(hasEnteredHotWater)) {
                    int enteredCount = playerData.getInt(hotWaterEnteredNumber) + 1;
                    playerData.putInt(hotWaterEnteredNumber, enteredCount);
                    playerData.putBoolean(hasEnteredHotWater, true);

                    if (enteredCount >= enteredCountTriggerNumber) {
                        Advancement advancement =
                                Objects.requireNonNull(player.getServer())
                                        .getAdvancements()
                                        .getAdvancement(Objects.requireNonNull(ResourceLocation.tryParse(hotWaterAdvancementId)));

                        if (advancement != null) {
                            player.getAdvancements().award(advancement, "code_triggered");
                            playerData.putInt(hotWaterEnteredNumber, 0);
                        }
                    }
                }

                int hotBathTime = playerData.getInt(hotWaterStayedTime) + 1;
                playerData.putInt(hotWaterStayedTime, hotBathTime);

                if (playerData.getInt(hotWaterStayedTime) >= stayedEffectTriggerTime * TICK_NUMBER) {
                    player.addEffect(
                            new MobEffectInstance(
                                    MobEffects.MOVEMENT_SPEED, TICK_NUMBER * 20, 0, false, false, true));
                }
            } else {
                playerData.putInt(hotWaterStayedTime, 0);
                playerData.putBoolean(hasEnteredHotWater, false);
            }
        }
    }
}










