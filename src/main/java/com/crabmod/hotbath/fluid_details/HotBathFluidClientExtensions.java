package com.crabmod.hotbath.fluid_details;

import com.crabmod.hotbath.HotBath;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
@SuppressWarnings("removal")
public class HotBathFluidClientExtensions implements IClientFluidTypeExtensions {
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;
    private final ResourceLocation overlayTexture;
    private final int tintColor;
    private final Vector3f fogColor;

    public HotBathFluidClientExtensions(ResourceLocation stillTexture, ResourceLocation flowingTexture, ResourceLocation overlayTexture, int tintColor, Vector3f fogColor) {
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.overlayTexture = overlayTexture;
        this.tintColor = tintColor;
        this.fogColor = fogColor;
    }

    @Override
    public ResourceLocation getStillTexture() {
        if (stillTexture == null) {
            HotBath.LOGGER.warn("Still texture is null! Defaulting to water.");
            return new ResourceLocation("minecraft", "block/water_still");
        }
        return stillTexture;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        if (flowingTexture == null) {
            HotBath.LOGGER.warn("Flowing texture is null! Defaulting to water.");
            return new ResourceLocation("minecraft", "block/water_flow");
        }
        return flowingTexture;
    }

    @Override
    public ResourceLocation getOverlayTexture() {
        if (overlayTexture == null) {
            HotBath.LOGGER.warn("Overlay texture is null! Defaulting to water overlay.");
            return new ResourceLocation("minecraft", "block/water_overlay");
        }
        return overlayTexture;
    }

    @Override
    public int getTintColor() {
        return tintColor;
    }

    @Override
    public @NotNull Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
        Vector3f color = fogColor;
        if (color == null) {
            color = fluidFogColor;
        }
        if (color == null) {
            color = new Vector3f(1f, 1f, 1f);
        }
        return color;
    }
}
