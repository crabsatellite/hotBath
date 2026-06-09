package com.crabmod.hotbath.mixin.create;

import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = FluidManipulationBehaviour.class, remap = false)
public interface FluidManipulationBehaviourAccessor {
    @Accessor(value = "affectedArea", remap = false)
    @Nullable
    BoundingBox hotbath$getAffectedArea();
}
