package com.crabmod.hotbath.registers;

import net.minecraftforge.registries.ForgeRegistries;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.items.ThrownBathWater;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public class EntityRegister {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HotBath.MOD_ID);

    public static final RegistryObject<EntityType<ThrownBathWater>> THROWN_BATH_WATER =
            ENTITY_TYPES.register("thrown_bath_water",
                    () -> EntityType.Builder.<ThrownBathWater>of(ThrownBathWater::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("thrown_bath_water"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
