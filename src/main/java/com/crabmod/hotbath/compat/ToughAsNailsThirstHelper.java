package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import net.minecraft.world.entity.player.Player;
import toughasnails.api.thirst.ThirstHelper;
import toughasnails.api.thirst.IThirst;

public class ToughAsNailsThirstHelper {
    
    /** Default thirst restoration for built-in bath water bottles */
    private static final int DEFAULT_THIRST = 4;
    private static final float DEFAULT_HYDRATION = 0.6F;
    
    /**
     * Restores thirst for built-in bath water bottles.
     * Uses default values: 4 thirst (2 shanks) and 0.6 hydration.
     */
    public static void restoreThirst(Player player) {
        restoreThirst(player, DEFAULT_THIRST, DEFAULT_HYDRATION);
    }
    
    /**
     * Restores thirst for custom fluid bottles.
     * Uses the thirst value from the fluid definition.
     */
    public static void restoreThirst(Player player, CustomFluidDefinition definition) {
        if (definition != null && definition.thirst() > 0) {
            restoreThirst(player, definition.thirst(), DEFAULT_HYDRATION);
        }
    }
    
    /**
     * Restores thirst with specified values.
     */
    public static void restoreThirst(Player player, int thirst, float hydration) {
        IThirst thirstData = ThirstHelper.getThirst(player);
        thirstData.addThirst(thirst);
        thirstData.addHydration(hydration);
    }
}










