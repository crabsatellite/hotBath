package com.crabmod.hotbath.compat;

import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Custom BlockTemp for Hot Bath blocks
 * This integrates directly with Cold Sweat's temperature system
 */
public class HotBathTempModifier extends BlockTemp {
    
    // Temperature configuration constants
    public static final double BASE_TEMPERATURE = 0.25;  // Base warmth provided
    private static final double MAX_EFFECT = 2.0;        // Maximum effect to counteract cold
    private static final double RANGE = 7.0;             // Effective range in blocks
    
    private final double baseTemp;

    public HotBathTempModifier(double baseTemp, Block... blocks) {
        super(
            0.0,        // minEffect
            MAX_EFFECT, // maxEffect - high enough to counteract extreme cold
            Double.NEGATIVE_INFINITY,  // minTemperature (always active)
            Double.POSITIVE_INFINITY,  // maxTemperature (always active)
            RANGE,      // range in blocks
            true,       // fade with distance (Cold Sweat handles this)
            false,      // not logarithmic
            blocks
        );
        this.baseTemp = baseTemp;
    }

    @Override
    public double getTemperature(Level level, @Nullable LivingEntity entity, BlockState state, BlockPos pos, double distance) {
        // Return the fixed temperature - Cold Sweat handles distance fade automatically
        return baseTemp;
    }

    @Override
    public boolean isValid(Level level, BlockPos pos, BlockState state) {
        // Always valid for our hot bath blocks
        return true;
    }
}
