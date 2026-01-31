package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for block entities in HotBath mod.
 */
public class BlockEntityRegister {
    
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HotBath.MOD_ID);

    /**
     * Block entity for custom fluid blocks that stores the fluid ID from data packs.
     */
    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<CustomFluidBlockEntity>> CUSTOM_FLUID_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("custom_fluid_block_entity",
                    () -> BlockEntityType.Builder.of(
                            CustomFluidBlockEntity::new,
                            CustomFluidBlocksRegister.CUSTOM_FLUID_BLOCK.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
