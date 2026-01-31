package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for BaseCoralWallFanBlock to support hotBath fluid waterlogging.
 * 
 * <p>BaseCoralWallFanBlock has its own getStateForPlacement and updateShape
 * that override the parent class implementations. This mixin handles both.</p>
 */
@Mixin(value = BaseCoralWallFanBlock.class, priority = 500)
public abstract class BaseCoralWallFanBlockMixin {

    /**
     * When placing coral wall fan in hotBath fluid, store the fluid type.
     */
    @Inject(method = "getStateForPlacement", at = @At("RETURN"))
    private void hotbath$getStateForPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState resultState = cir.getReturnValue();
        if (resultState != null && resultState.hasProperty(BlockStateProperties.WATERLOGGED)
                && resultState.getValue(BlockStateProperties.WATERLOGGED)) {
            FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
            Fluid fluidType = fluidState.getType();
            
            // Only store hotBath fluids specifically
            if (HotbathFluidHelper.isHotbathFluid(fluidType)) {
                Fluid sourceFluid = HotbathFluidHelper.getSourceFluid(fluidType);
                HotbathWaterloggingHelper.storeFluidType(context.getLevel(), context.getClickedPos(), sourceFluid);
            }
        }
    }

    /**
     * Schedule hotBath fluid tick instead of vanilla water.
     */
    @Inject(method = "updateShape", at = @At("HEAD"))
    private void hotbath$updateShape(BlockState state, Direction facing, BlockState facingState, 
                                      LevelAccessor level, BlockPos currentPos, BlockPos facingPos,
                                      CallbackInfoReturnable<BlockState> cir) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, currentPos);
            // Only schedule tick for hotBath fluids specifically
            if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                level.scheduleTick(currentPos, storedFluid, storedFluid.getTickDelay(level));
            }
        }
    }
}
