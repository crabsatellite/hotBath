package com.crabmod.hotbath.custom_fluid;

import com.crabmod.hotbath.dirtiness.DirtinessAttachment;
import com.crabmod.hotbath.dirtiness.DirtinessData;
import com.crabmod.hotbath.dirtiness.DirtinessNetworking;
import com.crabmod.hotbath.HotBathConfig;
import com.crabmod.hotbath.registers.EntityRegister;
import com.crabmod.hotbath.registers.ParticleRegister;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * Projectile entity for thrown custom fluid splash bottles.
 * When it hits, it applies effects to nearby entities based on
 * the custom fluid definition stored in the item.
 */
public class ThrownCustomFluidBottle extends ThrowableItemProjectile {

    public ThrownCustomFluidBottle(EntityType<? extends ThrowableItemProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownCustomFluidBottle(Level level, LivingEntity shooter) {
        super(EntityRegister.THROWN_CUSTOM_FLUID_BOTTLE.get(), shooter, level);
    }

    public ThrownCustomFluidBottle(Level level, double x, double y, double z) {
        super(EntityRegister.THROWN_CUSTOM_FLUID_BOTTLE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return CustomFluidItems.CUSTOM_FLUID_SPLASH_BOTTLE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.SPLASH_POTION_BREAK,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F,
                    this.random.nextFloat() * 0.1F + 0.9F);
            
            ItemStack stack = this.getItem();
            CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
            
            if (definition != null && stack.getItem() instanceof SplashCustomFluidBottleItem splashItem) {
                applySplash(splashItem, definition);
            }
            
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    private void applySplash(SplashCustomFluidBottleItem splashItem, CustomFluidDefinition definition) {
        AABB aabb = this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, aabb);

        for (LivingEntity entity : entities) {
            if (entity.isAffectedByPotions()) {
                double distSqr = this.distanceToSqr(entity);
                if (distSqr < 16.0D) {
                    splashItem.applySplashEffect(entity, definition);

                    // Reduce dirtiness when hit by splash (if enabled)
                    if (HotBathConfig.isDirtinessEnabled() && entity instanceof ServerPlayer serverPlayer) {
                        DirtinessData data = serverPlayer.getData(DirtinessAttachment.DIRTINESS);
                        long gameTime = serverPlayer.level().getGameTime();
                        data.reduceDirtiness(gameTime, 0.10f); // 10% reduction
                        DirtinessNetworking.syncToClient(serverPlayer);
                    }
                }
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ItemStack stack = this.getItem();
            CustomFluidDefinition definition = CustomFluidDataComponents.getFluidDefinition(stack);
            
            int color = definition != null ? definition.color() : 0x45E1E9;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            
            // Check if particles should be shown
            boolean showParticles = definition == null || definition.showParticles();

            // Effect particles using vanilla ENTITY_EFFECT with dynamic color (like vanilla potion)
            if (showParticles) {
            ColorParticleOption coloredParticle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, FastColor.ARGB32.colorFromFloat(1.0f, r, g, b));
            for (int k = 0; k < 100; ++k) {
                double radius = this.random.nextDouble() * 4.0D;
                double angle = this.random.nextDouble() * Math.PI * 2.0D;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                this.level().addParticle(coloredParticle,
                        this.getX() + offsetX * 0.1D,
                        this.getY() + 0.3D,
                        this.getZ() + offsetZ * 0.1D,
                        offsetX, 0.01D + this.random.nextDouble() * 0.5D, offsetZ);
            }
            }

            // Steam particles - only show for hot fluids (temperature >= threshold) and when particles enabled
            boolean isHot = definition != null && definition.isHot();
            boolean showSteam = showParticles && isHot && (definition == null || definition.showSteam());
            if (showSteam) {
                for (int k = 0; k < 10; ++k) {
                    double radius = this.random.nextDouble() * 0.5D;
                    double angle = this.random.nextDouble() * Math.PI * 2.0D;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;

                    this.level().addParticle(ParticleRegister.STEAM_PARTICLE.get(),
                            this.getX() + offsetX,
                            this.getY() + 0.2D,
                            this.getZ() + offsetZ,
                            0.0D, 0.1D + this.random.nextDouble() * 0.05D, 0.0D);
                }

                // Steam particles - Dispersed
                for (int k = 0; k < 15; ++k) {
                    double radius = 0.5D + this.random.nextDouble() * 2.0D;
                    double angle = this.random.nextDouble() * Math.PI * 2.0D;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;

                    this.level().addParticle(ParticleRegister.STEAM_PARTICLE.get(),
                            this.getX() + offsetX,
                            this.getY() + 0.1D,
                            this.getZ() + offsetZ,
                            0.0D, 0.02D + this.random.nextDouble() * 0.03D, 0.0D);
                }
            }

            // Splash particles are now part of the colored effect particles above
            // No need for separate splash particles since we use vanilla ENTITY_EFFECT

            // Item break particles
            for (int j = 0; j < 8; ++j) {
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack),
                        this.getX(), this.getY() + 0.2D, this.getZ(),
                        (this.random.nextFloat() - 0.5D) * 0.15D,
                        0.15D,
                        (this.random.nextFloat() - 0.5D) * 0.15D);
            }
        }
    }
}
