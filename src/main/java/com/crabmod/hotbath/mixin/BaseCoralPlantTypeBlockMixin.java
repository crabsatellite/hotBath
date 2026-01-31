package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for BaseCoralPlantTypeBlock to support hotBath fluid waterlogging.
 * 
 * <p>BaseCoralPlantTypeBlock implements SimpleWaterloggedBlock and is the base
 * for all coral plant blocks. This mixin handles:
 * - getStateForPlacement: store hotBath fluid type when placing in hotBath
 * - updateShape: schedule hotBath fluid tick instead of vanilla water
 * - scanForWater: treat hotBath fluids as non-water when coral should die (configurable)</p>
 */
@Mixin(value = BaseCoralPlantTypeBlock.class, priority = 500)
public abstract class BaseCoralPlantTypeBlockMixin {

    /**
     * When placing coral in hotBath fluid, store the fluid type.
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
                HotbathWaterloggingHelper.storeFluidTypeWithCustomId(context.getLevel(), context.getClickedPos(), sourceFluid);
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
    
    /**
     * Modify scanForWater to treat hotBath fluids as non-water when coral should die.
     * When coralDiesInHotbathFluid config is true (default), coral in hotBath will die.
     * When false, coral will survive in hotBath fluids as if they were water.
     */
    @Inject(method = "scanForWater", at = @At("HEAD"), cancellable = true)
    private static void hotbath$scanForWater(BlockState state, BlockGetter level, BlockPos pos, 
                                              CallbackInfoReturnable<Boolean> cir) {
        // If coral should die in hotbath (default behavior), check if we're in hotbath fluid
        if (HotBathConfig.doesCoralDieInHotbath()) {
            if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                    && state.getValue(BlockStateProperties.WATERLOGGED)) {
                // Check if the stored fluid is a hotbath fluid
                if (level instanceof LevelAccessor levelAccessor) {
                    Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(levelAccessor, pos);
                    if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                        // Coral is in hotbath fluid - treat as if not in water (coral will die)
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
        // Otherwise, let vanilla behavior proceed (coral survives in water or hotbath if config disabled)
    }
}
