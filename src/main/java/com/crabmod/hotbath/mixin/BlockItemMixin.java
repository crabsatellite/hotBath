package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.DynamicFluidRegistry;
import com.crabmod.hotbath.util.HotbathFluidHelper;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for BlockItem to handle placing waterlogable blocks in hotBath fluids.
 * 
 * <p>Problem: Vanilla's getStateForPlacement only checks for Fluids.WATER, not FluidTags.WATER.
 * When placing a waterlogable block in hotBath fluid:
 * 1. Vanilla checks: fluidState.getType() == Fluids.WATER -> false
 * 2. Block is placed with WATERLOGGED=false
 * 3. The hotBath fluid block is replaced -> fluid disappears!
 * 
 * <p>Solution: 
 * 1. Before place(): record the fluid at the target position
 * 2. After place(): if it was a hotBath fluid and block supports waterlogging,
 *    set WATERLOGGED=true and store the fluid type
 */
@Mixin(BlockItem.class)
public class BlockItemMixin {
    
    // ThreadLocal to store fluid info between before/after hooks
    @Unique
    private static final ThreadLocal<FluidInfo> hotbath$pendingFluid = new ThreadLocal<>();
    
    @Unique
    private record FluidInfo(BlockPos pos, Fluid fluid, ResourceLocation customFluidId) {}
    
    /**
     * Before block placement, record the fluid at the target position.
     * Only records SOURCE fluids - flowing fluids should not waterlog blocks.
     */
    @Inject(method = "place", at = @At("HEAD"))
    private void hotbath$beforePlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) return;
        
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        FluidState fluidState = level.getFluidState(pos);
        Fluid fluid = fluidState.getType();
        
        // Record if it's a hotBath fluid AND it's a SOURCE (not flowing)
        // Only source fluids should waterlog blocks - flowing fluids should be destroyed
        if (HotbathFluidHelper.isHotbathFluid(fluid) && fluidState.isSource()) {
            Fluid sourceFluid = hotbath$getSourceFluid(fluid);
            
            // Check if it's a dynamic custom fluid and get its ID
            ResourceLocation customFluidId = null;
            if (hotbath$isDynamicCustomFluid(sourceFluid)) {
                customFluidId = hotbath$findCustomFluidIdFromSurrounding(level, pos);
            }
            
            hotbath$pendingFluid.set(new FluidInfo(pos, sourceFluid, customFluidId));
        } else {
            hotbath$pendingFluid.remove();
        }
    }
    
    /**
     * After block placement, if we recorded a hotBath fluid and the block supports
     * waterlogging, set WATERLOGGED=true and store the fluid type.
     */
    @Inject(method = "place", at = @At("RETURN"))
    private void hotbath$afterPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide()) return;
        
        FluidInfo fluidInfo = hotbath$pendingFluid.get();
        hotbath$pendingFluid.remove();
        
        if (fluidInfo == null) return;
        
        // Check if placement was successful
        InteractionResult result = cir.getReturnValue();
        if (result == InteractionResult.FAIL) return;
        
        Level level = context.getLevel();
        BlockPos pos = fluidInfo.pos();
        BlockState placedState = level.getBlockState(pos);
        
        // Check if placed block supports waterlogging
        if (!placedState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return;
        }
        
        // Check if block is already waterlogged (some blocks might handle it themselves)
        boolean isWaterlogged = placedState.getValue(BlockStateProperties.WATERLOGGED);
        
        if (!isWaterlogged) {
            // Vanilla set WATERLOGGED=false because it only checks Fluids.WATER
            // We need to fix this by setting WATERLOGGED=true
            BlockState newState = placedState.setValue(BlockStateProperties.WATERLOGGED, true);
            level.setBlock(pos, newState, 3);
        }
        
        // Store the hotBath fluid type
        HotbathWaterloggingHelper.storeFluidType(level, pos, fluidInfo.fluid());
        
        // If it's a dynamic custom fluid, also store the custom fluid ID
        if (fluidInfo.customFluidId() != null) {
            HotbathWaterloggingHelper.storeCustomFluidId(level, pos, fluidInfo.customFluidId());
        }
    }
    
    /**
     * Check if a fluid is the dynamic custom fluid (from data packs).
     */
    @Unique
    private static boolean hotbath$isDynamicCustomFluid(Fluid fluid) {
        Fluid sourceFluid = hotbath$getSourceFluid(fluid);
        return sourceFluid == DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()
                || sourceFluid == DynamicFluidRegistry.DYNAMIC_FLUID_FLOWING.get();
    }
    
    /**
     * Find the custom fluid ID from surrounding CustomFluidBlockEntity blocks.
     * Searches current position first, then adjacent positions.
     */
    @Unique
    private static ResourceLocation hotbath$findCustomFluidIdFromSurrounding(Level level, BlockPos pos) {
        // First check the current position (in case the fluid block is there)
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe && customBe.getFluidId() != null) {
            return customBe.getFluidId();
        }
        
        // Search adjacent positions for CustomFluidBlockEntity
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            BlockEntity adjacentBe = level.getBlockEntity(adjacent);
            if (adjacentBe instanceof CustomFluidBlockEntity customBe && customBe.getFluidId() != null) {
                return customBe.getFluidId();
            }
        }
        
        return null;
    }
    
    @Unique
    private static Fluid hotbath$getSourceFluid(Fluid fluid) {
        if (fluid instanceof FlowingFluid flowingFluid) {
            return flowingFluid.getSource();
        }
        return fluid;
    }
}
