package com.crabmod.hotbath.fluid_details;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BaseFluidType extends FluidType {
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final ResourceLocation overlayTexture;
    private final int tintColor;
    private final Vector3f fogColor;
    private final Supplier<? extends ParticleOptions> dripParticle;

    public BaseFluidType(
            final ResourceLocation stillTexture,
            final ResourceLocation flowingTexture,
            final ResourceLocation overlayTexture,
            final int tintColor,
            final Vector3f fogColor,
            final Properties properties,
            final Supplier<? extends ParticleOptions> dripParticle) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.overlayTexture = overlayTexture;
        this.tintColor = tintColor;
        this.fogColor = fogColor;
        this.dripParticle = dripParticle;
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

    @Override
    public @Nullable FluidType.DripstoneDripInfo getDripInfo() {
        FluidType.DripstoneDripInfo info = super.getDripInfo();
        if (dripParticle != null && info != null) {
            return new FluidType.DripstoneDripInfo(info.chance(), dripParticle.get(), info.filledCauldron());
        }
        return info;
    }

    @SuppressWarnings("removal")
    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(
                new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return stillTexture;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return flowingTexture;
                    }

                    @Override
                    public ResourceLocation getOverlayTexture() {
                        return overlayTexture;
                    }

                    @Override
                    public int getTintColor() {
                        return tintColor;
                    }

                    @Override
                    public @NotNull Vector3f modifyFogColor(
                            Camera camera,
                            float partialTick,
                            ClientLevel level,
                            int renderDistance,
                            float darkenWorldAmount,
                            Vector3f fluidFogColor) {
                        return fogColor;
                    }
                });
    }
}
