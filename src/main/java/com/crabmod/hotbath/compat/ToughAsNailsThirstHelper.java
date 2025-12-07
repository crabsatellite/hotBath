package com.crabmod.hotbath.compat;

import net.minecraft.world.entity.player.Player;
import toughasnails.api.thirst.ThirstHelper;
import toughasnails.api.thirst.IThirst;

public class ToughAsNailsThirstHelper {
    public static void restoreThirst(Player player) {
        IThirst thirst = ThirstHelper.getThirst(player);
        // Restore 4 thirst (2 shanks) and 0.6 hydration
        thirst.addThirst(4);
        thirst.addHydration(0.6F);
    }
}










