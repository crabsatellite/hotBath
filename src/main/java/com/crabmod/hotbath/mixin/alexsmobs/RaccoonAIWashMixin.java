package com.crabmod.hotbath.mixin.alexsmobs;

import com.crabmod.hotbath.custom_fluid.DynamicCustomFluidBlock;
import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import com.github.alexthe666.alexsmobs.entity.ai.RaccoonAIWash;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to speed up raccoon washing in hot bath fluids.
 * Raccoons wash 1.5x faster when using hot bath fluids instead of regular water.
 */
@Mixin(value = RaccoonAIWash.class, remap = false)
public class RaccoonAIWashMixin {
    
    @Shadow
    @Final
    private EntityRaccoon raccoon;
    
    @Shadow
    private BlockPos waterPos;
    
    @Shadow
    private int washTime;
    
    // Cache to avoid repeated block state lookups
    @Unique
    private Boolean hotbath$isInHotBath = null;
    
    @Unique
    private BlockPos hotbath$lastWaterPos = null;
    
    /**
     * Injects at the end of tick() to add extra wash time progress when in hot bath fluids.
     * This effectively makes washing 1.5x faster.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void hotbath$boostWashSpeed(CallbackInfo ci) {
        // Only process if raccoon is actively washing
        if (!raccoon.isWashing() || waterPos == null) return;
        
        // Only check block state if position changed (caching)
        if (!waterPos.equals(hotbath$lastWaterPos)) {
            hotbath$lastWaterPos = waterPos;
            var block = raccoon.level().getBlockState(waterPos).getBlock();
            hotbath$isInHotBath = block instanceof AbstractHotbathBlock || block instanceof DynamicCustomFluidBlock;
        }
        
        // Apply speed bonus if in hot bath
        if (Boolean.TRUE.equals(hotbath$isInHotBath) && raccoon.tickCount % 2 == 0) {
            washTime++;
        }
    }
    
    /**
     * Reset cache when goal stops.
     */
    @Inject(method = "stop", at = @At("HEAD"))
    private void hotbath$resetCache(CallbackInfo ci) {
        hotbath$isInHotBath = null;
        hotbath$lastWaterPos = null;
    }
}
