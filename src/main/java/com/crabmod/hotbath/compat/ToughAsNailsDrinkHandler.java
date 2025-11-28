package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.items.BathWaterEffects;
import com.crabmod.hotbath.util.CustomFluidHandler;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Handler for ToughAsNails drinking integration.
 * Applies our bath water effects when players drink from our bath water sources.
 */
@EventBusSubscriber(modid = HotBath.MOD_ID)
public class ToughAsNailsDrinkHandler {

    /**
     * When a player finishes drinking any item while in a hot bath block,
     * apply the appropriate bath water temperature effects.
     * This covers ToughAsNails' hand drinking feature when used in our bath water.
     */
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        // Only process on server side
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // Only process for players
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // Only apply effects if ToughAsNails is loaded
        if (!ToughAsNailsIntegration.isToughAsNailsLoaded()) {
            return;
        }

        // Check which type of bath the player is in and apply corresponding effects
        // We only apply temperature effects, not the potion effects, since those are specific to our bottles
        if (CustomFluidHandler.isPlayerInHotWaterBlock(player)) {
            BathWaterEffects.applyTemperatureEffectsOnly(player);
        } else if (CustomFluidHandler.isPlayerInHoneyBathBlock(player)) {
            BathWaterEffects.applyTemperatureEffectsOnly(player);
        } else if (CustomFluidHandler.isPlayerInMilkBathBlock(player)) {
            BathWaterEffects.applyTemperatureEffectsOnly(player);
        } else if (CustomFluidHandler.isPlayerInHerbalBathBlock(player)) {
            BathWaterEffects.applyTemperatureEffectsOnly(player);
        } else if (CustomFluidHandler.isPlayerInPeonyBathBlock(player)) {
            BathWaterEffects.applyTemperatureEffectsOnly(player);
        } else if (CustomFluidHandler.isPlayerInRoseBathBlock(player)) {
            BathWaterEffects.applyTemperatureEffectsOnly(player);
        }
    }
}
