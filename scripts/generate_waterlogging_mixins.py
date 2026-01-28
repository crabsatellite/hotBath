#!/usr/bin/env python3
"""
Script to generate Mixin classes for all blocks that implement SimpleWaterloggedBlock.
This enables hotBath fluids to be placed in any waterloggable block.

For NeoForge 1.21.1, we use interface mixin since it's supported.
"""

import os
from pathlib import Path

# All Minecraft block classes that implement SimpleWaterloggedBlock in 1.21
# For 1.21, we can use a single interface mixin approach since NeoForge supports it
# This script is provided for reference/documentation

WATERLOGGED_BLOCKS_1_21 = [
    # Basic blocks with WATERLOGGED property
    "StairBlock",
    "SlabBlock",
    "TrapDoorBlock",
    "FenceBlock",
    "FenceGateBlock",
    "WallBlock",
    "LadderBlock",
    "ChainBlock",
    "LanternBlock",
    "CampfireBlock",
    "SignBlock",
    "HangingSignBlock",  # 1.20.2+
    "ConduitBlock",
    "SeaPickleBlock",
    "ScaffoldingBlock",
    "LightningRodBlock",
    "PointedDripstoneBlock",
    "AmethystClusterBlock",
    "BigDripleafBlock",
    "BigDripleafStemBlock",
    "SmallDripleafBlock",
    "HangingRootsBlock",
    "MangrovePropaguleBlock",
    "MangroveRootsBlock",
    "SculkSensorBlock",
    "SculkShriekerBlock",
    "SculkVeinBlock",
    "GlowLichenBlock",
    "CandleBlock",
    "DecoratedPotBlock",
    "ChestBlock",
    "EnderChestBlock",
    "LeavesBlock",
    "LightBlock",
    "BarrierBlock",
    "HeavyCoreBlock",  # 1.21+
    # Parent classes (these cover many blocks)
    # "BaseCoralPlantTypeBlock",
    # "BaseRailBlock",
    # "CrossCollisionBlock",
    # "WaterloggedTransparentBlock",
]

INTERFACE_MIXIN_TEMPLATE = '''package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to extend SimpleWaterloggedBlock to accept any fluid in the #minecraft:water tag.
 * This allows hotBath fluids to waterlog blocks like stairs, slabs, fences, etc.
 * 
 * NeoForge 1.21 supports interface mixin with @Inject, so we can use a single mixin
 * to cover ALL blocks implementing SimpleWaterloggedBlock.
 * 
 * Covered blocks: {block_list}
 */
@Mixin(SimpleWaterloggedBlock.class)
public interface SimpleWaterloggedBlockMixin {{

    /**
     * Modify canPlaceLiquid to accept any fluid in the water tag, not just Fluids.WATER
     */
    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$canPlaceLiquid(@Nullable LivingEntity entity, BlockGetter level, BlockPos pos,
                                         BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {{
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {{
            boolean isWaterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
            boolean isWaterTagFluid = fluid.defaultFluidState().is(FluidTags.WATER);
            
            if (!isWaterlogged && isWaterTagFluid) {{
                cir.setReturnValue(true);
            }}
        }}
    }}

    /**
     * Modify placeLiquid to handle any fluid in the water tag
     */
    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$placeLiquid(LevelAccessor level, BlockPos pos, BlockState state,
                                      FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {{
        if (fluidState.is(FluidTags.WATER)) {{
            if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                    && !state.getValue(BlockStateProperties.WATERLOGGED)) {{
                if (!level.isClientSide()) {{
                    HotbathWaterloggingHelper.storeFluidType(level, pos, fluidState.getType());
                    
                    level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                    level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
                }}
                cir.setReturnValue(true);
            }}
        }}
    }}

    /**
     * Modify pickupBlock to return the correct bucket for hotBath fluids
     */
    @Inject(method = "pickupBlock", at = @At("HEAD"), cancellable = true)
    default void hotbath$pickupBlock(@Nullable LivingEntity entity, LevelAccessor level, BlockPos pos,
                                      BlockState state, CallbackInfoReturnable<ItemStack> cir) {{
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                && state.getValue(BlockStateProperties.WATERLOGGED)) {{
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
            
            if (storedFluid != null && storedFluid != Fluids.WATER && storedFluid != Fluids.EMPTY) {{
                level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
                HotbathWaterloggingHelper.removeFluidType(level, pos);
                
                ItemStack bucket = new ItemStack(storedFluid.getBucket());
                if (!bucket.isEmpty()) {{
                    cir.setReturnValue(bucket);
                }}
            }}
        }}
    }}

    /**
     * Modify getFluidState to return the correct fluid for hotBath fluids
     */
    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    default void hotbath$getFluidState(BlockState state, CallbackInfoReturnable<FluidState> cir) {{
        // This method doesn't have position info, so we can't use our storage here
        // The fluid state will be retrieved from the block's actual position in other ways
    }}
}}
'''


def main():
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    mixin_dir = project_root / "src" / "main" / "java" / "com" / "crabmod" / "hotbath" / "mixin"
    
    print("For NeoForge 1.21, a single interface mixin is sufficient.")
    print(f"The mixin covers {len(WATERLOGGED_BLOCKS_1_21)} block types automatically.")
    print()
    print("Blocks covered by SimpleWaterloggedBlockMixin:")
    for block in WATERLOGGED_BLOCKS_1_21:
        print(f"  - {block}")
    
    # Generate the interface mixin content with block list in comment
    block_list = ", ".join(WATERLOGGED_BLOCKS_1_21[:10]) + "..."
    content = INTERFACE_MIXIN_TEMPLATE.format(block_list=block_list)
    
    output_file = mixin_dir / "SimpleWaterloggedBlockMixin.java"
    
    if output_file.exists():
        print(f"\n✅ {output_file.name} already exists")
        print("   No changes needed for 1.21 - interface mixin covers all blocks.")
    else:
        mixin_dir.mkdir(parents=True, exist_ok=True)
        output_file.write_text(content, encoding='utf-8')
        print(f"\n✅ Generated: {output_file}")


if __name__ == "__main__":
    main()
