package com.crabmod.hotbath.mixin.create;

import com.crabmod.hotbath.custom_fluid.CustomFluidStackHelper;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import it.unimi.dsi.fastutil.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidDrainingBehaviour.class, remap = false)
public abstract class FluidDrainingBehaviourMixin {
    @Shadow(remap = false)
    PriorityQueue<FluidManipulationBehaviour.BlockPosEntry> queue;

    @Inject(method = "getDrainableFluid", at = @At("RETURN"), cancellable = true, remap = false)
    private void hotbath$preserveCustomFluidId(BlockPos pos, CallbackInfoReturnable<FluidStack> cir) {
        FluidStack stack = cir.getReturnValue();
        if (!CustomFluidStackHelper.isDynamicCustomFluid(stack)
                || CustomFluidStackHelper.getFluidId(stack) != null) {
            return;
        }

        ResourceLocation fluidId = hotbath$findCustomFluidId(pos);
        if (fluidId == null) {
            return;
        }

        FluidStack copy = stack.copy();
        CustomFluidStackHelper.setFluidId(copy, fluidId);
        cir.setReturnValue(copy);
    }

    @Nullable
    private ResourceLocation hotbath$findCustomFluidId(BlockPos requestedPos) {
        Level level = hotbath$getWorld();
        if (level == null) {
            return null;
        }

        if (queue != null && !queue.isEmpty()) {
            ResourceLocation queuedId = CustomFluidStackHelper.getFluidIdAt(level, queue.first().pos());
            if (queuedId != null) {
                return queuedId;
            }
        }

        ResourceLocation requestedId = CustomFluidStackHelper.getFluidIdAt(level, requestedPos);
        if (requestedId != null) {
            return requestedId;
        }

        BoundingBox affectedArea = hotbath$getAffectedArea();
        if (affectedArea == null) {
            return null;
        }

        int volume = (affectedArea.maxX() - affectedArea.minX() + 1)
                * (affectedArea.maxY() - affectedArea.minY() + 1)
                * (affectedArea.maxZ() - affectedArea.minZ() + 1);
        if (volume > 4096) {
            return null;
        }

        for (int x = affectedArea.minX(); x <= affectedArea.maxX(); x++) {
            for (int y = affectedArea.minY(); y <= affectedArea.maxY(); y++) {
                for (int z = affectedArea.minZ(); z <= affectedArea.maxZ(); z++) {
                    ResourceLocation areaId = CustomFluidStackHelper.getFluidIdAt(level, new BlockPos(x, y, z));
                    if (areaId != null) {
                        return areaId;
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private Level hotbath$getWorld() {
        return ((BlockEntityBehaviour) (Object) this).getWorld();
    }

    @Nullable
    private BoundingBox hotbath$getAffectedArea() {
        return ((FluidManipulationBehaviourAccessor) (Object) this).hotbath$getAffectedArea();
    }
}
