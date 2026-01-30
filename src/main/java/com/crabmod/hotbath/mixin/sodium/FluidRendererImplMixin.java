package com.crabmod.hotbath.mixin.sodium;

import com.crabmod.hotbath.util.HotbathFluidHelper;
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
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Sodium's FluidRendererImpl to fix hotBath fluid rendering in waterlogged blocks.
 * 
 * <p>Sodium bypasses vanilla's fluid rendering and uses its own optimized pipeline.
 * For waterlogged blocks, the FluidState passed to Sodium is always water, not our
 * custom hotBath fluid. This mixin intercepts the render method and replaces the
 * FluidState with the correct hotBath fluid from our waterlogging cache.</p>
 * 
 * <p>This works together with BaseFluidType.renderFluid() which tells Sodium to use
 * vanilla rendering for hotBath fluids. This mixin ensures the correct FluidState
 * is passed to that method.</p>
 */
@Mixin(value = FluidRendererImpl.class, remap = false)
public class FluidRendererImplMixin {

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
                BlockPos immutablePos = blockPos.immutable();
                Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(immutablePos);
                
                if (storedFluid != null && HotbathFluidHelper.isHotbathFluid(storedFluid)) {
                    hotbath$correctFluidState.set(storedFluid.defaultFluidState());
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
        return IClientFluidTypeExtensions.of(hotbath$getCorrectFluidState(original));
    }

    /**
     * Modify handler.renderFluid(fluidState, ...) to use our correct fluid.
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
            return IClientFluidTypeExtensions.of(correctFluidState);
        }
        return original;
    }
}
