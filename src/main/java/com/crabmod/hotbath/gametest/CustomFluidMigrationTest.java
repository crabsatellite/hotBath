package com.crabmod.hotbath.gametest;

import com.crabmod.hotbath.custom_fluid.CustomFluidDataComponents;
import com.crabmod.hotbath.custom_fluid.CustomFluidDefinition;
import com.crabmod.hotbath.custom_fluid.CustomFluidItems;
import com.crabmod.hotbath.custom_fluid.CustomFluidRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * GameTests for verifying legacy 1.20.1 NBT → 1.21.1 Data Component migration.
 *
 * <p>In 1.20.1, custom fluid items stored data in NBT tags:
 * <pre>{HotbathCustomFluid:{FluidId:"hotbath:golden_bath", FluidColor:16766720, FluidName:"Golden Bath"}}</pre>
 *
 * <p>When a world is upgraded from 1.20.1 to 1.21.1, Minecraft moves these NBT tags
 * into the {@code minecraft:custom_data} component. The migration code in
 * {@link CustomFluidDataComponents#getFluidId} detects this legacy format and converts
 * it to the new Data Component format ({@code hotbath:custom_fluid_id}, etc.).
 *
 * <p>Run these tests via: {@code ./gradlew runGameTestServer}
 */
@GameTestHolder("hotbath")
@PrefixGameTestTemplate(false)
public class CustomFluidMigrationTest {

    // ==========================================
    // Test: New 1.21.1 component format works
    // ==========================================

    /**
     * Verify that a stack with the new hotbath:custom_fluid_id component
     * returns the correct ResourceLocation from getFluidId().
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_new_component_format(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());
        stack.set(CustomFluidDataComponents.CUSTOM_FLUID_ID.get(), "hotbath:golden_bath");

        ResourceLocation result = CustomFluidDataComponents.getFluidId(stack);

        if (result == null) {
            helper.fail("getFluidId() returned null for stack with new component format");
            return;
        }
        if (!result.equals(ResourceLocation.parse("hotbath:golden_bath"))) {
            helper.fail("Expected hotbath:golden_bath but got " + result);
            return;
        }

        helper.succeed();
    }

    // ==========================================
    // Test: Legacy 1.20.1 NBT migration
    // ==========================================

    /**
     * Verify that legacy 1.20.1 NBT format in custom_data is detected and migrated.
     * The legacy format: {HotbathCustomFluid:{FluidId:"hotbath:golden_bath"}}
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_legacy_nbt_migration(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());

        // Simulate legacy 1.20.1 NBT that was moved to custom_data during world upgrade
        CompoundTag legacyTag = new CompoundTag();
        CompoundTag fluidTag = new CompoundTag();
        fluidTag.putString("FluidId", "hotbath:golden_bath");
        fluidTag.putInt("FluidColor", 16766720);
        fluidTag.putString("FluidName", "Golden Bath");
        legacyTag.put("HotbathCustomFluid", fluidTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyTag));

        // Call getFluidId - should trigger migration
        ResourceLocation result = CustomFluidDataComponents.getFluidId(stack);

        if (result == null) {
            helper.fail("getFluidId() returned null for legacy NBT format");
            return;
        }
        if (!result.equals(ResourceLocation.parse("hotbath:golden_bath"))) {
            helper.fail("Expected hotbath:golden_bath but got " + result);
            return;
        }

        // Verify the new component was set during migration
        String newComponentValue = stack.get(CustomFluidDataComponents.CUSTOM_FLUID_ID.get());
        if (newComponentValue == null || !newComponentValue.equals("hotbath:golden_bath")) {
            helper.fail("Migration did not set CUSTOM_FLUID_ID component. Value: " + newComponentValue);
            return;
        }

        helper.succeed();
    }

    // ==========================================
    // Test: Legacy NBT is cleaned up after migration
    // ==========================================

    /**
     * Verify that after migration, the HotbathCustomFluid tag is removed from custom_data,
     * and custom_data itself is removed if empty.
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_legacy_nbt_cleanup(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());

        // Only legacy data, no other custom_data
        CompoundTag legacyTag = new CompoundTag();
        CompoundTag fluidTag = new CompoundTag();
        fluidTag.putString("FluidId", "hotbath:golden_bath");
        legacyTag.put("HotbathCustomFluid", fluidTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyTag));

        // Trigger migration
        CustomFluidDataComponents.getFluidId(stack);

        // custom_data should be completely removed since there's no other data
        CustomData remainingData = stack.get(DataComponents.CUSTOM_DATA);
        if (remainingData != null) {
            CompoundTag remainingTag = remainingData.copyTag();
            if (remainingTag.contains("HotbathCustomFluid")) {
                helper.fail("HotbathCustomFluid tag was not removed from custom_data");
                return;
            }
            if (!remainingTag.isEmpty()) {
                helper.fail("custom_data should be removed when empty, but found: " + remainingTag);
                return;
            }
        }

        helper.succeed();
    }

    // ==========================================
    // Test: Other custom_data preserved during migration
    // ==========================================

    /**
     * When custom_data contains both legacy HotbathCustomFluid and other data,
     * only HotbathCustomFluid should be removed; other data should be preserved.
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_legacy_preserves_other_custom_data(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());

        CompoundTag customTag = new CompoundTag();
        // Legacy fluid data
        CompoundTag fluidTag = new CompoundTag();
        fluidTag.putString("FluidId", "hotbath:golden_bath");
        customTag.put("HotbathCustomFluid", fluidTag);
        // Some other mod's data that should be preserved
        customTag.putString("SomeOtherMod", "preserved_value");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));

        // Trigger migration
        CustomFluidDataComponents.getFluidId(stack);

        // Verify other data is preserved
        CustomData remainingData = stack.get(DataComponents.CUSTOM_DATA);
        if (remainingData == null) {
            helper.fail("custom_data was completely removed but should still have SomeOtherMod data");
            return;
        }
        CompoundTag remainingTag = remainingData.copyTag();
        if (remainingTag.contains("HotbathCustomFluid")) {
            helper.fail("HotbathCustomFluid was not removed during migration");
            return;
        }
        if (!remainingTag.contains("SomeOtherMod")) {
            helper.fail("SomeOtherMod data was incorrectly removed during migration");
            return;
        }
        if (!"preserved_value".equals(remainingTag.getString("SomeOtherMod"))) {
            helper.fail("SomeOtherMod data was corrupted during migration");
            return;
        }

        helper.succeed();
    }

    // ==========================================
    // Test: No fluid data returns null
    // ==========================================

    /**
     * A stack with no fluid data (no component, no legacy NBT) should return null.
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_no_fluid_data_returns_null(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());

        ResourceLocation result = CustomFluidDataComponents.getFluidId(stack);

        if (result != null) {
            helper.fail("Expected null for stack with no fluid data, but got " + result);
            return;
        }

        helper.succeed();
    }

    // ==========================================
    // Test: Empty FluidId in legacy data
    // ==========================================

    /**
     * If the legacy NBT has an empty FluidId string, it should return null.
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_empty_legacy_fluid_id(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());

        CompoundTag legacyTag = new CompoundTag();
        CompoundTag fluidTag = new CompoundTag();
        fluidTag.putString("FluidId", "");
        legacyTag.put("HotbathCustomFluid", fluidTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyTag));

        ResourceLocation result = CustomFluidDataComponents.getFluidId(stack);

        if (result != null) {
            helper.fail("Expected null for empty FluidId, but got " + result);
            return;
        }

        helper.succeed();
    }

    // ==========================================
    // Test: Migration with registry definition populates all components
    // ==========================================

    /**
     * When the CustomFluidRegistry has the fluid definition loaded,
     * migration should populate all data components (ID, color, name).
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_migration_with_registry(GameTestHelper helper) {
        // Register a test fluid definition
        ResourceLocation testFluidId = ResourceLocation.parse("hotbath:test_migration_fluid");
        CustomFluidDefinition testDef = new CustomFluidDefinition.Builder(testFluidId)
                .color(0xFF00FF)
                .temperature(42.0f)
                .build();
        CustomFluidRegistry.onFluidsReloaded(List.of(testDef));

        try {
            ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());

            // Set up legacy NBT
            CompoundTag legacyTag = new CompoundTag();
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.putString("FluidId", "hotbath:test_migration_fluid");
            legacyTag.put("HotbathCustomFluid", fluidTag);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyTag));

            // Trigger migration
            ResourceLocation result = CustomFluidDataComponents.getFluidId(stack);

            if (result == null || !result.equals(testFluidId)) {
                helper.fail("getFluidId() returned wrong value: " + result);
                return;
            }

            // Verify all components were set by setFluid()
            String idComponent = stack.get(CustomFluidDataComponents.CUSTOM_FLUID_ID.get());
            if (!"hotbath:test_migration_fluid".equals(idComponent)) {
                helper.fail("CUSTOM_FLUID_ID not set correctly: " + idComponent);
                return;
            }

            Integer colorComponent = stack.get(CustomFluidDataComponents.CUSTOM_FLUID_COLOR.get());
            if (colorComponent == null) {
                helper.fail("CUSTOM_FLUID_COLOR not set after migration with registry");
                return;
            }
            // setFluid() applies 0xFF000000 | color
            int expectedColor = 0xFF000000 | 0xFF00FF;
            if (colorComponent != expectedColor) {
                helper.fail("CUSTOM_FLUID_COLOR wrong: expected " +
                        Integer.toHexString(expectedColor) + " got " + Integer.toHexString(colorComponent));
                return;
            }

            String nameComponent = stack.get(CustomFluidDataComponents.CUSTOM_FLUID_NAME.get());
            if (nameComponent == null || !nameComponent.equals("item.hotbath.custom_fluid.test_migration_fluid")) {
                helper.fail("CUSTOM_FLUID_NAME not set correctly: " + nameComponent);
                return;
            }

            helper.succeed();
        } finally {
            // Clean up: restore empty registry
            CustomFluidRegistry.onFluidsReloaded(List.of());
        }
    }

    // ==========================================
    // Test: Bottle item also migrates correctly
    // ==========================================

    /**
     * Verify migration also works on bottle items (not just buckets).
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_bottle_legacy_migration(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BOTTLE.get());

        CompoundTag legacyTag = new CompoundTag();
        CompoundTag fluidTag = new CompoundTag();
        fluidTag.putString("FluidId", "hotbath:herbal_bath");
        legacyTag.put("HotbathCustomFluid", fluidTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyTag));

        ResourceLocation result = CustomFluidDataComponents.getFluidId(stack);

        if (result == null || !result.equals(ResourceLocation.parse("hotbath:herbal_bath"))) {
            helper.fail("Bottle migration failed. Got: " + result);
            return;
        }

        helper.succeed();
    }

    // ==========================================
    // Test: Splash bottle also migrates correctly
    // ==========================================

    /**
     * Verify migration works on splash bottle items.
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_splash_bottle_legacy_migration(GameTestHelper helper) {
        ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_SPLASH_BOTTLE.get());

        CompoundTag legacyTag = new CompoundTag();
        CompoundTag fluidTag = new CompoundTag();
        fluidTag.putString("FluidId", "hotbath:ender_bath");
        legacyTag.put("HotbathCustomFluid", fluidTag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(legacyTag));

        ResourceLocation result = CustomFluidDataComponents.getFluidId(stack);

        if (result == null || !result.equals(ResourceLocation.parse("hotbath:ender_bath"))) {
            helper.fail("Splash bottle migration failed. Got: " + result);
            return;
        }

        helper.succeed();
    }

    // ==========================================
    // Test: getFluidColor fallback from registry
    // ==========================================

    /**
     * When only custom_fluid_id is set (no cached color), getFluidColor()
     * should look up the color from the registry.
     */
    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_fluid_color_fallback(GameTestHelper helper) {
        ResourceLocation testFluidId = ResourceLocation.parse("hotbath:test_color_fluid");
        CustomFluidDefinition testDef = new CustomFluidDefinition.Builder(testFluidId)
                .color(0xAABBCC)
                .build();
        CustomFluidRegistry.onFluidsReloaded(List.of(testDef));

        try {
            ItemStack stack = new ItemStack(CustomFluidItems.CUSTOM_FLUID_BUCKET.get());
            // Only set the ID, not the color component
            stack.set(CustomFluidDataComponents.CUSTOM_FLUID_ID.get(), "hotbath:test_color_fluid");

            int color = CustomFluidDataComponents.getFluidColor(stack);
            int expected = 0xFF000000 | 0xAABBCC;

            if (color != expected) {
                helper.fail("Expected color " + Integer.toHexString(expected) +
                        " but got " + Integer.toHexString(color));
                return;
            }

            helper.succeed();
        } finally {
            CustomFluidRegistry.onFluidsReloaded(List.of());
        }
    }
}
