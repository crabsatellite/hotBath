package com.crabmod.hotbath.fluid_blocks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public interface IInsideAreaTracker {
    public record InsideAreaResult(
            boolean isFirstEnter,
            boolean shouldProcess,
            int stayedTicks,
            int totalEnterCount
    ) {
    }

    default int reenterThresholdTicks() {
        return 10;
    }

    default InsideAreaResult trackInside(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();

        String key = getAreaKey();

        String LAST_INSIDE = key + "_LastInsideTick";
        String STAYED_TIME = key + "_StayedTime";
        String ENTER_COUNT = key + "_EnterCount";

        int currentTick = player.tickCount;
        int lastInsideTick = data.getInt(LAST_INSIDE);

        if (lastInsideTick == currentTick) {
            return new InsideAreaResult(
                    false,
                    false,
                    data.getInt(STAYED_TIME),
                    data.getInt(ENTER_COUNT)
            );
        }

        boolean isFirstEnter =
                lastInsideTick == 0
                        || currentTick < lastInsideTick
                        || (currentTick - lastInsideTick) > reenterThresholdTicks();

        int enterCount = data.getInt(ENTER_COUNT);
        int stayed;

        if (isFirstEnter) {
            enterCount++;
            stayed = 0;
            data.putInt(ENTER_COUNT, enterCount);
            data.putInt(STAYED_TIME, 0);
        } else {
            stayed = data.getInt(STAYED_TIME) + 1;
            data.putInt(STAYED_TIME, stayed);
        }

        data.putInt(LAST_INSIDE, currentTick);

        return new InsideAreaResult(
                isFirstEnter,
                true,
                stayed,
                enterCount
        );
    }


    default String getAreaKey() {
        return getClass().getSimpleName();
    }
}
