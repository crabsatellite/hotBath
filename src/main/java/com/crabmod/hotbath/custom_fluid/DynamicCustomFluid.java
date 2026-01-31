package com.crabmod.hotbath.custom_fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.jetbrains.annotations.NotNull;

/**
 * Custom FlowingFluid implementation that propagates BlockEntity data when spreading.
 * This ensures that flowing fluid blocks inherit the color and fluid ID from their source.
 */
public abstract class DynamicCustomFluid extends BaseFlowingFluid {

    protected DynamicCustomFluid(Properties properties) {
        super(properties);
    }

    /**
     * Override spreadTo to copy BlockEntity data from source to new flowing block.
     * This ensures flowing fluid maintains the same color as the source.
     */
    @Override
    protected void spreadTo(@NotNull LevelAccessor level, @NotNull BlockPos pos, 
                            @NotNull BlockState blockState, @NotNull Direction direction, 
                            @NotNull FluidState fluidState) {
        // Get the source position (where the fluid is spreading FROM)
        BlockPos sourcePos = pos.relative(direction.getOpposite());
        
        // Get fluid data from the source block
        ResourceLocation fluidId = null;
        BlockEntity sourceBe = level.getBlockEntity(sourcePos);
        if (sourceBe instanceof CustomFluidBlockEntity sourceFluidBe) {
            fluidId = sourceFluidBe.getFluidId();
        }
        
        // Call parent to place the fluid block
        super.spreadTo(level, pos, blockState, direction, fluidState);
        
        // Copy the fluid ID to the new block's BlockEntity
        if (fluidId != null) {
            BlockEntity newBe = level.getBlockEntity(pos);
            if (newBe instanceof CustomFluidBlockEntity newFluidBe) {
                newFluidBe.setFluidId(fluidId);
            }
        }
    }

    /**
     * Source (still) version of the dynamic custom fluid.
     */
    public static class Source extends DynamicCustomFluid {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getAmount(@NotNull FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(@NotNull FluidState state) {
            return true;
        }
    }

    /**
     * Flowing version of the dynamic custom fluid.
     */
    public static class Flowing extends DynamicCustomFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any().setValue(LEVEL, 7));
        }

        @Override
        protected void createFluidStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.material.Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(@NotNull FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(@NotNull FluidState state) {
            return false;
        }
    }
}
