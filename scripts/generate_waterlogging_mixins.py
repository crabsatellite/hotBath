#!/usr/bin/env python3
"""
Script to generate Mixin classes for all blocks that implement SimpleWaterloggedBlock.
This enables hotBath fluids to be placed in any waterloggable block.

For NeoForge 1.21.1, we use interface mixin since it's supported.

Also generates the HotBathMixinPlugin for compatibility mode support.
When compatibility mode is enabled, all mixins, mod integrations, and waterlogging are disabled.
"""

import os
from pathlib import Path

# All Minecraft block classes that implement SimpleWaterloggedBlock in 1.21
# For 1.21, we can use a single interface mixin approach since NeoForge supports it
# This script is provided for reference/documentation

WATERLOGGED_BLOCKS_1_21 = [
    # Basic blocks with WATERLOGGED property
    "StairBlock",
    "SlabBlock",
    "TrapDoorBlock",
    "FenceBlock",
    "FenceGateBlock",
    "WallBlock",
    "LadderBlock",
    "ChainBlock",
    "LanternBlock",
    "CampfireBlock",
    "SignBlock",
    "HangingSignBlock",  # 1.20.2+
    "ConduitBlock",
    "SeaPickleBlock",
    "ScaffoldingBlock",
    "LightningRodBlock",
    "PointedDripstoneBlock",
    "AmethystClusterBlock",
    "BigDripleafBlock",
    "BigDripleafStemBlock",
    "SmallDripleafBlock",
    "HangingRootsBlock",
    "MangrovePropaguleBlock",
    "MangroveRootsBlock",
    "SculkSensorBlock",
    "SculkShriekerBlock",
    "SculkVeinBlock",
    "GlowLichenBlock",
    "CandleBlock",
    "DecoratedPotBlock",
    "ChestBlock",
    "EnderChestBlock",
    "LeavesBlock",
    "LightBlock",
    "BarrierBlock",
    "HeavyCoreBlock",  # 1.21+
    # Parent classes (these cover many blocks)
    # "BaseCoralPlantTypeBlock",
    # "BaseRailBlock",
    # "CrossCollisionBlock",
    # "WaterloggedTransparentBlock",
]

INTERFACE_MIXIN_TEMPLATE = '''package com.crabmod.hotbath.mixin;

import com.crabmod.hotbath.waterlogging.HotbathWaterloggingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to extend SimpleWaterloggedBlock to accept any fluid in the #minecraft:water tag.
 * This allows hotBath fluids to waterlog blocks like stairs, slabs, fences, etc.
 * 
 * NeoForge 1.21 supports interface mixin with @Inject, so we can use a single mixin
 * to cover ALL blocks implementing SimpleWaterloggedBlock.
 * 
 * Covered blocks: {block_list}
 */
@Mixin(SimpleWaterloggedBlock.class)
public interface SimpleWaterloggedBlockMixin {{

    /**
     * Modify canPlaceLiquid to accept any fluid in the water tag, not just Fluids.WATER
     */
    @Inject(method = "canPlaceLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$canPlaceLiquid(@Nullable LivingEntity entity, BlockGetter level, BlockPos pos,
                                         BlockState state, Fluid fluid, CallbackInfoReturnable<Boolean> cir) {{
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {{
            boolean isWaterlogged = state.getValue(BlockStateProperties.WATERLOGGED);
            boolean isWaterTagFluid = fluid.defaultFluidState().is(FluidTags.WATER);
            
            if (!isWaterlogged && isWaterTagFluid) {{
                cir.setReturnValue(true);
            }}
        }}
    }}

    /**
     * Modify placeLiquid to handle any fluid in the water tag
     */
    @Inject(method = "placeLiquid", at = @At("HEAD"), cancellable = true)
    default void hotbath$placeLiquid(LevelAccessor level, BlockPos pos, BlockState state,
                                      FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {{
        if (fluidState.is(FluidTags.WATER)) {{
            if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                    && !state.getValue(BlockStateProperties.WATERLOGGED)) {{
                if (!level.isClientSide()) {{
                    HotbathWaterloggingHelper.storeFluidType(level, pos, fluidState.getType());
                    
                    level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), 3);
                    level.scheduleTick(pos, fluidState.getType(), fluidState.getType().getTickDelay(level));
                }}
                cir.setReturnValue(true);
            }}
        }}
    }}

    /**
     * Modify pickupBlock to return the correct bucket for hotBath fluids
     */
    @Inject(method = "pickupBlock", at = @At("HEAD"), cancellable = true)
    default void hotbath$pickupBlock(@Nullable LivingEntity entity, LevelAccessor level, BlockPos pos,
                                      BlockState state, CallbackInfoReturnable<ItemStack> cir) {{
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) 
                && state.getValue(BlockStateProperties.WATERLOGGED)) {{
            Fluid storedFluid = HotbathWaterloggingHelper.getStoredFluidType(level, pos);
            
            if (storedFluid != null && storedFluid != Fluids.WATER && storedFluid != Fluids.EMPTY) {{
                level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
                HotbathWaterloggingHelper.removeFluidType(level, pos);
                
                ItemStack bucket = new ItemStack(storedFluid.getBucket());
                if (!bucket.isEmpty()) {{
                    cir.setReturnValue(bucket);
                }}
            }}
        }}
    }}

    /**
     * Modify getFluidState to return the correct fluid for hotBath fluids
     */
    @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    default void hotbath$getFluidState(BlockState state, CallbackInfoReturnable<FluidState> cir) {{
        // This method doesn't have position info, so we can't use our storage here
        // The fluid state will be retrieved from the block's actual position in other ways
    }}
}}
'''

