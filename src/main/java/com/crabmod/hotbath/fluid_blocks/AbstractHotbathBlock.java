package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.util.CustomFluidHandler;
import com.crabmod.hotbath.util.ParticleGenerator;
import com.crabmod.hotbath.util.SoundHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import com.crabmod.hotbath.fluid_details.BaseFluidType;
import net.neoforged.neoforge.fluids.FluidType;

public abstract class AbstractHotbathBlock extends LiquidBlock {
    private static final String HOTBATH_UNDERWATER_STATE = "HotbathUnderwaterState";
    private static final String HOTBATH_ENTER_WATER_STATE = "HotbathEnterWaterState";

    protected AbstractHotbathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier.get(), properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        
        // Bubble column physics
        int direction = getBubbleColumnDirection(level, pos);
        if (direction != 0) {
            boolean dragDown = direction < 0;
            BlockState stateAbove = level.getBlockState(pos.above());
            if (stateAbove.isAir()) {
                entity.onAboveBubbleCol(dragDown);
            } else {
                entity.onInsideBubbleColumn(dragDown);
            }
        }
    }

    private int getBubbleColumnDirection(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos mutablePos = pos.mutable();
        FluidType currentFluidType = level.getFluidState(pos).getFluidType();
        
        // Limit scan to avoid lag, but allow deep oceans
        for (int i = 0; i < 384; i++) {
            mutablePos.move(Direction.DOWN);
            BlockState state = level.getBlockState(mutablePos);
            if (state.is(Blocks.SOUL_SAND)) return 1;
            if (state.is(Blocks.MAGMA_BLOCK)) return -1;
            
            // Stop if we hit a solid block or a different fluid
            if (!state.is(this) && state.getFluidState().getFluidType() != currentFluidType) return 0;
        }
        return 0;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(
            @NotNull BlockState stateIn,
            @NotNull Level worldIn,
            @NotNull BlockPos pos,
            @NotNull RandomSource rand) {

        // Generate steam particles at random adjacent air blocks
        generateSteamParticles(worldIn, pos, rand);

        // Bubble column particles
        int direction = getBubbleColumnDirection(worldIn, pos);
        if (direction != 0) {
            if (direction > 0) {
                 ParticleOptions bubbleParticle = null;
                 FluidType fluidType = stateIn.getFluidState().getFluidType();
                 if (fluidType instanceof BaseFluidType baseFluidType) {
                     bubbleParticle = baseFluidType.getBubbleParticle();
                 }
                 if (bubbleParticle == null) bubbleParticle = ParticleTypes.BUBBLE_COLUMN_UP;

                 worldIn.addParticle(bubbleParticle, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0D, 0.04D, 0.0D);
                 if (rand.nextInt(200) == 0) {
                     worldIn.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, net.minecraft.sounds.SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
                 }
            } else {
                 worldIn.addParticle(ParticleTypes.CURRENT_DOWN, pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D, 0.0D, -0.04D, 0.0D);
                 if (rand.nextInt(200) == 0) {
                     worldIn.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, net.minecraft.sounds.SoundSource.BLOCKS, 0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
                 }
            }
        }

        // Set the maximum distance in squared units to avoid unnecessary checks
        final double maxDistanceSqr = 3.0 * 3.0;

        // Client-side detection for players being underwater or entering/exiting the water
        for (var player : worldIn.players()) {
            // Calculate the squared distance between the player and the block position
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
                    > maxDistanceSqr) {
                continue; // Skip if the player is too far from the hot bath block
            }

            // Check if the player's head is underwater, if so, don't play ambient sound
            boolean isPlayerHeadUnderwater = CustomFluidHandler.isPlayerHeadInHotBath(player);
            if (!isPlayerHeadUnderwater) {
                // Play ambient water sound only if the player is not completely underwater
                SoundHandler.playAmbientWaterSound(worldIn, pos, rand);
            }

            // Only handle underwater and water entry/exit states if the player is within range
            handleClientPlayerUnderwaterState(player, rand);
            handleClientPlayerEnterWaterState(player, rand);
        }
    }

    // Handle the logic for when a player enters the water for the first time and exits completely
    private void handleClientPlayerEnterWaterState(Player player, RandomSource rand) {
        CompoundTag playerData = player.getPersistentData();

        // Check if the player's feet are in hot bath fluid (not necessarily the head)
        boolean isPlayerInWater = CustomFluidHandler.isPlayerInHotBathBlock(player);

        if (isPlayerInWater) {
            // If the player is in water and hasn't triggered the entry sound yet
            if (!playerData.getBoolean(HOTBATH_ENTER_WATER_STATE)) {
                SoundHandler.playEnterWaterSound(player, rand); // Play entry sound
                playerData.putBoolean(
                        HOTBATH_ENTER_WATER_STATE, true); // Set the player as having entered water
            }
        } else {
            // If the player has exited the water, reset the state to allow entry sound to play again
            if (playerData.getBoolean(HOTBATH_ENTER_WATER_STATE)) {
                playerData.putBoolean(HOTBATH_ENTER_WATER_STATE, false); // Reset the entry state
            }
        }
    }

    // Handle the underwater state of the player, controlling sound for entering and exiting
    // underwater
    private void handleClientPlayerUnderwaterState(Player player, RandomSource rand) {
        CompoundTag playerData = player.getPersistentData();

        // Check if the player's head is in the hot bath fluid (completely underwater)
        boolean isPlayerHeadUnderwater = CustomFluidHandler.isPlayerHeadInHotBath(player);

        if (isPlayerHeadUnderwater) {
            // If the player just went underwater
            if (!playerData.getBoolean(HOTBATH_UNDERWATER_STATE)) {
                SoundHandler.playUnderwaterEnterSound(player, rand); // Play underwater entry sound
                playerData.putBoolean(HOTBATH_UNDERWATER_STATE, true); // Mark the player as underwater
            } else {
                SoundHandler.playUnderwaterLoopSound(player, rand); // Play underwater loop sound
            }
        } else {
            // If the player has exited from being underwater
            if (playerData.getBoolean(HOTBATH_UNDERWATER_STATE)) {
                SoundHandler.playExitWaterSound(player, rand); // Play exit water sound
                SoundHandler.stopUnderwaterLoopSound(player); // Stop underwater loop sound
                playerData.putBoolean(HOTBATH_UNDERWATER_STATE, false); // Reset the underwater state
            }
        }
    }

    // Generate steam particles around the block if adjacent blocks are air
    private void generateSteamParticles(Level worldIn, BlockPos pos, RandomSource rand) {
        BlockPos[] adjacentPositions =
                new BlockPos[]{pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()};

        int airBlockCount = 0;
        BlockPos[] airBlocks = new BlockPos[adjacentPositions.length];

        // Check for air blocks adjacent to the current position
        for (BlockPos adjacentPos : adjacentPositions) {
            if (worldIn.getBlockState(adjacentPos).isAir()) {
                airBlocks[airBlockCount++] = adjacentPos;
            }
        }

        // If there are air blocks, randomly select one and generate steam particles
        if (airBlockCount > 0) {
            BlockPos selectedPos = airBlocks[rand.nextInt(airBlockCount)];
            ParticleGenerator.renderDefaultSteam((ClientLevel) worldIn, selectedPos, rand);
        }
    }
}
