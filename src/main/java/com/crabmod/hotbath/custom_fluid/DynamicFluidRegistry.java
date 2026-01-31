package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.registers.CustomFluidBlocksRegister;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for dynamic custom fluid instances.
 * These fluids use DynamicFluidType which supports per-block coloring.
 * Uses DynamicCustomFluid which propagates BlockEntity data when spreading.
 */
public class DynamicFluidRegistry {
    
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, HotBath.MOD_ID);

    /**
     * Still version of dynamic custom fluid.
     */
    public static final RegistryObject<FlowingFluid> DYNAMIC_FLUID_STILL =
            FLUIDS.register("dynamic_custom_fluid",
                    () -> new DynamicCustomFluid.Source(createFluidProperties()));

    /**
     * Flowing version of dynamic custom fluid.
     */
    public static final RegistryObject<FlowingFluid> DYNAMIC_FLUID_FLOWING =
            FLUIDS.register("dynamic_custom_fluid_flowing",
                    () -> new DynamicCustomFluid.Flowing(createFluidProperties()));

    private static ForgeFlowingFluid.Properties createFluidProperties() {
        return new ForgeFlowingFluid.Properties(
                DynamicFluidTypeRegistry.DYNAMIC_CUSTOM_FLUID_TYPE,
                DYNAMIC_FLUID_STILL,
                DYNAMIC_FLUID_FLOWING
        )
        .slopeFindDistance(4)
        .levelDecreasePerBlock(1)
        .block(CustomFluidBlocksRegister.CUSTOM_FLUID_BLOCK)
        .bucket(() -> Items.AIR); // No bucket item - we use custom bucket
    }

    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
    }
}
