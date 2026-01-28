package com.crabmod.hotbath.mixin.sodium;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
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
 * optimized fluid rendering pipeline. This mixin intercepts the render method and
 * replaces the fluidState with our hotBath fluid when appropriate.</p>
 * 
 * <p>We need to replace fluidState in multiple places:
 * <ul>
 *   <li>DefaultMaterials.forFluidState(fluidState) - to get correct material</li>
 *   <li>IClientFluidTypeExtensions.of(fluidState) - to get correct fluid handler</li>
 *   <li>handler.renderFluid(fluidState, ...) - to render correct fluid</li>
 *   <li>DefaultRenderContext.setUp(..., fluidState, ...) - to setup context correctly</li>
 * </ul>
 * </p>
 * 
 * <p>This mixin is only applied when Sodium is present, controlled by SodiumMixinPlugin.</p>
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.neoforge.render.FluidRendererImpl", remap = false)
public class FluidRendererImplMixin {

    /**
     * ThreadLocal to store the correct FluidState for the current render call.
     * This is used to pass information between the @Inject and @Redirect/@ModifyArg.
     */
    @Unique
    private static final ThreadLocal<FluidState> hotbath$correctFluidState = new ThreadLocal<>();

    /**
     * At the beginning of render, check if we need to substitute a different fluid.
     * Store the correct FluidState in a ThreadLocal for use by the redirects.
     * 
     * <p>Using @Coerce on parameters that are Sodium-specific types allows us to use Object
     * as the parameter type without needing a compile-time dependency on Sodium.</p>
     */
    @Inject(
        method = "render",
        at = @At("HEAD"),
        remap = false
    )
    private void hotbath$onRenderHead(@Coerce Object level, BlockState blockState, FluidState fluidState, 
            BlockPos blockPos, BlockPos offset, @Coerce Object collector, @Coerce Object buffers, CallbackInfo ci) {
        
        // Check if this block has WATERLOGGED property first
        if (blockState != null && blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            boolean isWaterlogged = blockState.getValue(BlockStateProperties.WATERLOGGED);
            
            if (isWaterlogged) {
                // Get the stored fluid type from our client cache
                Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidTypeClientDirect(blockPos);
                
                if (storedFluid != null && storedFluid != Fluids.EMPTY && storedFluid != Fluids.WATER) {
                    // Store the correct fluid state for use in the redirects
                    hotbath$correctFluidState.set(storedFluid.defaultFluidState());
                    return;
                }
            }
        }
        hotbath$correctFluidState.set(null);
    }

    /**
     * Clear the ThreadLocal after the render method completes.
     */
    @Inject(
        method = "render",
        at = @At("RETURN"),
        remap = false
    )
    private void hotbath$onRenderReturn(@Coerce Object level, BlockState blockState, FluidState fluidState, 
            BlockPos blockPos, BlockPos offset, @Coerce Object collector, @Coerce Object buffers, CallbackInfo ci) {
        hotbath$correctFluidState.remove();
    }

    /**
     * Modify the fluidState argument passed to DefaultMaterials.forFluidState().
     * This ensures the correct render material (e.g., translucent) is used.
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
    private FluidState hotbath$modifyForFluidStateArg(FluidState originalFluidState) {
        FluidState correctFluidState = hotbath$correctFluidState.get();
        return correctFluidState != null ? correctFluidState : originalFluidState;
    }

    /**
     * Redirect the IClientFluidTypeExtensions.of() call to return the handler for our custom fluid.
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
    private IClientFluidTypeExtensions hotbath$redirectGetFluidExtensions(FluidState originalFluidState) {
        FluidState correctFluidState = hotbath$correctFluidState.get();
        if (correctFluidState != null) {
            return IClientFluidTypeExtensions.of(correctFluidState);
        }
        return IClientFluidTypeExtensions.of(originalFluidState);
    }

    /**
     * Modify the fluidState argument passed to handler.renderFluid().
     * The method signature is: renderFluid(FluidState, BlockAndTintGetter, BlockPos, VertexConsumer, BlockState)
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
    private FluidState hotbath$modifyRenderFluidArg(FluidState originalFluidState) {
        FluidState correctFluidState = hotbath$correctFluidState.get();
        return correctFluidState != null ? correctFluidState : originalFluidState;
    }

    /**
     * Modify the fluidState argument passed to DefaultRenderContext.setUp().
     * Parameter index 4 is the fluidState (0: colorProviderRegistry, 1: renderer, 2: level, 3: blockState, 4: fluidState, ...)
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
    private FluidState hotbath$modifySetUpFluidStateArg(FluidState originalFluidState) {
        FluidState correctFluidState = hotbath$correctFluidState.get();
        return correctFluidState != null ? correctFluidState : originalFluidState;
    }
}