# ============================================================================
# Mixin Plugin Template - Handles compatibility mode detection at mixin load time
# For NeoForge 1.21
# ============================================================================
MIXIN_PLUGIN_TEMPLATE = '''package com.crabmod.hotbath.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that checks for compatibility mode and waterlogging settings before loading mixins.
 * 
 * Config Priority:
 * 1. compatibilityMode = true -> ALL mixins disabled (except FishingHookMixin)
 * 2. enableWaterlogging = false -> Waterlogging mixins disabled, other mixins still work
 * 
 * SAVE SAFETY:
 * - Disabling waterlogging does NOT corrupt world data
 * - Existing waterlogged blocks will display as vanilla water
 * - Data is preserved and will be restored when re-enabled
 * 
 * This reads the config file directly since NeoForge config is not available at mixin load time.
 * Auto-generated by generate_waterlogging_mixins.py
 */
public class HotBathMixinPlugin implements IMixinConfigPlugin {
    
    private static boolean compatibilityMode = false;
    private static boolean waterloggingEnabled = true;
    private static boolean configChecked = false;
    
    // List of waterlogging-related mixin class names (without package)
    private static final Set<String> WATERLOGGING_MIXINS = Set.of(
        "SimpleWaterloggedBlockMixin",
        "LevelFluidStateMixin"
    );
    
    // Client-side waterlogging mixins
    private static final Set<String> CLIENT_WATERLOGGING_MIXINS = Set.of(
        "BlockRenderDispatcherMixin"
    );
    
    static {
        checkConfig();
    }
    
    /**
     * Check config settings by reading the config file directly.
     * This is necessary because NeoForge config is not available at mixin load time.
     */
    private static void checkConfig() {
        if (configChecked) {
            return;
        }
        configChecked = true;
        
        try {
            // Try to find the config file in common locations
            Path configDir = Path.of("config");
            Path configFile = configDir.resolve("hotbath-common.toml");
            
            if (Files.exists(configFile)) {
                String content = Files.readString(configFile);
                // Parse the TOML file for settings
                for (String line : content.split("\\\\n")) {
                    line = line.trim();
                    if (line.startsWith("compatibilityMode")) {
                        if (line.contains("true")) {
                            compatibilityMode = true;
                            System.out.println("[HotBath] Compatibility mode enabled - disabling all advanced mixins");
                        }
                    } else if (line.startsWith("enableWaterlogging")) {
                        if (line.contains("false")) {
                            waterloggingEnabled = false;
                            System.out.println("[HotBath] Waterlogging disabled in config");
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Config not found or not readable, use defaults
            System.out.println("[HotBath] Could not read config file, using default settings");
        }
        
        // Log final state
        if (!compatibilityMode && !waterloggingEnabled) {
            System.out.println("[HotBath] Waterlogging mixins will be disabled, other features remain active");
        }
    }
    
    /**
     * Check if compatibility mode is enabled.
     * @return true if compatibility mode is enabled
     */
    public static boolean isCompatibilityModeEnabled() {
        return compatibilityMode;
    }
    
    /**
     * Check if waterlogging is enabled.
     * @return true if waterlogging is enabled (and compatibility mode is off)
     */
    public static boolean isWaterloggingEnabled() {
        return !compatibilityMode && waterloggingEnabled;
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
        // Extract simple class name from full mixin class name
        String simpleName = mixinClassName;
        int lastDot = mixinClassName.lastIndexOf('.');
        if (lastDot >= 0) {
            simpleName = mixinClassName.substring(lastDot + 1);
        }
        
        // FishingHookMixin is always allowed - it just prevents fishing in bath water
        if (simpleName.equals("FishingHookMixin")) {
            return true;
        }
        
        // If compatibility mode is enabled, disable everything else
        if (compatibilityMode) {
            return false;
        }
        
        // If waterlogging is disabled, disable waterlogging-related mixins
        if (!waterloggingEnabled) {
            if (WATERLOGGING_MIXINS.contains(simpleName) || CLIENT_WATERLOGGING_MIXINS.contains(simpleName)) {
                return false;
            }
        }
        
        return true;
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
'''

