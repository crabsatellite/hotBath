package com.crabmod.hotbath.mixin.alexsmobs;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Mixin to increase Capuchin Monkey taming probability when in hot bath fluids.
 * Monkeys bathing in hot springs (like Japanese macaques!) are more relaxed
 * and easier to tame - 1.5x the normal taming chance.
 * 
 * Normal taming: 20% (1 in 5)
 * Hot spring taming: 30% (1.5x bonus)
 */
@Mixin(value = EntityCapuchinMonkey.class, remap = false)
public abstract class CapuchinTamingMixin {
    
    @Shadow
    public abstract boolean isTame();
    
    @Shadow
    public abstract void setTame(boolean tamed);
    
    @Shadow
    public abstract void setOwnerUUID(java.util.UUID ownerUUID);
    
    @Unique
    private static final Random HOTBATH_RANDOM = new Random();
    
    /**
     * Inject after mobInteract to provide bonus taming when in hot spring.
     * The normal taming is 1/5 (20%) chance, we add 10% more for 30% total (1.5x).
     */
    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void hotbath$bonusTamingOnInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        EntityCapuchinMonkey self = (EntityCapuchinMonkey) (Object) this;
        
        // Only process if not already tamed
        if (this.isTame()) return;
        
        // Check if monkey is in hot spring
        if (!hotbath$isInHotSpring(self)) return;
        
        // Check if player is using taming food
        ItemStack itemstack = player.getItemInHand(hand);
        if (!EntityCapuchinMonkey.isTameableFood(itemstack)) return;
        
        // Provide bonus taming attempt (10% additional chance for 30% total = 1.5x)
        // This only triggers if the normal 20% failed
        if (HOTBATH_RANDOM.nextFloat() < 0.125f) { // 1 - (0.8 * (1-0.125)) = 0.30 = 30%
            this.setTame(true);
            this.setOwnerUUID(player.getUUID());
            if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
                CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, self);
            }
            self.level().broadcastEntityEvent(self, (byte) 7);
        }
    }
    
    /**
     * Inject after onGetItem to provide bonus taming when catching thrown items in hot spring.
     * This handles the case where players throw items to the monkey.
     */
    @Inject(method = "onGetItem", at = @At("TAIL"))
    private void hotbath$bonusTamingOnGetItem(ItemEntity itemEntity, CallbackInfo ci) {
        EntityCapuchinMonkey self = (EntityCapuchinMonkey) (Object) this;
        
        // Only process if not already tamed
        if (this.isTame()) return;
        
        // Check if monkey is in hot spring
        if (!hotbath$isInHotSpring(self)) return;
        
        // Check if item is taming food (the original method already checked this)
        if (itemEntity.getOwner() == null) return;
        
        // Provide bonus taming attempt
        if (HOTBATH_RANDOM.nextFloat() < 0.125f) {
            this.setTame(true);
            this.setOwnerUUID(itemEntity.getOwner().getUUID());
            if (itemEntity.getOwner() instanceof ServerPlayer serverPlayer && !(serverPlayer instanceof FakePlayer)) {
                CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, self);
            }
            self.level().broadcastEntityEvent(self, (byte) 7);
        }
    }
    
    /**
     * Check if the monkey is currently in a hot spring fluid block.
     */
    @Unique
    private boolean hotbath$isInHotSpring(EntityCapuchinMonkey monkey) {
        BlockPos pos = monkey.blockPosition();
        return monkey.level().getBlockState(pos).getBlock() instanceof AbstractHotbathBlock;
    }
}
