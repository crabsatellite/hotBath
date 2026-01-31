package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.custom_fluid.DynamicCustomFluidBlock;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for custom fluid blocks that use BlockEntity to store fluid data.
 */
public class CustomFluidBlocksRegister {
    
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, HotBath.MOD_ID);

    /**
     * A universal custom fluid block that uses BlockEntity to store the fluid ID.
     * This allows placing fluids from data packs without needing to register each one.
     * Uses DynamicFluidRegistry.DYNAMIC_FLUID_STILL which supports per-block coloring.
     */
    public static final DeferredHolder<Block, DynamicCustomFluidBlock> CUSTOM_FLUID_BLOCK =
            BLOCKS.register("custom_fluid_block",
                    () -> new DynamicCustomFluidBlock(
                            DynamicFluidRegistry.DYNAMIC_FLUID_STILL,
                            Block.Properties.ofFullCopy(Blocks.WATER)
                                    .noCollission()
                                    .strength(1000.0F)
                                    .isValidSpawn((state, level, pos, entityType) -> false)
                                    .noOcclusion()
                    ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
