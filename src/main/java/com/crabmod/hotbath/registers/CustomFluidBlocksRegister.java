package com.crabmod.hotbath.registers;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.custom_fluid.DynamicCustomFluidBlock;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom fluid blocks that use BlockEntity to store fluid data.
 */
public class CustomFluidBlocksRegister {
    
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, HotBath.MOD_ID);

    /**
     * A universal custom fluid block that uses BlockEntity to store the fluid ID.
     * This allows placing fluids from data packs without needing to register each one.
     * Uses DynamicFluidRegistry.DYNAMIC_FLUID_STILL which supports per-block coloring.
     */
    public static final RegistryObject<DynamicCustomFluidBlock> CUSTOM_FLUID_BLOCK =
            BLOCKS.register("custom_fluid_block",
                    () -> new DynamicCustomFluidBlock(
                            DynamicFluidRegistry.DYNAMIC_FLUID_STILL,
                            Block.Properties.copy(Blocks.WATER)
                                    .noCollission()
                                    .strength(1000.0F)
                                    .isValidSpawn((state, level, pos, entityType) -> false)
                                    .noOcclusion()
                                    // Non-zero base lightLevel so findBlockLightSources() detects these blocks.
                                    // The actual per-fluid luminosity comes from DynamicCustomFluidBlock.getLightEmission()
                                    // which the Forge-patched light engine calls with position context.
                                    .lightLevel(state -> 2)
                    ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
