package com.crabmod.hotbath.client.particle;

import com.crabmod.hotbath.registers.ParticleRegister;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.material.Fluids;

/**
 * A drip particle that accepts its color via the speed parameters (xSpeed=R, ySpeed=G, zSpeed=B).
 * Used for DynamicFluidType dripstone dripping where the color is determined at runtime.
 * Uses vanilla drip textures (grayscale) tinted with the dynamic color.
 */
public class ColoredDripParticle extends DripParticleAccess {
    private final float colorR;
    private final float colorG;
    private final float colorB;
    private final boolean isHanging;
    private final boolean isFalling;

    protected ColoredDripParticle(ClientLevel level, double x, double y, double z,
                                   float r, float g, float b, boolean isHanging, boolean isFalling) {
        super(level, x, y, z, Fluids.WATER);
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.isHanging = isHanging;
        this.isFalling = isFalling;
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        } else {
            if (this.onGround && this.isFalling) {
                this.remove();
                // Spawn landing particle with same color
                this.level.addParticle(ParticleRegister.LANDING_DYNAMIC.get(),
                        this.x, this.y, this.z, this.colorR, this.colorG, this.colorB);
                this.level.playLocalSound(this.x, this.y, this.z,
                        SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON,
                        SoundSource.BLOCKS, 0.3F, 1.0F, false);
            }

            if (this.isHanging && this.lifetime - this.age <= 20 && !this.removed) {
                // Transition from hanging to falling with same color
                this.remove();
                this.level.addParticle(ParticleRegister.FALLING_DYNAMIC.get(),
                        this.x, this.y, this.z, this.colorR, this.colorG, this.colorB);
            }

            // Movement
            if (this.isHanging) {
                this.xd *= 0.0D;
                this.yd *= 0.0D;
                this.zd *= 0.0D;
            } else {
                this.yd -= (double) this.gravity;
                this.move(this.xd, this.yd, this.zd);
                this.xd *= 0.98D;
                this.yd *= 0.98D;
                this.zd *= 0.98D;
            }
        }
    }

    public static class HangingFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public HangingFactory(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
            ColoredDripParticle particle = new ColoredDripParticle(level, x, y, z,
                    (float) xSpeed, (float) ySpeed, (float) zSpeed, true, false);
            particle.pickSprite(this.sprite);
            particle.gravity = 0.0F;
            particle.setLifetime(40 + level.random.nextInt(40));
            return particle;
        }
    }

    public static class FallingFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public FallingFactory(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
            ColoredDripParticle particle = new ColoredDripParticle(level, x, y, z,
                    (float) xSpeed, (float) ySpeed, (float) zSpeed, false, true);
            particle.pickSprite(this.sprite);
            particle.gravity = 0.06F;
            particle.setLifetime((int) (64.0D / (Math.random() * 0.8D + 0.2D)));
            return particle;
        }
    }

    public static class LandingFactory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public LandingFactory(SpriteSet sprite) {
            this.sprite = sprite;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                        double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed) {
            ColoredDripParticle particle = new ColoredDripParticle(level, x, y, z,
                    (float) xSpeed, (float) ySpeed, (float) zSpeed, false, false);
            particle.pickSprite(this.sprite);
            particle.gravity = 0.0F;
            particle.setLifetime((int) (16.0D / (Math.random() * 0.8D + 0.2D)));
            return particle;
        }
    }
}
