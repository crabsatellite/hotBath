package com.crabmod.hotbath.mixin.alexscaves;

import com.crabmod.hotbath.mixin.HotBathMixinPlugin;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads Alex's Caves mixins only when:
 * 1. The mod is present
 * 2. Compatibility mode is NOT enabled
 */
public class AlexsCavesMixinPlugin implements IMixinConfigPlugin {
    
    private static boolean alexsCavesLoaded = false;
    
    static {
        try {
            Class.forName("com.github.alexmodguy.alexscaves.AlexsCaves");
            alexsCavesLoaded = true;
        } catch (ClassNotFoundException e) {
            alexsCavesLoaded = false;
        }
    }
    
    @Override
    public void onLoad(String mixinPackage) {
        // No initialization needed
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
        // Only apply mixins if Alex's Caves is loaded
        return alexsCavesLoaded;
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
