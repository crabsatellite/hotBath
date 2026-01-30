package com.crabmod.hotbath.mixin.sodium;

import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin for Sodium's DefaultFluidRenderer to fix hotBath fluid connection in waterlogged blocks.
 * 
 * <p>The fluidHeight method in DefaultFluidRenderer checks if adjacent blocks have the same fluid
 * by using BlockState.getFluidState(). For hotBath waterlogged blocks, this returns vanilla water
 * instead of the hotBath fluid, causing fluids to not connect properly.</p>
 * 
 * <p>This mixin intercepts the fluid type comparison and checks our waterlogging cache to see if
 * the block actually contains a hotBath fluid.</p>
 */
@Mixin(value = DefaultFluidRenderer.class, remap = false)
public class DefaultFluidRendererMixin {

    /**
     * Redirect the BlockState.getFluidState() call in fluidHeight to return the correct
     * hotBath fluid state for waterlogged blocks.
     */
    @Redirect(
        method = "fluidHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getFluidState()Lnet/minecraft/world/level/material/FluidState;",
            remap = true
        ),
        remap = false
    )
    private FluidState hotbath$redirectGetFluidStateInFluidHeight(BlockState blockState,
            BlockAndTintGetter world, Fluid fluid, BlockPos blockPos, net.minecraft.core.Direction direction) {
        FluidState originalFluidState = blockState.getFluidState();
        
        // Check if this block is waterlogged with a hotBath fluid
        if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && blockState.getValue(BlockStateProperties.WATERLOGGED)) {
            
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(blockPos);
            
            // Only handle hotBath fluids specifically
            if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                // Return the hotBath fluid's source state
                return storedFluid.defaultFluidState();
            }
        }
        
        return originalFluidState;
    }

    /**
     * Also redirect the fluid.isSame check in fluidHeight to handle hotBath fluids.
     * This is the first isSame call that compares the current fluid with the adjacent fluid.
     */
    @Redirect(
        method = "fluidHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/Fluid;isSame(Lnet/minecraft/world/level/material/Fluid;)Z",
            ordinal = 0,
            remap = true
        ),
        remap = false
    )
    private boolean hotbath$redirectIsSameFluid(Fluid thisFluid, Fluid otherFluid,
            BlockAndTintGetter world, Fluid fluid, BlockPos blockPos, net.minecraft.core.Direction direction) {
        // Original check
        if (thisFluid.isSame(otherFluid)) {
            return true;
        }
        
        // Check if the block at blockPos is waterlogged with a hotBath fluid
        BlockState blockState = world.getBlockState(blockPos);
        if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) 
                && blockState.getValue(BlockStateProperties.WATERLOGGED)) {
            
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(blockPos);
            
            if (HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                // Check if the stored fluid matches
                return thisFluid.isSame(storedFluid);
            }
        }
        
        return false;
    }

    /**
     * Redirect the second isSame check in fluidHeight for the block above.
     */
    @Redirect(
        method = "fluidHeight(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/Fluid;isSame(Lnet/minecraft/world/level/material/Fluid;)Z",
            ordinal = 1,
            remap = true
        ),
        remap = false
    )
    private boolean hotbath$redirectIsSameFluidAbove(Fluid thisFluid, Fluid otherFluid,
            BlockAndTintGetter world, Fluid fluid, BlockPos blockPos, net.minecraft.core.Direction direction) {
        // Original check
        if (thisFluid.isSame(otherFluid)) {
            return true;
        }
        
        // Check if the block above is waterlogged with a hotBath fluid
        BlockPos posAbove = blockPos.above();
        BlockState stateAbove = world.getBlockState(posAbove);
        
        if (stateAbove.hasProperty(BlockStateProperties.WATERLOGGED) 
                && stateAbove.getValue(BlockStateProperties.WATERLOGGED)) {
            
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(posAbove);
            
            if (HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                return thisFluid.isSame(storedFluid);
            }
        }
        
        return false;
    }
}
