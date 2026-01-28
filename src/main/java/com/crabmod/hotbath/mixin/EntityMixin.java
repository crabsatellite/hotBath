package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.fluid_details.BaseFluidType;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to make entities recognize hotBath fluids as water.
 * This allows water-dependent behaviors like fish swimming, drowning checks, etc.
 * to work correctly in hotBath fluids.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    
    @Shadow
    public abstract FluidType getMaxHeightFluidType();
    
    @Shadow
    public abstract FluidType getEyeInFluidType();
    
    /**
     * Make isInWater() return true when entity is in a hotBath fluid.
     * This is the core method that many water-related checks depend on.
     */
    @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true)
    private void hotbath$isInWaterInHotBath(CallbackInfoReturnable<Boolean> cir) {
        FluidType fluidType = this.getMaxHeightFluidType();
        if (fluidType instanceof BaseFluidType) {
            cir.setReturnValue(true);
        }
    }
    
    /**
     * Make isEyeInFluid(FluidTags.WATER) return true when entity's eyes are in a hotBath fluid.
     * This is used by fish movement control and other water-related checks.
     */
    @Inject(method = "isEyeInFluid", at = @At("HEAD"), cancellable = true)
    private void hotbath$isEyeInFluidInHotBath(TagKey<Fluid> fluidTag, CallbackInfoReturnable<Boolean> cir) {
        if (fluidTag == FluidTags.WATER) {
            FluidType eyeFluidType = this.getEyeInFluidType();
            if (eyeFluidType instanceof BaseFluidType) {
                cir.setReturnValue(true);
            }
        }
    }
}
