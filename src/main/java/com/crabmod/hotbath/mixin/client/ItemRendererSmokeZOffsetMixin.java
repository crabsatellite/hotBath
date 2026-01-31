package com.crabmod.hotbath.mixin.client;

import com.crabmod.hotbath.custom_fluid.CustomFluidItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Mixin to fix Z-fighting for smoke overlay on custom fluid items.
 */
@Mixin(ItemRenderer.class)
public class ItemRendererSmokeZOffsetMixin {

    // Small offset to separate smoke layer from base layer, prevents Z-fighting
    @Unique
    private static final float SMOKE_LAYER_OFFSET = 0.001f;

    /**
     * Inject at the start of renderQuadList to apply offset for smoke layer.
     */
    @Inject(method = "renderQuadList", at = @At("HEAD"))
    private void hotbath$applyZOffsetForSmoke(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads,
                                               ItemStack itemStack, int combinedLight, int combinedOverlay, CallbackInfo ci) {
        if (itemStack.isEmpty() || quads.isEmpty()) return;
        if (!isCustomFluidItem(itemStack)) return;

        BakedQuad firstQuad = quads.get(0);
        if (isSmokeTexture(firstQuad)) {
            poseStack.pushPose();
            poseStack.translate(SMOKE_LAYER_OFFSET, 0, SMOKE_LAYER_OFFSET);
        }
    }

    /**
     * Inject at the end of renderQuadList to restore pose if we modified it.
     */
    @Inject(method = "renderQuadList", at = @At("RETURN"))
    private void hotbath$restoreZOffset(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads,
                                         ItemStack itemStack, int combinedLight, int combinedOverlay, CallbackInfo ci) {
        if (itemStack.isEmpty() || quads.isEmpty()) return;
        if (!isCustomFluidItem(itemStack)) return;

        BakedQuad firstQuad = quads.get(0);
        if (isSmokeTexture(firstQuad)) {
            poseStack.popPose();
        }
    }

    @Unique
    private boolean isCustomFluidItem(ItemStack stack) {
        return stack.is(CustomFluidItems.CUSTOM_FLUID_BUCKET.get()) ||
               stack.is(CustomFluidItems.CUSTOM_FLUID_BOTTLE.get()) ||
               stack.is(CustomFluidItems.CUSTOM_FLUID_SPLASH_BOTTLE.get());
    }

    @Unique
    private boolean isSmokeTexture(BakedQuad quad) {
        if (quad.getSprite() == null || quad.getSprite().contents() == null) return false;
        String textureName = quad.getSprite().contents().name().toString();
        return textureName.contains("smoke");
    }
}