# ============================================================================
# Sodium Mixin Plugin Template - For Sodium compatibility mixins (waterlogging-related)
# ============================================================================
SODIUM_MIXIN_PLUGIN_TEMPLATE = '''package com.crabmod.hotbath.mixin.sodium;

import com.crabmod.hotbath.mixin.HotBathMixinPlugin;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads Sodium compatibility mixins only when:
 * 1. Sodium is present
 * 2. Waterlogging is enabled (or compatibility mode is off)
 * 
 * Sodium replaces vanilla's chunk rendering system with its own optimized pipeline,
 * which bypasses our BlockRenderDispatcherMixin. This plugin enables Sodium-specific
 * mixins to intercept the fluid rendering at the right point.
 * 
 * Auto-generated by generate_waterlogging_mixins.py
 */
public class SodiumMixinPlugin implements IMixinConfigPlugin {
    
    private static Boolean sodiumLoaded = null;
    
    /**
     * Check if Sodium is present using NeoForge's mod loading API.
     */
    private static boolean checkSodiumPresent() {
        if (sodiumLoaded == null) {
            try {
                // Use NeoForge's FMLLoader API to check if sodium mod is in the loading list
                sodiumLoaded = FMLLoader.getLoadingModList().getModFileById("sodium") != null;
                
                if (sodiumLoaded) {
                    System.out.println("[HotBath] Sodium detected via FMLLoader");
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
        // First check: Is Sodium even loaded?
        if (!checkSodiumPresent()) {
            return false;
        }
        
        // Second check: Is waterlogging enabled?
        // Sodium mixins are for waterlogging fluid rendering
        if (!HotBathMixinPlugin.isWaterloggingEnabled()) {
            System.out.println("[HotBath] Sodium mixin skipped - waterlogging is disabled");
            return false;
        }
        
        System.out.println("[HotBath] Enabling Sodium compatibility mixin: " + mixinClassName);
        return true;
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
'''

# ============================================================================
# AlexsMobs Mixin Plugin Template
# ============================================================================
ALEXSMOBS_MIXIN_PLUGIN_TEMPLATE = '''package com.crabmod.hotbath.mixin.alexsmobs;

import com.crabmod.hotbath.mixin.HotBathMixinPlugin;
import net.neoforged.fml.loading.FMLLoader;
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
 * Auto-generated by generate_waterlogging_mixins.py
 */
public class AlexsMobsMixinPlugin implements IMixinConfigPlugin {
    
    private static Boolean alexsMobsLoaded = null;
    
    private static boolean checkAlexsMobsPresent() {
        if (alexsMobsLoaded == null) {
            try {
                alexsMobsLoaded = FMLLoader.getLoadingModList().getModFileById("alexsmobs") != null;
                if (alexsMobsLoaded) {
                    System.out.println("[HotBath] Alex's Mobs detected via FMLLoader");
                }
            } catch (Throwable t) {
                alexsMobsLoaded = false;
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
'''

# ============================================================================
# AlexsCaves Mixin Plugin Template
# ============================================================================
ALEXSCAVES_MIXIN_PLUGIN_TEMPLATE = '''package com.crabmod.hotbath.mixin.alexscaves;

import com.crabmod.hotbath.mixin.HotBathMixinPlugin;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads Alex's Caves mixins only when:
 * 1. The mod is present
 * 2. Compatibility mode is NOT enabled
 * 
 * Auto-generated by generate_waterlogging_mixins.py
 */
public class AlexsCavesMixinPlugin implements IMixinConfigPlugin {
    
    private static Boolean alexsCavesLoaded = null;
    
    private static boolean checkAlexsCavesPresent() {
        if (alexsCavesLoaded == null) {
            try {
                alexsCavesLoaded = FMLLoader.getLoadingModList().getModFileById("alexscaves") != null;
                if (alexsCavesLoaded) {
                    System.out.println("[HotBath] Alex's Caves detected via FMLLoader");
                }
            } catch (Throwable t) {
                alexsCavesLoaded = false;
            }
        }
        return alexsCavesLoaded;
    }
    
    @Override
    public void onLoad(String mixinPackage) {
        checkAlexsCavesPresent();
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
        return checkAlexsCavesPresent();
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
'''


