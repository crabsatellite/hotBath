package com.crabmod.hotbath.mixin.sodium;

import com.crabmod.hotbath.custom_fluid.CustomFluidItems;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Mixin to fix Z-fighting for smoke overlay when Sodium is present.
 * Wraps the renderQuadList call to apply offset for smoke layer.
 */
@Mixin(value = ItemRenderer.class, priority = 1100)
public class ItemRendererSodiumSmokeZOffsetMixin {

    // Small offset to separate smoke layer from base layer
    @Unique
    private static final float SMOKE_LAYER_OFFSET = 0.001f;

    /**
     * Wrap the renderQuadList call to apply offset for smoke layers.
     */
    @WrapOperation(
        method = "renderModelLists",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;II)V"
        ),
        require = 0
    )
    private void hotbath$wrapRenderQuadListForSmoke(
            ItemRenderer instance,
            PoseStack poseStack,
            VertexConsumer buffer,
            List<BakedQuad> quads,
            ItemStack itemStack,
            int combinedLight,
            int combinedOverlay,
            Operation<Void> original
    ) {
        if (quads.isEmpty() || itemStack.isEmpty() || !isCustomFluidItem(itemStack)) {
            original.call(instance, poseStack, buffer, quads, itemStack, combinedLight, combinedOverlay);
            return;
        }

        BakedQuad firstQuad = quads.get(0);
        if (isSmokeTexture(firstQuad)) {
            poseStack.pushPose();
            poseStack.translate(SMOKE_LAYER_OFFSET, 0, SMOKE_LAYER_OFFSET);
            try {
                original.call(instance, poseStack, buffer, quads, itemStack, combinedLight, combinedOverlay);
            } finally {
                poseStack.popPose();
            }
        } else {
            original.call(instance, poseStack, buffer, quads, itemStack, combinedLight, combinedOverlay);
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
