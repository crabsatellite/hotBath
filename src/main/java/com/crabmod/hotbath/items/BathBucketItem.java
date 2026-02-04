package com.crabmod.hotbath.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Bath bucket with custom tooltip.
 * This class overrides emptyContents to properly call placeLiquid for hotBath fluids,
 * since vanilla BucketItem only calls placeLiquid for Fluids.WATER.
 */
public class BathBucketItem extends BucketItem {
    private final Supplier<? extends Fluid> fluidSupplier;
    
    public BathBucketItem(Supplier<? extends Fluid> supplier, Properties properties) {
        super(supplier, properties);
        this.fluidSupplier = supplier;
    }

    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable net.minecraft.nbt.CompoundTag nbt) {
        return new net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper(stack);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
    
    /**
     * Override emptyContents to properly handle hotBath fluids.
     * The vanilla implementation only calls placeLiquid for Fluids.WATER,
     * but we need it to work for all hotBath fluids.
     */
    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hitResult) {
        Fluid content = this.fluidSupplier.get();
        
        if (!(content instanceof FlowingFluid flowingFluid)) {
            return false;
        }
        
        BlockState blockState = level.getBlockState(pos);
        Block block = blockState.getBlock();
        boolean canReplace = blockState.canBeReplaced(content);
        
        // Check if we can place liquid here
        boolean canPlace;
        if (!blockState.isAir() && !canReplace) {
            // Check if block can accept this liquid via LiquidBlockContainer
            if (block instanceof LiquidBlockContainer container 
                    && container.canPlaceLiquid(level, pos, blockState, content)) {
                canPlace = true;
            } else {
                canPlace = false;
            }
        } else {
            canPlace = true;
        }
        
        if (!canPlace) {
            // Try to place at adjacent position
            return hitResult != null && this.emptyContents(player, level, hitResult.getBlockPos().relative(hitResult.getDirection()), null);
        }
        
        // Handle nether evaporation
        if (level.dimensionType().ultraWarm() && content.is(FluidTags.WATER)) {
            playEvaporationEffects(level, pos, player);
            return true;
        }
        
        // KEY FIX: Call placeLiquid for LiquidBlockContainer blocks (not just for WATER)
        if (block instanceof LiquidBlockContainer container 
                && container.canPlaceLiquid(level, pos, blockState, content)) {
            container.placeLiquid(level, pos, blockState, flowingFluid.getSource(false));
            this.playEmptySound(player, level, pos);
            return true;
        }
        
        // Regular fluid placement (replacing blocks)
        if (!level.isClientSide && canReplace && !blockState.liquid()) {
            level.destroyBlock(pos, true);
        }
        
        if (!level.setBlock(pos, content.defaultFluidState().createLegacyBlock(), 11) 
                && !blockState.getFluidState().isSource()) {
            return false;
        } else {
            this.playEmptySound(player, level, pos);
            return true;
        }
    }
    
    private void playEvaporationEffects(Level level, BlockPos pos, @Nullable Player player) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        level.playSound(
                player,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
        );
        for (int i = 0; i < 8; i++) {
            level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    (double) x + Math.random(),
                    (double) y + Math.random(),
                    (double) z + Math.random(),
                    0.0, 0.0, 0.0
            );
        }
    }
    
    @Override
    protected void playEmptySound(@Nullable Player player, LevelAccessor level, BlockPos pos) {
        Fluid content = this.fluidSupplier.get();
        SoundEvent sound = content.getFluidType().getSound(player, level, pos, net.minecraftforge.common.SoundActions.BUCKET_EMPTY);
        if (sound == null) {
            sound = content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        }
        level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    }
}











