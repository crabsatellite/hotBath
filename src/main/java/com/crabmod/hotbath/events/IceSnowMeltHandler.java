package com.crabmod.hotbath.events;

import com.crabmod.hotbath.HotBath;
import com.crabmod.hotbath.custom_fluid.CustomFluidBlockEntity;
import com.crabmod.hotbath.custom_fluid.DynamicCustomFluidBlock;
import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Handles melting of ice and snow blocks near hot bath liquids.
 * This is a core feature that works regardless of which mods are installed.
 * 
 * Features:
 * - Ice blocks within 3 blocks of hot bath melt into water
 * - Snow layers within 3 blocks of hot bath disappear
 * - Snow blocks within 3 blocks of hot bath disappear
 */
@Mod.EventBusSubscriber(modid = HotBath.MOD_ID)
public class IceSnowMeltHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Radius to check for ice/snow melting
    private static final int MELT_RADIUS = 3;
    
    // How often to check for ice melting (every 100 ticks = 5 seconds)
    private static final int MELT_CHECK_INTERVAL = 100;
    
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level.isClientSide()) return;
        if (!(event.level instanceof ServerLevel level)) return;
        
        // Only check periodically for performance
        if (level.getGameTime() % MELT_CHECK_INTERVAL != 0) return;
        
        // Melt ice and snow near hot baths
        meltIceAndSnowNearHotBaths(level);
    }
    
    /**
     * Find hot bath blocks near players and melt ice/snow around them.
     */
    private static void meltIceAndSnowNearHotBaths(ServerLevel level) {
        // Iterate through loaded chunks and find hot bath blocks
        // For performance, we limit the search to players' nearby areas
        for (ServerPlayer player : level.players()) {
            BlockPos playerPos = player.blockPosition();
            
            // Search in a reasonable area around the player
            for (int x = -16; x <= 16; x++) {
                for (int y = -8; y <= 8; y++) {
                    for (int z = -16; z <= 16; z++) {
                        BlockPos pos = playerPos.offset(x, y, z);
                        BlockState state = level.getBlockState(pos);
                        
                        // Check if this is a hot bath block
                        if (state.getBlock() instanceof AbstractHotbathBlock) {
                            // For DynamicCustomFluidBlock, check if it's actually hot
                            if (state.getBlock() instanceof DynamicCustomFluidBlock) {
                                BlockEntity be = level.getBlockEntity(pos);
                                if (be instanceof CustomFluidBlockEntity customBe) {
                                    // Only melt ice/snow if the fluid is hot (temperature >= 35°C)
                                    if (!customBe.isHot()) {
                                        continue; // Skip non-hot custom fluids
                                    }
                                }
                            }
                            // Melt ice/snow in radius around this hot bath
                            meltIceAndSnowAroundPosition(level, pos);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Melt ice and snow blocks around a hot bath position.
     */
    private static void meltIceAndSnowAroundPosition(ServerLevel level, BlockPos hotBathPos) {
        for (int x = -MELT_RADIUS; x <= MELT_RADIUS; x++) {
            for (int y = -MELT_RADIUS; y <= MELT_RADIUS; y++) {
                for (int z = -MELT_RADIUS; z <= MELT_RADIUS; z++) {
                    BlockPos checkPos = hotBathPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    // Melt ice to water
                    if (state.is(Blocks.ICE)) {
                        level.setBlockAndUpdate(checkPos, Blocks.WATER.defaultBlockState());
                        LOGGER.debug("Melted ice at {} near hot bath at {}", checkPos, hotBathPos);
                    }
                    // Remove snow layers
                    else if (state.is(Blocks.SNOW)) {
                        level.setBlockAndUpdate(checkPos, Blocks.AIR.defaultBlockState());
                        LOGGER.debug("Melted snow layer at {} near hot bath at {}", checkPos, hotBathPos);
                    }
                    // Remove snow blocks
                    else if (state.is(Blocks.SNOW_BLOCK)) {
                        level.setBlockAndUpdate(checkPos, Blocks.AIR.defaultBlockState());
                        LOGGER.debug("Melted snow block at {} near hot bath at {}", checkPos, hotBathPos);
                    }
                }
            }
        }
    }
}
