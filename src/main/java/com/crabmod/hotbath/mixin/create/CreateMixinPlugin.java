package com.crabmod.hotbath.mixin.create;

import com.crabmod.hotbath.mixin.HotBathMixinPlugin;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class CreateMixinPlugin implements IMixinConfigPlugin {
    private static Boolean createLoaded = null;

    private static boolean checkCreatePresent() {
        if (createLoaded == null) {
            try {
                createLoaded = FMLLoader.getLoadingModList().getModFileById("create") != null;
                if (createLoaded) {
                    System.out.println("[HotBath] Create detected via FMLLoader - Create mixins enabled");
                }
            } catch (Throwable t) {
                createLoaded = false;
            }
        }
        return createLoaded;
    }

    @Override
    public void onLoad(String mixinPackage) {
        checkCreatePresent();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !HotBathMixinPlugin.isCompatibilityModeEnabled() && checkCreatePresent();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
