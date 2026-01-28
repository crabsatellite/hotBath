package com.crabmod.hotbath.mixin.alexsmobs;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads Alex's Mobs mixins only when the mod is present.
 * This prevents class loading errors when Alex's Mobs is not installed.
 */
public class AlexsMobsMixinPlugin implements IMixinConfigPlugin {
    
    private static boolean alexsMobsLoaded = false;
    
    static {
        try {
            Class.forName("com.github.alexthe666.alexsmobs.AlexsMobs");
            alexsMobsLoaded = true;
        } catch (ClassNotFoundException e) {
            alexsMobsLoaded = false;
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
        // Only apply mixins if Alex's Mobs is loaded
        return alexsMobsLoaded;
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
        // No pre-apply processing needed
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No post-apply processing needed
    }
}
