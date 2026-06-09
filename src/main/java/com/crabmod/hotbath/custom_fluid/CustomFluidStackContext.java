package com.crabmod.hotbath.custom_fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class CustomFluidStackContext {
    private static final ThreadLocal<FluidStack> CURRENT_CREATE_DEPOSIT = new ThreadLocal<>();

    private CustomFluidStackContext() {
    }

    public static void setCreateDepositStack(FluidStack stack) {
        CURRENT_CREATE_DEPOSIT.set(stack == null ? null : stack.copy());
    }

    @Nullable
    public static FluidStack getCreateDepositStack() {
        FluidStack stack = CURRENT_CREATE_DEPOSIT.get();
        return stack == null ? null : stack.copy();
    }

    public static void clearCreateDepositStack() {
        CURRENT_CREATE_DEPOSIT.remove();
    }
}
