package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.registers.FluidsRegister;
import com.crabmod.hotbath.util.ParticleGenerator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/** Milk Bath Block */
public class MilkBathBlock extends LiquidBlock implements IHotbathBlock {
  public MilkBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
    super(supplier, properties);
  }

  @Override
  public FluidState getHotBathFluidState() {
    return FluidsRegister.MILK_BATH_FLUID.get().defaultFluidState();
  }

  @Override
  @OnlyIn(Dist.CLIENT)
  public void animateTick(
      @NotNull BlockState stateIn,
      @NotNull Level worldIn,
      @NotNull BlockPos pos,
      java.util.@NotNull Random rand) {
    ParticleGenerator.renderDefaultSteam((ClientLevel) worldIn, pos, rand);
  }
}
