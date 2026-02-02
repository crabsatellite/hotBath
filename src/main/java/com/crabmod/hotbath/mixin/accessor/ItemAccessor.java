package com.crabmod.hotbath.mixin.accessor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin to expose protected methods from Item class.
 */
@Mixin(Item.class)
public interface ItemAccessor {
    
    /**
     * Accessor for the protected getPlayerPOVHitResult method.
     * This is needed to perform raycasts in the same way as vanilla Item classes.
     */
    @Invoker("getPlayerPOVHitResult")
    static BlockHitResult invokeGetPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid fluidMode) {
        throw new AssertionError();
    }
}
