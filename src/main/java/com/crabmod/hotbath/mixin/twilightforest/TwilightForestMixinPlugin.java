package com.crabmod.hotbath.mixin.twilightforest;

import com.crabmod.hotbath.mixin.HotBathMixinPlugin;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads Twilight Forest mixins only when:
 * 1. The mod is present
 * 2. Compatibility mode is NOT enabled
 * 
 * This allows HotBath bath water bottles and custom fluid bottles to be
 * placed into Twilight Forest's Brittle Flask and Greater Flask items.
 */
public class TwilightForestMixinPlugin implements IMixinConfigPlugin {
    
    private static Boolean twilightForestLoaded = null;
    
    private static boolean checkTwilightForestPresent() {
        if (twilightForestLoaded == null) {
            try {
                twilightForestLoaded = FMLLoader.getLoadingModList().getModFileById("twilightforest") != null;
                if (twilightForestLoaded) {
                    System.out.println("[HotBath] Twilight Forest detected via FMLLoader - Flask integration mixins enabled");
                }
            } catch (Throwable t) {
                twilightForestLoaded = false;
                System.out.println("[HotBath] Error checking for Twilight Forest: " + t.getMessage());
            }
        }
        return twilightForestLoaded;
    }
    
    @Override
    public void onLoad(String mixinPackage) {
        checkTwilightForestPresent();
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Disable if compatibility mode is enabled
        if (HotBathMixinPlugin.isCompatibilityModeEnabled()) {
            return false;
        }
        // Only apply mixins if Twilight Forest is loaded
        return checkTwilightForestPresent();
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
