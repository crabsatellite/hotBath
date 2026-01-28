package com.crabmod.hotbath.mixin.alexsmobs;

import com.crabmod.hotbath.fluid_blocks.AbstractHotbathBlock;
import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Random;

/**
 * Mixin to increase raccoon taming probability when washing in hot bath fluids.
 * Raccoons have 1.5x the normal taming chance (45% instead of 30%) when washing
 * items in hot bath fluids.
 */
@Mixin(value = EntityRaccoon.class, remap = false)
public abstract class RaccoonTamingMixin {
    
    @Shadow
    public abstract Optional<BlockPos> getWashPos();
    
    @Shadow
    public abstract boolean isTame();
    
    @Shadow
    public abstract void setTame(boolean tamed, boolean updateOwner);
    
    @Shadow
    public abstract void setOwnerUUID(java.util.UUID ownerUUID);
    
    @Shadow
    public java.util.UUID eggThrowerUUID;
    
    @Unique
    private static final Random HOTBATH_RANDOM = new Random();
    
    /**
     * Injects after the normal taming check fails to provide a bonus taming attempt.
     * If the raccoon is washing in a hot bath fluid and the normal 30% taming failed,
     * we give an additional chance for a total effective rate of 1.5x.
     */
    @Inject(method = "postWashItem", at = @At("TAIL"))
    private void hotbath$bonusTamingChance(ItemStack stack, CallbackInfo ci) {
        EntityRaccoon self = (EntityRaccoon) (Object) this;
        
        // Only process if not already tamed and has a potential owner
        if (this.isTame() || this.eggThrowerUUID == null) return;
        
        // Check if washing in hot bath fluid
        Optional<BlockPos> washPosOpt = this.getWashPos();
        if (washPosOpt.isEmpty()) return;
        
        BlockPos washPos = washPosOpt.get();
        if (!(self.level().getBlockState(washPos).getBlock() instanceof AbstractHotbathBlock)) return;
        
        // Provide bonus taming attempt (21.43% additional chance after normal fails)
        // This gives exactly 1.5x total: 1 - (0.7 * (1-0.2143)) = 0.45
        if (HOTBATH_RANDOM.nextFloat() < 0.2143f) {
            this.setTame(true, true);
            this.setOwnerUUID(eggThrowerUUID);
            Player player = self.level().getPlayerByUUID(eggThrowerUUID);
            if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
                net.minecraft.advancements.CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, self);
            }
            self.level().broadcastEntityEvent(self, (byte) 7);
        }
    }
}
