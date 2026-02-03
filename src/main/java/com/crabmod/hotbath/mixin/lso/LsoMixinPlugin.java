package com.crabmod.hotbath.mixin.lso;

import com.crabmod.hotbath.mixin.HotBathMixinPlugin;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads Legendary Survival Overhaul mixins only when:
 * 1. The mod is present
 * 2. Compatibility mode is NOT enabled
 */
public class LsoMixinPlugin implements IMixinConfigPlugin {
    
    private static Boolean lsoLoaded = null;
    
    private static boolean checkLsoPresent() {
        if (lsoLoaded == null) {
            try {
                lsoLoaded = FMLLoader.getLoadingModList().getModFileById("legendarysurvivaloverhaul") != null;
                if (lsoLoaded) {
                    System.out.println("[HotBath] Legendary Survival Overhaul detected via FMLLoader");
                }
            } catch (Throwable t) {
                lsoLoaded = false;
            }
        }
        return lsoLoaded;
    }
    
    @Override
    public void onLoad(String mixinPackage) {
        checkLsoPresent();
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
        // Only apply if LSO is loaded
        return checkLsoPresent();
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