def generate_mixins_json() -> str:
    """Generate the hotbath.mixins.json content with plugin support."""
    return '''{
  "required": true,
  "minVersion": "0.8",
  "package": "com.crabmod.hotbath.mixin",
  "compatibilityLevel": "JAVA_21",
  "plugin": "com.crabmod.hotbath.mixin.HotBathMixinPlugin",
  "mixins": [
    "FishingHookMixin",
    "SimpleWaterloggedBlockMixin",
    "LevelFluidStateMixin"
  ],
  "client": ["client.BlockRenderDispatcherMixin"],
  "injectors": {
    "defaultRequire": 1
  }
}
'''


def main():
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    mixin_dir = project_root / "src" / "main" / "java" / "com" / "crabmod" / "hotbath" / "mixin"
    sodium_mixin_dir = mixin_dir / "sodium"
    alexsmobs_mixin_dir = mixin_dir / "alexsmobs"
    alexscaves_mixin_dir = mixin_dir / "alexscaves"
    resources_dir = project_root / "src" / "main" / "resources"
    
    print("For NeoForge 1.21, generating mixin plugins with compatibility mode support.")
    print(f"The interface mixin covers {len(WATERLOGGED_BLOCKS_1_21)} block types automatically.")
    print()
    
    # Ensure directories exist
    mixin_dir.mkdir(parents=True, exist_ok=True)
    sodium_mixin_dir.mkdir(parents=True, exist_ok=True)
    alexsmobs_mixin_dir.mkdir(parents=True, exist_ok=True)
    alexscaves_mixin_dir.mkdir(parents=True, exist_ok=True)
    
    # ========================================
    # Generate HotBathMixinPlugin.java
    # ========================================
    plugin_file = mixin_dir / "HotBathMixinPlugin.java"
    plugin_file.write_text(MIXIN_PLUGIN_TEMPLATE, encoding='utf-8')
    print(f"  Generated: HotBathMixinPlugin.java")
    
    # ========================================
    # Generate SodiumMixinPlugin.java (replaces existing)
    # ========================================
    sodium_plugin_file = sodium_mixin_dir / "SodiumMixinPlugin.java"
    sodium_plugin_file.write_text(SODIUM_MIXIN_PLUGIN_TEMPLATE, encoding='utf-8')
    print(f"  Generated: sodium/SodiumMixinPlugin.java")
    
    # ========================================
    # Generate AlexsMobsMixinPlugin.java
    # ========================================
    alexsmobs_plugin_file = alexsmobs_mixin_dir / "AlexsMobsMixinPlugin.java"
    alexsmobs_plugin_file.write_text(ALEXSMOBS_MIXIN_PLUGIN_TEMPLATE, encoding='utf-8')
    print(f"  Generated: alexsmobs/AlexsMobsMixinPlugin.java")
    
    # ========================================
    # Generate AlexsCavesMixinPlugin.java
    # ========================================
    alexscaves_plugin_file = alexscaves_mixin_dir / "AlexsCavesMixinPlugin.java"
    alexscaves_plugin_file.write_text(ALEXSCAVES_MIXIN_PLUGIN_TEMPLATE, encoding='utf-8')
    print(f"  Generated: alexscaves/AlexsCavesMixinPlugin.java")
    
    # ========================================
    # Generate hotbath.mixins.json
    # ========================================
    mixins_json_file = resources_dir / "hotbath.mixins.json"
    mixins_json_file.write_text(generate_mixins_json(), encoding='utf-8')
    print(f"  Updated: hotbath.mixins.json")
    
    # ========================================
    # Generate SimpleWaterloggedBlockMixin.java if not exists
    # ========================================
    block_list = ", ".join(WATERLOGGED_BLOCKS_1_21[:10]) + "..."
    content = INTERFACE_MIXIN_TEMPLATE.format(block_list=block_list)
    
    output_file = mixin_dir / "SimpleWaterloggedBlockMixin.java"
    
    if output_file.exists():
        print(f"\n✅ {output_file.name} already exists - skipped")
    else:
        output_file.write_text(content, encoding='utf-8')
        print(f"\n✅ Generated: {output_file}")
    
    print()
    print("Blocks covered by SimpleWaterloggedBlockMixin:")
    for block in WATERLOGGED_BLOCKS_1_21[:5]:
        print(f"  - {block}")
    print(f"  ... and {len(WATERLOGGED_BLOCKS_1_21) - 5} more")
    
    print()
    print("✅ Generated all mixin plugins with compatibility mode support")
    print()
    print("📋 Compatibility Mode Features:")
    print("   - compatibilityMode = true -> ALL mixins disabled (except FishingHookMixin)")
    print("   - enableWaterlogging = false -> Only waterlogging mixins disabled")
    print("   - Sodium mixin respects waterlogging setting")
    print("   - All settings are SAVE SAFE - no world corruption")


if __name__ == "__main__":
    main()
