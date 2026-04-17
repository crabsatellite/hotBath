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
 * Physics replicate vanilla {@code BubbleColumnUpParticle} exactly.
 */
public class ColoredBubbleParticle extends TextureSheetParticle {

    ColoredBubbleParticle(ClientLevel level, double x, double y, double z,
                           double rSpeed, double gSpeed, double bSpeed) {
        super(level, x, y, z);
        this.gravity = -0.125F;
        this.friction = 0.85F;
        this.setSize(0.02F, 0.02F);
        this.quadSize = this.quadSize * (this.random.nextFloat() * 0.6F + 0.2F);
        // Speed params carry color, not velocity
        this.xd = (Math.random() * 2.0 - 1.0) * 0.02;
        this.yd = (Math.random() * 2.0 - 1.0) * 0.02;
        this.zd = (Math.random() * 2.0 - 1.0) * 0.02;
        this.lifetime = (int)(40.0 / (Math.random() * 0.8 + 0.2));
        // Apply color from speed params
        this.rCol = (float) rSpeed;
        this.gCol = (float) gSpeed;
        this.bCol = (float) bSpeed;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
            if (!this.level.getFluidState(pos).is(FluidTags.WATER)
                    && !(this.level.getBlockState(pos).getBlock() instanceof AbstractHotbathBlock)) {
                this.remove();
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
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
