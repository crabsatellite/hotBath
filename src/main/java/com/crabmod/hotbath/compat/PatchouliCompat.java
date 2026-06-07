package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.HotBath;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * Compatibility handler for Patchouli guide book mod.
 * Provides the Hot Bath Guide book when Patchouli is loaded.
 */
public class PatchouliCompat {
    
    public static final String PATCHOULI_MOD_ID = "patchouli";
    public static final ResourceLocation BOOK_ID = new ResourceLocation(HotBath.MOD_ID, "hotbath_guide");
    
    /**
     * Checks if Patchouli mod is loaded.
     * @return true if Patchouli is available
     */
    public static boolean isPatchouliLoaded() {
        return ModList.get().isLoaded(PATCHOULI_MOD_ID);
    }
    
    /**
     * Gets the Hot Bath Guide book ItemStack.
     * Only call this if isPatchouliLoaded() returns true.
     * @return ItemStack of the guide book, or empty if Patchouli is not available
     */
    public static ItemStack getGuideBook() {
        if (!isPatchouliLoaded()) {
            return ItemStack.EMPTY;
        }
        try {
            return PatchouliAPIHelper.getBookStack(BOOK_ID);
        } catch (Throwable e) {
            HotBath.LOGGER.warn("Failed to get Patchouli guide book: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Sets a config flag in Patchouli for controlling page/entry visibility.
     * Call this to update compat disabled status flags.
     * @param flagName The flag name (e.g., "hotbath:cold_sweat_disabled")
     * @param value The flag value
     */
    public static void setConfigFlag(String flagName, boolean value) {
        if (!isPatchouliLoaded()) {
            return;
        }
        try {
            PatchouliAPIHelper.setConfigFlag(flagName, value);
            HotBath.LOGGER.debug("Set Patchouli flag {} = {}", flagName, value);
        } catch (Throwable e) {
            HotBath.LOGGER.warn("Failed to set Patchouli flag {}: {}", flagName, e.getMessage());
        }
    }
    
    /**
     * Update all compat disabled flags in Patchouli.
     * Should be called after compat initialization.
     */
    public static void updateCompatFlags() {
        if (!isPatchouliLoaded()) {
            return;
        }
        
        // Set disabled flags for each compat module
        String[] compatModIds = {
            "cold_sweat", "toughasnails", "legendarysurvivaloverhaul",
            "alexsmobs", "alexscaves", "farmersdelight", 
            "twilightforest", "sereneseasons", "create", "epicfight"
        };
        
        for (String modId : compatModIds) {
            boolean isDisabled = !CompatManager.isCompatEnabled(modId);
            boolean isLoaded = ModList.get().isLoaded(modId);
            
            // Set flag: hotbath:<modid>_disabled = true when compat is loaded but disabled due to error
            setConfigFlag("hotbath:" + modId + "_disabled", isLoaded && isDisabled);
            
            // Set flag: hotbath:<modid>_working = true when compat is loaded and working
            setConfigFlag("hotbath:" + modId + "_working", isLoaded && !isDisabled);
        }
        
        HotBath.LOGGER.info("Updated Patchouli compat flags");
    }
    
    /**
     * Helper class to isolate Patchouli API calls.
     * This prevents ClassNotFoundException if Patchouli is not loaded.
     */
    private static class PatchouliAPIHelper {
        static ItemStack getBookStack(ResourceLocation bookId) {
            return vazkii.patchouli.api.PatchouliAPI.get().getBookStack(bookId);
        }
        
        static void setConfigFlag(String flag, boolean value) {
            vazkii.patchouli.api.PatchouliAPI.get().setConfigFlag(flag, value);
        }
    }
}
