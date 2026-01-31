package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for dynamic custom fluid types.
 * These fluid types support per-block coloring based on BlockEntity data.
 */
public class DynamicFluidTypeRegistry {
    
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, HotBath.MOD_ID);

    /**
     * A dynamic fluid type that reads color from BlockEntity.
     * Used by DynamicCustomFluidBlock to display different colors per-block.
     * Note: PathType parameters are not available in Forge 1.20.1
     */
    public static final RegistryObject<DynamicFluidType> DYNAMIC_CUSTOM_FLUID_TYPE =
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
                                    .canConvertToSource(false)  // Don't create infinite source
                                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                                    .canHydrate(true)
                    ));

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
    }
}
