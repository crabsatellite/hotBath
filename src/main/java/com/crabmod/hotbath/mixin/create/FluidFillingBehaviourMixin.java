package com.crabmod.hotbath.mixin.create;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackContext;
import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidFillingBehaviour.class, remap = false)
public abstract class FluidFillingBehaviourMixin {
    @Inject(method = "tryDeposit", at = @At("RETURN"), remap = false)
    private void hotbath$restoreCustomFluidId(Fluid fluid, BlockPos pos, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if (simulate || !cir.getReturnValueZ() || !CustomFluidStackHelper.isDynamicCustomFluid(fluid)) {
            return;
        }

        FluidStack stack = CustomFluidStackContext.getCreateDepositStack();
        ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(stack);
        if (fluidId == null) {
            return;
        }

        Level level = hotbath$getWorld();
        if (level == null) {
            return;
        }

        hotbath$applyCustomFluidId(level, pos, fluidId);
        BoundingBox affectedArea = hotbath$getAffectedArea();
        if (affectedArea == null) {
            return;
        }

        int volume = (affectedArea.maxX() - affectedArea.minX() + 1)
                * (affectedArea.maxY() - affectedArea.minY() + 1)
                * (affectedArea.maxZ() - affectedArea.minZ() + 1);
        if (volume > 4096) {
            return;
        }

        for (int x = affectedArea.minX(); x <= affectedArea.maxX(); x++) {
            for (int y = affectedArea.minY(); y <= affectedArea.maxY(); y++) {
                for (int z = affectedArea.minZ(); z <= affectedArea.maxZ(); z++) {
                    hotbath$applyCustomFluidId(level, new BlockPos(x, y, z), fluidId);
                }
            }
        }
    }

    @Inject(method = "getAtPos", at = @At("RETURN"), cancellable = true, remap = false)
    private void hotbath$blockDifferentCustomFluid(Level level, BlockPos pos, Fluid fluid, CallbackInfoReturnable<Object> cir) {
        if (!CustomFluidStackHelper.isDynamicCustomFluid(fluid)) {
            return;
        }

        ResourceLocation fluidId = CustomFluidStackHelper.getFluidId(CustomFluidStackContext.getCreateDepositStack());
        if (fluidId == null || !CustomFluidStackHelper.hasDifferentFluidIdAt(level, pos, fluidId)) {
            return;
        }

        cir.setReturnValue(hotbath$blockingSpaceType());
    }

    private boolean hotbath$applyCustomFluidId(Level level, BlockPos pos, ResourceLocation fluidId) {
        return CustomFluidStackHelper.setFluidIdAt(level, pos, fluidId);
    }

    @Nullable
    private Level hotbath$getWorld() {
        return ((BlockEntityBehaviour) (Object) this).getWorld();
    }

    @Nullable
    private BoundingBox hotbath$getAffectedArea() {
        return ((FluidManipulationBehaviourAccessor) (Object) this).hotbath$getAffectedArea();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object hotbath$blockingSpaceType() {
        try {
            Class<?> spaceType = Class.forName("com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour$SpaceType");
            return Enum.valueOf((Class<? extends Enum>) spaceType.asSubclass(Enum.class), "BLOCKING");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Create FluidFillingBehaviour.SpaceType is unavailable", e);
        }
    }
}
