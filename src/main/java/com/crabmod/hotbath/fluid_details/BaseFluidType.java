package com.crabmod.hotbath.fluid_details;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import org.joml.Vector3f;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class BaseFluidType extends FluidType {
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final ResourceLocation overlayTexture;
    private final int tintColor;
    private final Vector3f fogColor;
    private final Supplier<? extends ParticleOptions> dripParticle;
    private final Supplier<? extends ParticleOptions> bubbleParticle;
    private final Supplier<? extends ParticleOptions> splashParticle;
    private final Supplier<Fluid> fluidSupplier;

    public BaseFluidType(
            final ResourceLocation stillTexture,
            final ResourceLocation flowingTexture,
            final ResourceLocation overlayTexture,
            final int tintColor,
            final Vector3f fogColor,
            final Properties properties,
            final Supplier<? extends ParticleOptions> dripParticle,
            final Supplier<? extends ParticleOptions> bubbleParticle,
            final Supplier<? extends ParticleOptions> splashParticle,
            final Supplier<Fluid> fluidSupplier) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.overlayTexture = overlayTexture;
        this.tintColor = tintColor;
        this.fogColor = fogColor;
        this.dripParticle = dripParticle;
        this.bubbleParticle = bubbleParticle;
        this.splashParticle = splashParticle;
        this.fluidSupplier = fluidSupplier;
    }

    public ParticleOptions getBubbleParticle() {
        return bubbleParticle != null ? bubbleParticle.get() : null;
    }

    public ParticleOptions getSplashParticle() {
        return splashParticle != null ? splashParticle.get() : null;
    }

    public ResourceLocation getStillTexture() {
        return stillTexture;
    }

    public ResourceLocation getFlowingTexture() {
        return flowingTexture;
    }

    public ResourceLocation getOverlayTexture() {
        return overlayTexture;
    }

    public int getTintColor() {
        return tintColor;
    }

    public Vector3f getFogColor() {
        return fogColor;
    }

    /*
    @Override
    public @Nullable FluidType.DripstoneDripInfo getDripInfo() {
        // Default chance is 0.17578125F (same as water)
        // We return our custom particle and fluid
        if (dripParticle != null) {
            return new FluidType.DripstoneDripInfo(0.17578125F, dripParticle.get(), fluidSupplier != null ? fluidSupplier.get().defaultFluidState().createLegacyBlock().getBlock() : null);
        }
        return super.getDripInfo();
    }
    */

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new HotBathFluidClientExtensions(stillTexture, flowingTexture, overlayTexture, tintColor, fogColor));
    }
}










