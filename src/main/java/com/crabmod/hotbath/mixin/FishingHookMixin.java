package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.fluid_details.BaseFluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to prevent fishing in HotBath fluids.
 * This allows HotBath fluids to remain in the water tag (for swimming, boats, etc.)
 * while still preventing players from catching fish in them.
 */
@Mixin(FishingHook.class)
public class FishingHookMixin {

    /**
     * Cancels the fishing logic when the fishing hook is in a HotBath fluid.
     * This is injected at the HEAD of catchingFish method.
     */
    @Inject(method = "catchingFish", at = @At("HEAD"), cancellable = true)
    private void hotbath$cancelFishingInHotBathFluid(BlockPos pos, CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        FluidState fluidState = hook.level().getFluidState(pos);
        
        // Check if the fluid is a HotBath fluid by checking its FluidType
        FluidType fluidType = fluidState.getFluidType();
        if (fluidType instanceof BaseFluidType) {
            // Cancel the fishing logic - no fish will be caught
            ci.cancel();
        }
    }
}
