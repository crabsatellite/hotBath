package com.crabmod.hotbath.compat;

import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

/**
 * Integration with Twilight Forest mod.
 * Provides:
 * - Frost effect removal when bathing in hot water
 * - Frost resistance buff after bathing
 * - Firefly particle effects around bath pools in Twilight Forest dimension
 * - Ice mob damage when in hot bath
 */
public class TwilightForestIntegration {
    private static final String TWILIGHT_FOREST_MOD_ID = "twilightforest";

    public static boolean isTwilightForestLoaded() {
        return ModList.get().isLoaded(TWILIGHT_FOREST_MOD_ID);
    }
    
    /**
     * Check if an entity is a Twilight Forest ice mob that should take damage in hot bath.
     * This includes: IceCrystal, StableIceCore, UnstableIceCore, SnowGuardian, and SnowQueen.
     * @param entity The entity to check
     * @return true if the entity is an ice mob
     */
    public static boolean isTwilightForestIceMob(Entity entity) {
        if (!isTwilightForestLoaded()) {
            return false;
        }
        return TwilightForestEventHandler.isIceMob(entity);
    }
}
