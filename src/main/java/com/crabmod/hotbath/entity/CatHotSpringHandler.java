package com.crabmod.hotbath.entity;

import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for vanilla Cat hot spring sitting behavior.
 * Cats will sit near hot springs when idle, similar to how they sit on chests.
 */
@Mod.EventBusSubscriber(modid = "hotbath")
public class CatHotSpringHandler {
    
    private static long lastCleanup = 0;
    private static final int CLEANUP_INTERVAL = 1200; // 1 minute
    
    @SubscribeEvent
    public static void onCatTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Cat cat)) return;
        
        // Periodic cache cleanup
        long currentTime = cat.level().getGameTime();
        if (currentTime - lastCleanup > CLEANUP_INTERVAL) {
            lastCleanup = currentTime;
            HotSpringCatBehavior.cleanupCache(currentTime);
        }
        
        // Create adapter for vanilla Cat
        HotSpringCatBehavior.SittableEntity adapter = HotSpringCatBehavior.createCatAdapter(cat);
        
        // Process sitting behavior
        boolean isSitting = HotSpringCatBehavior.processCatTick(cat, adapter);
        
        // If not sitting, try to attract to hot spring
        if (!isSitting) {
            HotSpringCatBehavior.attractToHotSpring(cat, adapter);
        }
    }
}
