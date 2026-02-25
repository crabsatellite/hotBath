package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.CustomFluidRegistry;
import com.crabmod.hotbath.custom_fluid.DynamicFluidType;
import com.crabmod.hotbath.fluid_details.BaseFluidType;
import com.crabmod.hotbath.registers.ParticleRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to make pointed dripstone use custom drip particles for hotBath fluids.
 * Handles both BaseFluidType (6 built-in bath types with dedicated particles)
 * and DynamicFluidType (data-pack fluids with dynamic colors).
 */
@Mixin(PointedDripstoneBlock.class)
public abstract class PointedDripstoneMixin {

    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void hotbath$customDripParticle(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!PointedDripstoneBlock.canDrip(state)) return;

        // Walk up from tip to find the ceiling block (first non-dripstone above the stalactite)
        BlockPos ceilingPos = pos.above();
        for (int i = 0; i < 11; i++) {
            BlockState blockState = level.getBlockState(ceilingPos);
            if (blockState.is(Blocks.POINTED_DRIPSTONE)
                    && blockState.getValue(PointedDripstoneBlock.TIP_DIRECTION) == Direction.DOWN) {
                ceilingPos = ceilingPos.above();
            } else {
                break;
            }
        }

        // Check fluid ABOVE the ceiling block (same as vanilla: rootPos.above())
        BlockPos fluidPos = ceilingPos.above();
        Fluid fluid = level.getFluidState(fluidPos).getType();
        FluidType fluidType = fluid.getFluidType();

        ParticleOptions dripParticle = null;
        float r = 0, g = 0, b = 0;
        boolean isDynamic = false;

        if (fluidType instanceof BaseFluidType baseFluidType) {
            dripParticle = baseFluidType.getDripParticle();
        } else if (fluidType instanceof DynamicFluidType) {
            // Get color from BlockEntity at the fluid source position
            BlockEntity be = level.getBlockEntity(fluidPos);
            if (be instanceof CustomFluidBlockEntity customBe) {
                ResourceLocation fluidId = customBe.getFluidId();
                if (fluidId != null) {
                    int color = CustomFluidRegistry.getRuntimeData(fluidId)
                            .map(data -> data.getColorARGB())
                            .orElse(0xFF45E1E9);
                    r = ((color >> 16) & 0xFF) / 255.0F;
                    g = ((color >> 8) & 0xFF) / 255.0F;
                    b = (color & 0xFF) / 255.0F;
                    isDynamic = true;
                }
            }
        }

        if (dripParticle == null && !isDynamic) return;

        // Custom fluid found - handle dripping with same frequency as vanilla water (12%)
        float f = random.nextFloat();
        if (f > 0.12F) {
            ci.cancel();
            return;
        }

        // Spawn drip particle at the stalactite tip
        Vec3 vec3 = state.getOffset(level, pos);
        double x = pos.getX() + 0.5D + vec3.x;
        double y = ((float) (pos.getY() + 1) - 0.6875F) - 0.0625D;
        double z = pos.getZ() + 0.5D + vec3.z;

        if (isDynamic) {
            // Dynamic fluid: use colored drip particle with RGB encoded as speed params
            level.addParticle(ParticleRegister.DRIPPING_DYNAMIC.get(), x, y, z, r, g, b);
        } else {
            // Built-in bath fluid: use dedicated pre-textured particle
            level.addParticle(dripParticle, x, y, z, 0.0D, 0.0D, 0.0D);
        }
        ci.cancel();
    }
}
