package com.crabmod.hotbath.mixin.sodium;

import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Debug mixin for DefaultFluidRenderer to verify parameters passed to render().
 */
@Mixin(value = DefaultFluidRenderer.class, remap = false)
public class DefaultFluidRendererMixin {

    @Unique
    private static final Logger HOTBATH_LOGGER = LoggerFactory.getLogger("HotBath-DefaultFluidRenderer");

    @Shadow
    private int[] quadColors;

    @Shadow
    private BlockPos.MutableBlockPos scratchPos;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void hotbath$debugRenderParams(LevelSlice level, BlockState blockState, FluidState fluidState, 
            BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkModelBuilder meshBuilder, 
            Material material, ColorProvider<FluidState> colorProvider, TextureAtlasSprite[] sprites, CallbackInfo ci) {
        
        // Only log for HotBath fluids
        String fluidName = fluidState.getType().toString();
        if (fluidName.contains("hotbath")) {
            HOTBATH_LOGGER.info("[HotBath-DefaultFluidRenderer] === render() called ===");
            HOTBATH_LOGGER.info("[HotBath-DefaultFluidRenderer] fluidState={}", fluidState.getType());
            HOTBATH_LOGGER.info("[HotBath-DefaultFluidRenderer] material={}, isTranslucent={}", material, material.isTranslucent());
            HOTBATH_LOGGER.info("[HotBath-DefaultFluidRenderer] sprites: still={}, flowing={}", 
                sprites[0] != null ? sprites[0].contents().name() : "NULL",
                sprites[1] != null ? sprites[1].contents().name() : "NULL");
            
            // Log color provider info
            HOTBATH_LOGGER.info("[HotBath-DefaultFluidRenderer] colorProvider={}", colorProvider.getClass().getName());
            
            // Get the color that would be returned
            try {
                int[] testColors = new int[4];
                colorProvider.getColors(level, blockPos, scratchPos, fluidState, null, testColors);
                int color = testColors[0];
                
                // Parse ARGB components
                int alpha = (color >> 24) & 0xFF;
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = color & 0xFF;
                
                HOTBATH_LOGGER.info("[HotBath-DefaultFluidRenderer] colorProvider returned: ARGB=0x{} (A={}, R={}, G={}, B={})", 
                    Integer.toHexString(color), alpha, red, green, blue);
            } catch (Exception e) {
                HOTBATH_LOGGER.warn("[HotBath-DefaultFluidRenderer] Error getting color: {}", e.getMessage());
            }
            
            // Log sprite dimensions and properties
            if (sprites[0] != null) {
                var contents = sprites[0].contents();
                HOTBATH_LOGGER.info("[HotBath-DefaultFluidRenderer] still sprite: name={}, width={}, height={}", 
                    contents.name(), contents.width(), contents.height());
            }
        }
    }
}
