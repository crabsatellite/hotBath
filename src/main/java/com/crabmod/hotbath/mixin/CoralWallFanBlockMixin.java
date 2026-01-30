package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for CoralWallFanBlock to support hotBath fluid waterlogging.
 * 
 * <p>CoralWallFanBlock overrides updateShape with its own implementation that
 * hardcodes Fluids.WATER. This mixin injects to handle hotBath fluids.</p>
 * 
 * <p>Note: getStateForPlacement is NOT overridden in CoralWallFanBlock - it uses
 * the parent class BaseCoralWallFanBlock's implementation.</p>
 */
@Mixin(value = CoralWallFanBlock.class, priority = 500)
public abstract class CoralWallFanBlockMixin {

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
