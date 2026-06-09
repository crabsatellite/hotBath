package com.crabmod.hotbath.mixin.create;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackContext;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HosePulleyFluidHandler.class, remap = false)
public class HosePulleyFluidHandlerMixin {
    @Inject(method = "fill", at = @At("HEAD"), remap = false)
    private void hotbath$captureCustomFluidStack(FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
        if (CustomFluidStackHelper.getFluidId(resource) != null) {
            CustomFluidStackContext.setCreateDepositStack(resource);
        }
    }

    @Inject(method = "fill", at = @At("RETURN"), remap = false)
    private void hotbath$clearCustomFluidStack(FluidStack resource, IFluidHandler.FluidAction action, CallbackInfoReturnable<Integer> cir) {
        CustomFluidStackContext.clearCreateDepositStack();
    }
}
