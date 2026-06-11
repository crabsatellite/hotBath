package com.crabmod.hotbath.api;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.dirtiness.DirtinessHandler;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * Stable public integration surface for external bath containers.
 */
public final class HotBathApi {

    public static final TagKey<Fluid> BATH_FLUIDS = fluidTag("bath_fluids");
    public static final TagKey<Fluid> CLEANSING_FLUIDS = fluidTag("cleansing_fluids");

    private HotBathApi() {
    }

    /**
     * Checks whether a fluid should be treated as a Hot Bath bath fluid.
     */
    public static boolean isBathFluid(Fluid fluid) {
        return isTaggedOrKnownHotBathFluid(fluid, BATH_FLUIDS);
    }

    /**
     * Checks whether a fluid may trigger Hot Bath dirtiness cleaning.
     */
    public static boolean isCleansingFluid(Fluid fluid) {
        return isTaggedOrKnownHotBathFluid(fluid, CLEANSING_FLUIDS);
    }

    /**
     * Applies one server tick of gradual dirtiness cleaning from a pre-validated bath.
     */
    public static boolean applyDirtinessCleaning(ServerPlayer player, boolean isMoving) {
        return DirtinessHandler.applyExternalBathing(player, isMoving);
    }

    /**
     * Applies one server tick of dirtiness cleaning if the supplied fluid is cleansing.
     */
    public static boolean applyDirtinessCleaning(ServerPlayer player, Fluid fluid, boolean isMoving) {
        return isCleansingFluid(fluid) && applyDirtinessCleaning(player, isMoving);
    }

    private static boolean isTaggedOrKnownHotBathFluid(Fluid fluid, TagKey<Fluid> tag) {
        return fluid != null && (fluid.defaultFluidState().is(tag) || HotbathFluidHelper.isHotbathFluid(fluid));
    }

    private static TagKey<Fluid> fluidTag(String name) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(HotBath.MOD_ID, name));
    }
}
