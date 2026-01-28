package com.crabmod.hotbath.mixin.sodium;

import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads Sodium compatibility mixins only when Sodium is present.
 * This prevents class loading errors when Sodium is not installed.
 * 
 * <p>Sodium replaces vanilla's chunk rendering system with its own optimized pipeline, 
 * which bypasses our BlockRenderDispatcherMixin and LevelFluidStateMixin. 
 * This plugin enables Sodium-specific mixins to intercept the fluid rendering at the right point.</p>
 * 
 * <p><b>IMPORTANT:</b> We use FMLLoader's mod loading list API to check for Sodium's presence.
 * This is the standard NeoForge approach, and avoids early class loading which can cause issues 
 * with other mods' mixins.</p>
 */
public class SodiumMixinPlugin implements IMixinConfigPlugin {
    
    private static Boolean sodiumLoaded = null;
    
    /**
     * Check if Sodium is present using NeoForge's mod loading API.
     * This is the standard approach for mod detection.
     */
    private static boolean checkSodiumPresent() {
        if (sodiumLoaded == null) {
            try {
                // Use NeoForge's FMLLoader API to check if sodium mod is in the loading list
                sodiumLoaded = FMLLoader.getLoadingModList().getModFileById("sodium") != null;
                
                if (sodiumLoaded) {
                    System.out.println("[HotBath] Sodium detected via FMLLoader, enabling Sodium compatibility mixins");
                } else {
                    System.out.println("[HotBath] Sodium not detected, Sodium mixins will be skipped");
                }
            } catch (Throwable t) {
                // Fallback: if FMLLoader is not available, try ClassLoader.getResource
                String classPath = "net/caffeinemc/mods/sodium/neoforge/render/FluidRendererImpl.class";
                sodiumLoaded = SodiumMixinPlugin.class.getClassLoader().getResource(classPath) != null;
                System.out.println("[HotBath] Used fallback detection for Sodium: " + sodiumLoaded);
            }
        }
        return sodiumLoaded;
    }
    
    /**
     * Check if Sodium is loaded.
     * @return true if Sodium is present
     */
    public static boolean isSodiumLoaded() {
        return checkSodiumPresent();
    }
    
    @Override
    public void onLoad(String mixinPackage) {
        // Check early but don't force class loading
        checkSodiumPresent();
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only apply mixins if Sodium is loaded
        return checkSodiumPresent();
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
        // No pre-apply modifications needed
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No post-apply modifications needed
    }
}
