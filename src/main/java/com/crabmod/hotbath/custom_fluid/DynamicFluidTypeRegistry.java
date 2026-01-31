package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registry for dynamic custom fluid types.
 * These fluid types support per-block coloring based on BlockEntity data.
 */
public class DynamicFluidTypeRegistry {
    
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, HotBath.MOD_ID);

    /**
     * A dynamic fluid type that reads color from BlockEntity.
     * Used by DynamicCustomFluidBlock to display different colors per-block.
     * Supports boats, infinite water source, and proper pathfinding like vanilla water.
     */
    public static final DeferredHolder<FluidType, DynamicFluidType> DYNAMIC_CUSTOM_FLUID_TYPE =
            FLUID_TYPES.register("dynamic_custom_fluid",
                    () -> new DynamicFluidType(
                            FluidType.Properties.create()
                                    .lightLevel(2)
                                    .density(1000)
                                    .viscosity(1000)
                                    .canExtinguish(true)
                                    .supportsBoating(true)
                                    .fallDistanceModifier(0.0F)
                                    .canDrown(true)
                                    .canSwim(true)
                                    .canConvertToSource(true)  // Enable infinite source like vanilla water
                                    .pathType(PathType.WATER)
                                    .adjacentPathType(PathType.WATER_BORDER)
                                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                                    .canHydrate(true)
                    ));

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
    }
}
