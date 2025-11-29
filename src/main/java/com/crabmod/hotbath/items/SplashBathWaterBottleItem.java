package com.crabmod.hotbath.items;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SplashBathWaterBottleItem extends Item {
    private final Consumer<LivingEntity> drinkEffect;
    private final Supplier<SimpleParticleType> particleType;
    private final Supplier<SimpleParticleType> bubbleParticleType;

    public SplashBathWaterBottleItem(Properties properties, Consumer<LivingEntity> drinkEffect, Supplier<SimpleParticleType> particleType, Supplier<SimpleParticleType> bubbleParticleType) {
        super(properties);
        this.drinkEffect = drinkEffect;
        this.particleType = particleType;
        this.bubbleParticleType = bubbleParticleType;
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                net.minecraft.sounds.SoundEvents.SPLASH_POTION_THROW, 
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        
        if (!level.isClientSide) {
            ThrownBathWater thrownBathWater = new ThrownBathWater(level, player);
            thrownBathWater.setItem(itemstack);
            thrownBathWater.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(thrownBathWater);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            itemstack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    public void applyEffect(LivingEntity entity) {
        if (drinkEffect != null) {
            drinkEffect.accept(entity);
        }
    }

    public SimpleParticleType getParticleType() {
        return particleType.get();
    }

    public SimpleParticleType getBubbleParticleType() {
        return bubbleParticleType.get();
    }
}
