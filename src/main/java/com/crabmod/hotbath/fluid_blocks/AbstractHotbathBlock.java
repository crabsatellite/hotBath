package com.crabmod.hotbath.fluid_blocks;

import com.crabmod.hotbath.util.CustomFluidHandler;
import com.crabmod.hotbath.util.ParticleGenerator;
import com.crabmod.hotbath.util.SoundHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public abstract class AbstractHotbathBlock extends LiquidBlock {
    private static final String HOTBATH_UNDERWATER_STATE = "HotbathUnderwaterState";
    private static final String HOTBATH_ENTER_WATER_STATE = "HotbathEnterWaterState";

    protected AbstractHotbathBlock(Supplier<? extends FlowingFluid> supplier, Properties properties) {
        super(supplier.get(), properties);
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
