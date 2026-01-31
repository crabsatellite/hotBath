package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.util.HotbathFluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for KelpBlock to prevent placement in hotBath fluids.
 * 
 * <p>Kelp cannot survive in hotBath fluids - the hot water kills it.
 * This mixin prevents placing hotBath fluids where kelp exists.</p>
 */
@Mixin(value = KelpBlock.class, priority = 500)
public abstract class KelpBlockMixin {

    /**
     * Prevent placing hotBath fluids in kelp.
     * This would kill the kelp, so we disallow it.
     */
    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    private void hotbath$canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, 
                                         BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        // Only block hotBath fluids specifically, not other mods' water-like fluids
        if (HotbathFluidHelper.isHotbathFluid(fluid)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Handle placing hotBath fluids - always reject for kelp.
     */
    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    private void hotbath$placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState,
                                      CallbackInfoReturnable<Boolean> cir) {
        // Only block hotBath fluids specifically
        if (HotbathFluidHelper.isHotbathFluid(fluidState.getType())) {
            cir.setReturnValue(false);
        }
    }
}
