package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for block entities in HotBath mod.
 */
public class BlockEntityRegister {
    
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, HotBath.MOD_ID);

    /**
     * Block entity for custom fluid blocks that stores the fluid ID from data packs.
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CustomFluidBlockEntity>> CUSTOM_FLUID_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("custom_fluid_block_entity",
                    () -> BlockEntityType.Builder.of(
                            CustomFluidBlockEntity::new,
                            CustomFluidBlocksRegister.CUSTOM_FLUID_BLOCK.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
