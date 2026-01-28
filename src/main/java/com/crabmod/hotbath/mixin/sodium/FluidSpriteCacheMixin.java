package com.crabmod.hotbath.mixin.sodium;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for NeoForge's FluidSpriteCache to fix waterlogged block fluid sprite lookup.
 * 
 * <p>When Sodium renders fluids in waterlogged blocks, it uses FluidSpriteCache.getFluidSprites()
 * to get the fluid textures. For hotbath waterlogged blocks, the FluidState is still water,
 * so this returns water sprites which appear transparent.</p>
 * 
 * <p>This mixin intercepts the getFluidSprites call and checks our waterlogging cache.
 * If a hotbath fluid is stored for this position, we get sprites for that fluid instead.</p>
 */
@Mixin(value = FluidSpriteCache.class, remap = false)
public class FluidSpriteCacheMixin {

    @Unique
    private static final Logger HOTBATH_LOGGER = LoggerFactory.getLogger("HotBath-FluidSprite");

    /**
     * Intercept getFluidSprites to use our stored fluid type for sprite lookup.
     * 
     * <p>If a hotbath fluid is stored for this position, we cancel the original call
     * and return sprites for the correct fluid.</p>
     */
    @Inject(
        method = "getFluidSprites",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void hotbath$redirectFluidSprites(BlockAndTintGetter level, BlockPos pos, FluidState fluid, 
            CallbackInfoReturnable<TextureAtlasSprite[]> cir) {
        // Check if we have a stored hotbath fluid for this position
        if (pos != null) {
            BlockPos immutablePos = pos.immutable();
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(immutablePos);
            
            // Debug logging
            if (storedFluid != null) {
                HOTBATH_LOGGER.info("[HotBath-FluidSprite] getFluidSprites at {}: storedFluid={}, originalFluid={}", 
                    immutablePos, storedFluid, fluid.getType());
            }
            
            if (storedFluid != null && storedFluid != Fluids.EMPTY && storedFluid != Fluids.WATER) {
                // Get the correct fluid state for sprite lookup
                FluidState correctFluidState = storedFluid.defaultFluidState();
                
                // Get sprites using the correct fluid - call the original method logic
                IClientFluidTypeExtensions props = IClientFluidTypeExtensions.of(correctFluidState);
                
                var stillTexture = props.getStillTexture(correctFluidState, level, pos);
                var flowingTexture = props.getFlowingTexture(correctFluidState, level, pos);
                var overlayTexture = props.getOverlayTexture(correctFluidState, level, pos);
                
                HOTBATH_LOGGER.info("[HotBath-FluidSprite] Using hotbath fluid sprites: stillTexture={}, flowingTexture={}", 
                    stillTexture, flowingTexture);
                
                // Use hotbath textures
                TextureAtlasSprite stillSprite = FluidSpriteCache.getSprite(stillTexture);
                TextureAtlasSprite flowingSprite = FluidSpriteCache.getSprite(flowingTexture);
                TextureAtlasSprite overlaySprite = overlayTexture == null ? null : FluidSpriteCache.getSprite(overlayTexture);
                
                HOTBATH_LOGGER.info("[HotBath-FluidSprite] Sprites: still={} ({}), flowing={} ({})", 
                    stillSprite != null ? stillSprite.contents().name() : "NULL",
                    stillSprite != null ? (stillSprite.contents().name().getPath().contains("missing") ? "MISSING!" : "OK") : "NULL",
                    flowingSprite != null ? flowingSprite.contents().name() : "NULL",
                    flowingSprite != null ? (flowingSprite.contents().name().getPath().contains("missing") ? "MISSING!" : "OK") : "NULL");
                
                TextureAtlasSprite[] sprites = new TextureAtlasSprite[] {
                    stillSprite, flowingSprite, overlaySprite
                };
                
                cir.setReturnValue(sprites);
                return; // 确保方法结束
            }
        }
    }
}
