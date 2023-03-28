package net.hiveteam.hotbath.fluid_blocks;

import static net.hiveteam.hotbath.util.ParticleGenerator.renderDefaultSteam;

import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Rose Bath Block */
public class RoseBathBlock extends AbstractHotBathBlock {
  public RoseBathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
    super(supplier, properties);
  }

  @Override
  public void specialEffect(ServerPlayerEntity serverPlayerEntity, Integer stayTime, Integer enteredCount) {

  }

  @Override
  public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, java.util.Random rand) {
    renderDefaultSteam(worldIn, pos, rand);
  }
}
