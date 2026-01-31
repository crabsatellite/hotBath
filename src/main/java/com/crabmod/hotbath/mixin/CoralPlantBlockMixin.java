package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for CoralPlantBlock to support hotBath fluid waterlogging.
 * 
 * <p>CoralPlantBlock overrides updateShape with its own implementation that
 * hardcodes Fluids.WATER. This mixin injects to handle hotBath fluids.</p>
 * 
 * <p>Also handles coral death in hotBath fluids - keeping the fluid when coral dies.</p>
 * 
 * <p>Note: getStateForPlacement is NOT overridden in CoralPlantBlock - it uses
 * the parent class BaseCoralPlantTypeBlock's implementation, which is handled
 * by BaseCoralPlantTypeBlockMixin.</p>
 */
@Mixin(value = CoralPlantBlock.class, priority = 500)
public abstract class CoralPlantBlockMixin {

    @Shadow @Final private Block deadBlock;

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
     * When coral dies in hotBath fluid, keep the fluid instead of replacing with air.
     * This intercepts the tick method and handles hotBath fluid case specially.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void hotbath$tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, 
                               CallbackInfo ci) {
        // Only handle if coral should die in hotbath (config enabled)
        if (!HotBathConfig.doesCoralDieInHotbath()) {
            return;
        }
        
        // Check if coral is in hotbath fluid
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
            if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                // Coral is in hotbath fluid - die but keep WATERLOGGED=true to preserve fluid
                BlockState deadState = this.deadBlock.defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, Boolean.TRUE);
                level.setBlock(pos, deadState, 2);
                // Transfer the stored fluid type to the dead coral block
                HotbathWaterloggingHelper.storeFluidType(level, pos, storedFluid);
                ci.cancel();
            }
        }
    }
}
