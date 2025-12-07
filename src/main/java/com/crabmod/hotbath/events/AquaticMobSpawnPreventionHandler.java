package com.crabmod.hotbath.events;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.living.MobSpawnEvent;

import static com.crabmod.hotbath.HotBath.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class AquaticMobSpawnPreventionHandler {

    @SubscribeEvent
    public static void onCheckSpawn(MobSpawnEvent.PositionCheck event) {
        LevelAccessor world = event.getLevel();
        BlockPos pos = event.getEntity().blockPosition();

        // Check if the spawning location is a HotBath block
        boolean isHotBathBlock = world.getBlockState(pos).getBlock() instanceof AbstractHotbathBlock;

        // If the spawning location is a HotBath block, cancel the spawn event
        if (isHotBathBlock) {
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
        }
    }
}










