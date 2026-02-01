package com.crabmod.hotbath.compat.twilightforest;

import com.crabmod.hotbath.fluid_details.FluidsColor;
import com.crabmod.hotbath.items.BathWaterEffects;
import com.crabmod.hotbath.registers.ItemRegister;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Helper class for mapping HotBath bath water colors and effects for Twilight Forest Flask integration.
 * This centralizes all color definitions to avoid hardcoding and ensure consistency with FluidsColor.
 */
public final class TFFlaskColorHelper {
    
    private TFFlaskColorHelper() {
        // Utility class, no instantiation
    }
    
    /**
     * Bath water types with their associated colors and effects.
     */
    public enum BathType {
        HOT_WATER(FluidsColor.HOT_WATER_COLOR, BathWaterEffects::hotWaterEffect, ItemRegister.HOT_WATER_BOTTLE),
        HONEY_BATH(FluidsColor.HONEY_BATH_COLOR, BathWaterEffects::honeyBathEffect, ItemRegister.HONEY_BATH_BOTTLE),
        MILK_BATH(FluidsColor.MILK_BATH_COLOR, BathWaterEffects::milkBathEffect, ItemRegister.MILK_BATH_BOTTLE),
        HERBAL_BATH(FluidsColor.HERBAL_BATH_COLOR, BathWaterEffects::herbalBathEffect, ItemRegister.HERBAL_BATH_BOTTLE),
        PEONY_BATH(FluidsColor.PEONY_BATH_COLOR, BathWaterEffects::peonyBathEffect, ItemRegister.PEONY_BATH_BOTTLE),
        ROSE_BATH(FluidsColor.ROSE_BATH_COLOR, BathWaterEffects::roseBathEffect, ItemRegister.ROSE_BATH_BOTTLE);
        
        private final int color;
        private final Consumer<LivingEntity> effectApplier;
        private final Supplier<Item> itemSupplier;
        
        BathType(int color, Consumer<LivingEntity> effectApplier, Supplier<Item> itemSupplier) {
            // FluidsColor includes alpha (0xFF000000), but PotionContents uses color without alpha
            this.color = color & 0x00FFFFFF;
            this.effectApplier = effectApplier;
            this.itemSupplier = itemSupplier;
        }
        
        public int getColor() {
            return color;
        }
        
        public void applyEffect(LivingEntity entity) {
            effectApplier.accept(entity);
        }
        
        public Item getItem() {
            return itemSupplier.get();
        }
    }
    
    // Cache for color -> BathType lookup (initialized at class load)
    private static final Map<Integer, BathType> COLOR_TO_TYPE;
    
    // Lazy-initialized cache for Item -> BathType lookup (must be lazy because Items aren't registered at class load)
    private static volatile Map<Item, BathType> ITEM_TO_TYPE;
    
    static {
        COLOR_TO_TYPE = new IdentityHashMap<>();
        for (BathType type : BathType.values()) {
            COLOR_TO_TYPE.put(type.getColor(), type);
        }
    }
    
    /**
     * Get or initialize the item-to-type cache.
     * Uses double-checked locking for thread-safe lazy initialization.
     */
    private static Map<Item, BathType> getItemToTypeCache() {
        if (ITEM_TO_TYPE == null) {
            synchronized (TFFlaskColorHelper.class) {
                if (ITEM_TO_TYPE == null) {
                    Map<Item, BathType> cache = new IdentityHashMap<>();
                    for (BathType type : BathType.values()) {
                        cache.put(type.getItem(), type);
                    }
                    ITEM_TO_TYPE = cache;
                }
            }
        }
        return ITEM_TO_TYPE;
    }
    
    /**
     * Get the BathType for a given color.
     * @param color The color value (without alpha)
     * @return Optional containing the BathType if found
     */
    public static Optional<BathType> getBathTypeByColor(int color) {
        // Ensure we're comparing without alpha
        int colorWithoutAlpha = color & 0x00FFFFFF;
        return Optional.ofNullable(COLOR_TO_TYPE.get(colorWithoutAlpha));
    }
    
    /**
     * Get the BathType for a legacy bath water bottle item.
     * Uses cached lookup for O(1) performance.
     * @param bottleItem The bottle item to check
     * @return Optional containing the BathType if it's a HotBath bottle
     */
    public static Optional<BathType> getBathTypeByBottle(Item bottleItem) {
        return Optional.ofNullable(getItemToTypeCache().get(bottleItem));
    }
    
    /**
     * Apply the bath effect for a given color.
     * @param entity The entity to apply the effect to
     * @param color The bath water color
     * @return true if an effect was applied
     */
    public static boolean applyEffectByColor(LivingEntity entity, int color) {
        Optional<BathType> bathType = getBathTypeByColor(color);
        bathType.ifPresent(type -> type.applyEffect(entity));
        return bathType.isPresent();
    }
    
    /**
     * Get the color for a legacy bath water bottle item.
     * @param bottleItem The bottle item
     * @return The color value, or -1 if not a HotBath bottle
     */
    public static int getColorForBottle(Item bottleItem) {
        return getBathTypeByBottle(bottleItem)
                .map(BathType::getColor)
                .orElse(-1);
    }
}
