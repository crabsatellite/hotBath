package com.crabmod.hotbath.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DripParticle;
import net.minecraft.world.level.material.Fluid;

public abstract class DripParticleAccess extends DripParticle {
    public DripParticleAccess(ClientLevel level, double x, double y, double z, Fluid fluid) {
        super(level, x, y, z, fluid);
    }
}
