package com.crabmod.hotbath.client.particle;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;

/**
 * A bubble particle that accepts its color via the speed parameters (xSpeed=R, ySpeed=G, zSpeed=B).
 * Used for DynamicFluidType bubble columns where the color is determined at runtime
 * from the CustomFluidDefinition stored in the BlockEntity.
 * Uses the same grayscale bubble texture tinted with the dynamic color.
 */
public class ColoredBubbleParticle extends TextureSheetParticle {
    ColoredBubbleParticle(ClientLevel level, double x, double y, double z,
                           double rSpeed, double gSpeed, double bSpeed) {
        super(level, x, y, z);
        this.setSize(0.02F, 0.02F);
        this.quadSize *= this.random.nextFloat() * 0.6F + 0.2F;
        // Fixed bubble physics - speed params are color, not velocity
        this.xd = (Math.random() * 2.0D - 1.0D) * 0.02D;
        this.yd = (Math.random() * 2.0D - 1.0D) * 0.02D;
        this.zd = (Math.random() * 2.0D - 1.0D) * 0.02D;
        this.lifetime = (int)(8.0D / (Math.random() * 0.8D + 0.2D));
        // Apply color from speed params
        this.rCol = (float) rSpeed;
        this.gCol = (float) gSpeed;
        this.bCol = (float) bSpeed;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.lifetime-- <= 0) {
            this.remove();
        } else {
            this.yd += 0.002D;
            this.move(this.xd, this.yd, this.zd);
            this.xd *= 0.85F;
            this.yd *= 0.85F;
            this.zd *= 0.85F;
            if (!this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER) &&
                !(this.level.getBlockState(BlockPos.containing(this.x, this.y, this.z)).getBlock() instanceof AbstractHotbathBlock)) {
                this.remove();
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return HotBathBubbleParticle.PARTICLE_SHEET_OPAQUE_NO_DEPTH;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            ColoredBubbleParticle particle = new ColoredBubbleParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }
}
