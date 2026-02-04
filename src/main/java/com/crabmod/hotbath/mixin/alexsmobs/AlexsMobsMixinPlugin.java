package com.crabmod.hotbath.mixin.alexsmobs;

import com.crabmod.hotbath.mixin.HotBathMixinPlugin;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads Alex's Mobs mixins only when:
 * 1. The mod is present
 * 2. Compatibility mode is NOT enabled
 * 
 * Uses FMLLoader instead of Class.forName to avoid early class loading issues.
 */
public class AlexsMobsMixinPlugin implements IMixinConfigPlugin {
    
    private static Boolean alexsMobsLoaded = null;
    
    private static boolean checkAlexsMobsPresent() {
        if (alexsMobsLoaded == null) {
            try {
                alexsMobsLoaded = FMLLoader.getLoadingModList().getModFileById("alexsmobs") != null;
                if (alexsMobsLoaded) {
                    System.out.println("[HotBath] Alex's Mobs detected via FMLLoader - integration mixins enabled");
                }
            } catch (Throwable t) {
                alexsMobsLoaded = false;
                System.out.println("[HotBath] Error checking for Alex's Mobs: " + t.getMessage());
            }
        }
        return alexsMobsLoaded;
    }
    
    @Override
    public void onLoad(String mixinPackage) {
        checkAlexsMobsPresent();
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
        // Only apply mixins if Alex's Mobs is loaded
        return checkAlexsMobsPresent();
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No special target handling needed
    }
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No pre-apply handling needed
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No post-apply handling needed
    }
}
