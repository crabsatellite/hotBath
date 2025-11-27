package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.registers.FluidsRegister;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import java.util.function.Supplier;

/** Herbal Bath Block */
public class HerbalBathBlock extends AbstractHotbathBlock {

  public HerbalBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
    super(supplier, properties);
  }
}
