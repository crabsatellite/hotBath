package com.crabmod.hotbath.mixin.sodium;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.neoforge.render.FluidRendererImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Sodium's FluidRendererImpl to intercept fluid state during rendering.
 * 
 * <p>Sodium bypasses vanilla's BlockRenderDispatcher.renderLiquid() and uses its own
 * optimized fluid rendering pipeline. This mixin intercepts all uses of fluidState
 * in the render method to use our hotBath fluid when appropriate.</p>
 * 
 * <p>Strategy: We use ThreadLocal to capture blockState and blockPos at method entry,
 * then use @Redirect and @ModifyArg to replace fluidState at each use site.</p>
 * 
 * <p>This mixin is only applied when Sodium is present, controlled by SodiumMixinPlugin.</p>
 */
@Mixin(value = FluidRendererImpl.class, remap = false)
public class FluidRendererImplMixin {

    @Unique
    private static final Logger HOTBATH_LOGGER = LoggerFactory.getLogger("HotBath-Sodium");

    /**
     * Thread-local storage for the correct FluidState.
     * Set at method entry if the block is waterlogged with a hotBath fluid.
     */
    @Unique
    private static final ThreadLocal<FluidState> hotbath$correctFluidState = new ThreadLocal<>();

    /**
     * Capture the correct FluidState at the beginning of render.
     * If the block is waterlogged with a hotBath fluid, store that fluid state.
     */
    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void hotbath$captureCorrectFluidState(LevelSlice level, BlockState blockState, FluidState fluidState, 
            BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkBuildBuffers buffers, CallbackInfo ci) {
        
        // Reset the ThreadLocal
        hotbath$correctFluidState.set(null);
        
        // Check if this block is waterlogged with a hotBath fluid
        if (blockState != null && blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            boolean isWaterlogged = blockState.getValue(BlockStateProperties.WATERLOGGED);
            
            if (isWaterlogged) {
                // Make immutable copy for safe cache lookup (blockPos might be mutable)
                BlockPos immutablePos = blockPos.immutable();
                
                // Get the stored fluid type from our client cache
                Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(immutablePos);
                
                // Debug logging
                HOTBATH_LOGGER.info("[HotBath-Sodium] Waterlogged block at {}: storedFluid={}, originalFluid={}", 
                    immutablePos, storedFluid, fluidState.getType());
                
                if (storedFluid != null && storedFluid != Fluids.EMPTY && storedFluid != Fluids.WATER) {
                    // Store the correct fluid state for use in the redirects
                    FluidState correctState = storedFluid.defaultFluidState();
                    hotbath$correctFluidState.set(correctState);
                    HOTBATH_LOGGER.info("[HotBath-Sodium] Set correct fluid state: {}", correctState.getType());
                }
            }
        }
    }

    /**
     * Clean up thread-local storage after render completes.
     */
    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void hotbath$clearCorrectFluidState(LevelSlice level, BlockState blockState, FluidState fluidState, 
            BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkBuildBuffers buffers, CallbackInfo ci) {
        hotbath$correctFluidState.remove();
    }

    /**
     * Helper method to get the correct FluidState.
     */
    @Unique
    private static FluidState hotbath$getCorrectFluidState(FluidState original) {
        FluidState correct = hotbath$correctFluidState.get();
        return correct != null ? correct : original;
    }

    /**
     * Redirect DefaultMaterials.forFluidState(fluidState) to use our correct fluid.
     */
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forFluidState(Lnet/minecraft/world/level/material/FluidState;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;",
            remap = false
        ),
        index = 0,
        remap = false
    )
    private FluidState hotbath$modifyForFluidStateArg(FluidState original) {
        return hotbath$getCorrectFluidState(original);
    }

    /**
     * Redirect IClientFluidTypeExtensions.of(fluidState) to use our correct fluid.
     */
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;of(Lnet/minecraft/world/level/material/FluidState;)Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;",
            remap = false
        ),
        remap = false
    )
    private IClientFluidTypeExtensions hotbath$redirectGetFluidExtensions(FluidState original) {
        FluidState corrected = hotbath$getCorrectFluidState(original);
        IClientFluidTypeExtensions handler = IClientFluidTypeExtensions.of(corrected);
        if (corrected != original) {
            int tintColor = handler.getTintColor();
            HOTBATH_LOGGER.info("[HotBath-Sodium] Redirected IClientFluidTypeExtensions.of(): original={}, corrected={}, handler={}, tintColor=0x{}", 
                original.getType(), corrected.getType(), handler.getClass().getName(), Integer.toHexString(tintColor));
        }
        return handler;
    }

    /**
     * Modify handler.renderFluid(fluidState, ...) to use our correct fluid.
     * renderFluid signature: (FluidState, BlockAndTintGetter, BlockPos, VertexConsumer, BlockState) -> boolean
     */
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;renderFluid(Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            remap = false
        ),
        index = 0,
        remap = false
    )
    private FluidState hotbath$modifyRenderFluidArg(FluidState original) {
        return hotbath$getCorrectFluidState(original);
    }

    /**
     * Modify defaultContext.setUp(..., fluidState, ...) to use our correct fluid.
     * setUp signature: (ColorProviderRegistry, DefaultFluidRenderer, LevelSlice, BlockState, FluidState, BlockPos, BlockPos, TranslucentGeometryCollector, ChunkModelBuilder, Material, IClientFluidTypeExtensions)
     * FluidState is at index 4 (0-indexed).
     */
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/neoforge/render/FluidRendererImpl$DefaultRenderContext;setUp(Lnet/caffeinemc/mods/sodium/client/model/color/ColorProviderRegistry;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;)V",
            remap = false
        ),
        index = 4,
        remap = false
    )
    private FluidState hotbath$modifySetUpFluidStateArg(FluidState original) {
        return hotbath$getCorrectFluidState(original);
    }

    /**
     * Modify defaultContext.setUp(..., handler) to use our correct fluid's handler.
     * IClientFluidTypeExtensions is at index 10 (0-indexed).
     * This ensures the color provider uses our fluid's tint color.
     */
    @ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/neoforge/render/FluidRendererImpl$DefaultRenderContext;setUp(Lnet/caffeinemc/mods/sodium/client/model/color/ColorProviderRegistry;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;)V",
            remap = false
        ),
        index = 10,
        remap = false
    )
    private IClientFluidTypeExtensions hotbath$modifySetUpHandlerArg(IClientFluidTypeExtensions original) {
        FluidState correctFluidState = hotbath$correctFluidState.get();
        if (correctFluidState != null) {
            IClientFluidTypeExtensions correctHandler = IClientFluidTypeExtensions.of(correctFluidState);
            HOTBATH_LOGGER.info("[HotBath-Sodium] Replaced setUp handler: original={}, corrected={}, tintColor=0x{}", 
                original.getClass().getName(), correctHandler.getClass().getName(), 
                Integer.toHexString(correctHandler.getTintColor()));
            return correctHandler;
        }
        return original;
    }
}
