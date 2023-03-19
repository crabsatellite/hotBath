package net.hiveteam.hotbath.util;

import static net.hiveteam.hotbath.register.ParticleRegister.STEAM_PARTICLE;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Fluids Particles */
public class ParticleGenerator {

  /**
   * Render default steam
   *
   * @param worldIn
   * @param pos
   * @param rand
   */
  public static void renderDefaultSteam(World worldIn, BlockPos pos, java.util.Random rand) {
    if (rand.nextInt(100) == 0) {
      worldIn.addParticle(
          STEAM_PARTICLE.get(),
          (double) pos.getX() + rand.nextDouble(),
          (double) pos.getY() + rand.nextDouble(),
          (double) pos.getZ() + rand.nextDouble(),
          0.0D,
          0.05D,
          0.0D);
    }
  }

  // new particle types

}