package com.crabmod.hotbath.mixin.sodium;

import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for DefaultRenderContext (inner class of FluidRendererImpl) to debug render parameters.
 */
@Mixin(targets = "net.caffeinemc.mods.sodium.neoforge.render.FluidRendererImpl$DefaultRenderContext", remap = false)
public class DefaultRenderContextMixin {

    @Unique
    private static final Logger HOTBATH_LOGGER = LoggerFactory.getLogger("HotBath-RenderContext");

    @Shadow
    private FluidState fluidState;
    
    @Shadow
    private IClientFluidTypeExtensions handler;

    @Shadow
    private ColorProviderRegistry colorProviderRegistry;

    @Shadow
    private LevelSlice level;

    @Shadow
    private BlockPos blockPos;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void hotbath$debugRender(CallbackInfo ci) {
        String fluidName = fluidState.getType().toString();
        if (fluidName.contains("hotbath")) {
            HOTBATH_LOGGER.info("[HotBath-RenderContext] === render() called ===");
            HOTBATH_LOGGER.info("[HotBath-RenderContext] fluidState={}", fluidState.getType());
            HOTBATH_LOGGER.info("[HotBath-RenderContext] handler={}", handler.getClass().getName());
            
            // Check handler tintColor
            int tintColor = handler.getTintColor();
            int alpha = (tintColor >> 24) & 0xFF;
            int red = (tintColor >> 16) & 0xFF;
            int green = (tintColor >> 8) & 0xFF;
            int blue = tintColor & 0xFF;
            HOTBATH_LOGGER.info("[HotBath-RenderContext] handler.getTintColor()=0x{} (A={}, R={}, G={}, B={})", 
                Integer.toHexString(tintColor), alpha, red, green, blue);
            
            // Check what colorProviderRegistry returns
            Fluid fluid = fluidState.getType();
            ColorProvider<FluidState> override = colorProviderRegistry.getColorProvider(fluid);
            HOTBATH_LOGGER.info("[HotBath-RenderContext] colorProviderRegistry.getColorProvider({})={}", 
                fluid, override != null ? override.getClass().getName() : "null (will use handler)");
            
            // Check sprites that will be used
            TextureAtlasSprite[] sprites = FluidSpriteCache.getFluidSprites(level, blockPos, fluidState);
            HOTBATH_LOGGER.info("[HotBath-RenderContext] FluidSpriteCache.getFluidSprites: still={}, flowing={}", 
                sprites[0] != null ? sprites[0].contents().name() : "NULL",
                sprites[1] != null ? sprites[1].contents().name() : "NULL");
        }
    }
}
