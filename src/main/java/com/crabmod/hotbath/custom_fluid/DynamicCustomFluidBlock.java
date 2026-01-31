package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.crabmod.hotbath.fluid_blocks.IInsideAreaTracker;
import com.crabmod.hotbath.util.ParticleGenerator;
import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A dynamic custom fluid block that uses BlockEntity to store fluid data from data packs.
 * This allows each placed fluid block to have its own unique fluid definition.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Stores fluid ID in BlockEntity for per-block customization</li>
 *   <li>Generates steam particles only when fluid is hot (temperature >= threshold)</li>
 *   <li>Applies effects from the stored fluid definition</li>
 *   <li>Supports tinted rendering based on fluid color</li>
 *   <li>Recovers custom fluid ID from waterlogging storage when created from broken waterlogged blocks</li>
 * </ul>
 */
public class DynamicCustomFluidBlock extends AbstractHotbathBlock implements EntityBlock, IInsideAreaTracker {
    
    private static final int TICKS_PER_SECOND = 20;

    public DynamicCustomFluidBlock(Supplier<? extends FlowingFluid> fluidSupplier, Properties properties) {
        super(fluidSupplier, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CustomFluidBlockEntity(pos, state);
    }
    
    /**
     * Called when this block is placed in the world.
     * Checks if there was a stored custom fluid ID in waterlogging storage at this position,
     * which happens when a waterlogged block is broken and releases the fluid.
     */
    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, 
                        @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        
        if (level.isClientSide) {
            return;
        }
        
        // Check if there's a stored custom fluid ID from waterlogging at this position
        // This handles the case when a waterlogged block is broken and fluid is released
        ResourceLocation customFluidId = HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos);
        if (customFluidId != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CustomFluidBlockEntity customBe) {
                // Only set if the BlockEntity doesn't already have a fluid ID
                if (customBe.getFluidId() == null) {
                    customBe.setFluidId(customFluidId);
                }
            }
            // Clean up the waterlogging storage since we've moved the fluid to a regular block
            HotbathWaterloggingHelper.removeFluidType(level, pos);
            HotbathWaterloggingHelper.removeFromClientCache(pos);
        }
    }

    /**
     * Override pickupBlock to return the correct custom fluid bucket.
     * This is called when a player uses an empty bucket on the fluid block.
     */
    @Override
    public @NotNull ItemStack pickupBlock(@Nullable Player player, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockState state) {
        // Only allow pickup of source blocks (level 0)
        if (state.getValue(LEVEL) != 0) {
            return ItemStack.EMPTY;
        }

        // Get fluid definition from BlockEntity before removing it
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            Optional<CustomFluidDefinition> definitionOpt = customBe.getFluidDefinition();
            if (definitionOpt.isPresent()) {
                // Remove the fluid block
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                // Return the filled custom fluid bucket
                return CustomFluidAPI.createBucket(definitionOpt.get());
            }
        }

        // Fallback to parent behavior if no definition found
        return super.pickupBlock(player, level, pos, state);
    }

    /**
     * Override getPickupSound to return the correct sound for bucket pickup.
     */
    @Override
    public @NotNull Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }

    /**
     * Gets the fluid definition from the BlockEntity at the given position.
     */
    public Optional<CustomFluidDefinition> getFluidDefinition(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            return customBe.getFluidDefinition();
        }
        return Optional.empty();
    }

    @Override
    public void entityInside(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity) {
        super.entityInside(state, level, pos, entity);
        
        if (level.isClientSide) {
            return;
        }
        
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        
        if (!player.isAlive()) {
            return;
        }
        
        // Get fluid definition from BlockEntity
        Optional<CustomFluidDefinition> definitionOpt = getFluidDefinition(level, pos);
        if (definitionOpt.isEmpty()) {
            return;
        }
        
        CustomFluidDefinition definition = definitionOpt.get();
        
        InsideAreaResult result = trackInside(player);
        
        if (!result.shouldProcess()) {
            return;
        }
        
        int stayedTicks = result.stayedTicks();
        int triggerTicks = definition.triggerTimeSeconds() * TICKS_PER_SECOND;
        
        // Apply effects after the trigger time has passed
        if (stayedTicks >= triggerTicks) {
            applyEffects(player, definition);
        }
    }

    /**
     * Applies all configured effects to the player.
     */
    private void applyEffects(ServerPlayer player, CustomFluidDefinition definition) {
        List<MobEffectInstance> effects = definition.createEffectInstances();
        for (MobEffectInstance effect : effects) {
            player.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(
            @NotNull BlockState stateIn,
            @NotNull Level worldIn,
            @NotNull BlockPos pos,
            @NotNull RandomSource rand) {
        
        // Call parent for bubble column and underwater effects
        super.animateTick(stateIn, worldIn, pos, rand);
        
        // Get fluid definition from BlockEntity
        BlockEntity be = worldIn.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            // Generate steam particles only for hot fluids
            if (customBe.isHot()) {
                generateSteamParticles(worldIn, pos, rand);
            }
        }
    }

    /**
     * Generates steam particles at adjacent air blocks.
     */
    private void generateSteamParticles(Level worldIn, BlockPos pos, RandomSource rand) {
        BlockPos[] adjacentPositions = new BlockPos[]{
                pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()
        };

        int airBlockCount = 0;
        BlockPos[] airBlocks = new BlockPos[adjacentPositions.length];

        for (BlockPos adjacentPos : adjacentPositions) {
            if (worldIn.getBlockState(adjacentPos).isAir()) {
                airBlocks[airBlockCount++] = adjacentPos;
            }
        }

        if (airBlockCount > 0 && rand.nextInt(5) == 0) {
            BlockPos selectedPos = airBlocks[rand.nextInt(airBlockCount)];
            ParticleGenerator.renderDefaultSteam((ClientLevel) worldIn, selectedPos, rand);
        }
    }

    /**
     * Gets the tint color for this fluid block at the given position.
     * Used by the rendering system to apply color to the grayscale texture.
     */
    public int getTintColor(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CustomFluidBlockEntity customBe) {
            return customBe.getFluidColor();
        }
        return 0x45E1E9; // Default cyan
    }
}
