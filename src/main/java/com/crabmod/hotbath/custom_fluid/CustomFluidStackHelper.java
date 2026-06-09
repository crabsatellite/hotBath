package com.crabmod.hotbath.custom_fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class CustomFluidStackHelper {
    public static final int BUCKET_AMOUNT = 1000;
    public static final int BOTTLE_AMOUNT = 250;

    private CustomFluidStackHelper() {
    }

    public static boolean isDynamicCustomFluid(Fluid fluid) {
        return fluid == DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()
                || fluid == DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get();
    }

    public static boolean isDynamicCustomFluid(FluidStack stack) {
        return stack != null && !stack.isEmpty() && isDynamicCustomFluid(stack.getFluid());
    }

    public static FluidStack createStack(ResourceLocation fluidId, int amount) {
        FluidStack stack = new FluidStack(DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get(), amount);
        setFluidId(stack, fluidId);
        return stack;
    }

    public static void setFluidId(FluidStack stack, ResourceLocation fluidId) {
        if (stack == null || stack.isEmpty() || fluidId == null || !isDynamicCustomFluid(stack)) {
            return;
        }
        CompoundTag customFluidTag = stack.getOrCreateChildTag(CustomFluidNBTHelper.TAG_CUSTOM_FLUID);
        customFluidTag.putString(CustomFluidNBTHelper.TAG_FLUID_ID, fluidId.toString());
    }

    @Nullable
    public static ResourceLocation getFluidId(FluidStack stack) {
        if (stack == null || stack.isEmpty() || !isDynamicCustomFluid(stack)) {
            return null;
        }
        CompoundTag customFluidTag = stack.getChildTag(CustomFluidNBTHelper.TAG_CUSTOM_FLUID);
        if (customFluidTag == null || !customFluidTag.contains(CustomFluidNBTHelper.TAG_FLUID_ID)) {
            return null;
        }
        return ResourceLocation.tryParse(customFluidTag.getString(CustomFluidNBTHelper.TAG_FLUID_ID));
    }

    @Nullable
    public static ResourceLocation getFluidId(ItemStack stack) {
        return CustomFluidNBTHelper.getFluidId(stack);
    }

    public static Optional<CustomFluidDefinition> getDefinition(FluidStack stack) {
        ResourceLocation fluidId = getFluidId(stack);
        return fluidId == null ? Optional.empty() : CustomFluidRegistry.getDefinition(fluidId);
    }

    public static boolean hasSameFluidId(FluidStack left, FluidStack right) {
        ResourceLocation leftId = getFluidId(left);
        return leftId != null && leftId.equals(getFluidId(right));
    }

    @Nullable
    public static ResourceLocation getFluidIdAt(LevelAccessor level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CustomFluidBlockEntity customFluidBlockEntity) {
            return customFluidBlockEntity.getFluidId();
        }

        return null;
    }

    public static boolean setFluidIdAt(Level level, BlockPos pos, ResourceLocation fluidId) {
        if (level == null || pos == null || fluidId == null) {
            return false;
        }

        FluidState fluidState = level.getFluidState(pos);
        if (fluidState.isEmpty() || !isDynamicCustomFluid(fluidState.getType())) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CustomFluidBlockEntity customFluidBlockEntity) {
            if (fluidId.equals(customFluidBlockEntity.getFluidId())) {
                return false;
            }
            customFluidBlockEntity.setFluidId(fluidId);
            return true;
        }

        return false;
    }
}
