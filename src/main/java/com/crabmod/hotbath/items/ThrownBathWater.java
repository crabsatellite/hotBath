package com.crabmod.hotbath.items;

import com.crabmod.hotbath.registers.EntityRegister;
import com.crabmod.hotbath.registers.ItemRegister;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class ThrownBathWater extends ThrowableItemProjectile {

    public ThrownBathWater(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownBathWater(Level level, LivingEntity shooter) {
        super(EntityRegister.THROWN_BATH_WATER.get(), shooter, level);
    }

    public ThrownBathWater(Level level, double x, double y, double z) {
        super(EntityRegister.THROWN_BATH_WATER.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ItemRegister.HOT_WATER_BOTTLE.get(); // Default fallback
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            ItemStack stack = this.getItem();
            if (stack.getItem() instanceof SplashBathWaterBottleItem splashItem) {
                applySplash(splashItem);
            }
            this.level().broadcastEntityEvent(this, (byte)3);
            this.discard();
        }
    }

    private void applySplash(SplashBathWaterBottleItem splashItem) {
        AABB aabb = this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, aabb);

        if (!list.isEmpty()) {
            for (LivingEntity livingentity : list) {
                if (livingentity.isAffectedByPotions()) {
                    double d0 = this.distanceToSqr(livingentity);
                    if (d0 < 16.0D) {
                        splashItem.applyEffect(livingentity);
                    }
                }
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ItemStack stack = this.getItem();
            SimpleParticleType particleType = ParticleTypes.SPLASH; // Default
            
            if (stack.getItem() instanceof SplashBathWaterBottleItem splashItem) {
                particleType = splashItem.getParticleType();
            }

            for(int i = 0; i < 8; ++i) {
                this.level().addParticle(particleType, this.getX(), this.getY(), this.getZ(), 
                    ((double)this.random.nextFloat() - 0.5D) * 0.08D, 
                    ((double)this.random.nextFloat() - 0.5D) * 0.08D, 
                    ((double)this.random.nextFloat() - 0.5D) * 0.08D);
            }
            
            // Also add item break particles
            for(int j = 0; j < 8; ++j) {
                 this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), this.getX(), this.getY(), this.getZ(), ((double)this.random.nextFloat() - 0.5D) * 0.15D, 0.15D, ((double)this.random.nextFloat() - 0.5D) * 0.15D);
            }
        }
    }
}
