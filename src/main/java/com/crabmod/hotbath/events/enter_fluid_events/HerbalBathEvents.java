package com.crabmod.hotbath.events.enter_fluid_events;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.util.CustomFluidHandler;
import com.crabmod.hotbath.util.EffectRemovalHandler;
import com.crabmod.hotbath.util.HealthRegenHandler;
import com.crabmod.hotbath.util.ResistanceBoostHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HotBath.MOD_ID)
public class HerbalBathEvents {
  private static final int TICK_NUMBER = 20;
  static final String HERBAL_BATH_ENTERED_NUMBER = "HerbalBathEnteredNumber";
  static final String HERBAL_BATH_STAYED_TIME = "HerbalBathStayedTime";
  static final String HAS_ENTERED_HERBAL_BATH = "HasEnteredHerbalBath";
  static final String HERBAL_BATH_ADVANCEMENT_ID = "hotbath:chronic_invalid";
  private static final int HERBAL_BATH_ENTERED_COUNT_TRIGGER_NUMBER = 100;
  private static final int HERBAL_BATH_STAYED_EFFECT_TRIGGER_TIME_SECONDS = 5;

  // enter hot water block event
  @SubscribeEvent
  public static void enterHerbalBathBlockEvent(LivingEvent.LivingTickEvent event) {
    enterFluidEvents(
        event,
        HERBAL_BATH_ENTERED_COUNT_TRIGGER_NUMBER,
        HERBAL_BATH_STAYED_EFFECT_TRIGGER_TIME_SECONDS,
        HERBAL_BATH_ENTERED_NUMBER,
        HERBAL_BATH_STAYED_TIME,
        HAS_ENTERED_HERBAL_BATH,
        HERBAL_BATH_ADVANCEMENT_ID);
  }

  public static void enterFluidEvents(
      LivingEvent.LivingTickEvent event,
      int enteredCountTriggerNumber,
      int stayedEffectTriggerTime,
      String enteredNumberInHerbalBath,
      String herbalBathStayedTime,
      String hasEnteredHerbalBath,
      String herbalBathAdvancementId) {
    Entity entity = event.getEntity();
    Level level = entity.level;

    if (!(entity instanceof Player)) {
      if (entity instanceof Zombie || entity instanceof Skeleton) {
        boolean isInHerbalBath = CustomFluidHandler.isEntityInHerbalBathBlock(entity);

        if (isInHerbalBath) {
          int damageIntervalTicks = 20;
          float damagePerSecond = 0.5F;

          if (entity.tickCount % damageIntervalTicks == 0) {
            entity.hurt(level.damageSources().magic(), damagePerSecond);
          }
        }
      }
      return;
    }

    boolean isInHerbalBath =
        CustomFluidHandler.isPlayerInHerbalBathBlock((Player) event.getEntity());

    if (event.getEntity() instanceof ServerPlayer player) {
      CompoundTag playerData = player.getPersistentData();

      if (isInHerbalBath) {
        HotWaterEvents.handleAdvancement(
            enteredCountTriggerNumber,
            enteredNumberInHerbalBath,
            herbalBathStayedTime,
            hasEnteredHerbalBath,
            herbalBathAdvancementId,
            player,
            playerData);

        HealthRegenHandler.regenHealth(0.25F, 2, player);

        if (playerData.getInt(herbalBathStayedTime) >= stayedEffectTriggerTime * TICK_NUMBER) {
          ResistanceBoostHandler.applyResistanceBoost(10, player);
        }

        if (playerData.getInt(herbalBathStayedTime) >= 15 * TICK_NUMBER) {
          // remove negative effects
          EffectRemovalHandler.removeNegativeEffects(player);
        }
      } else {
        playerData.putInt(herbalBathStayedTime, 0);
        playerData.putBoolean(hasEnteredHerbalBath, false);
      }
    }
  }
}