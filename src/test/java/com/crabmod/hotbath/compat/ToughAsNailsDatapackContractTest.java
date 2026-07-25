package com.crabmod.hotbath.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToughAsNailsDatapackContractTest {
    private static final Path TAG_ROOT =
            Path.of("src/main/resources/data/toughasnails/tags/items");

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static void assertContains(Path path, String... itemIds) throws IOException {
        String json = read(path);
        for (String itemId : itemIds) {
            assertTrue(json.contains("\"" + itemId + "\""),
                    () -> path + " should contain " + itemId);
        }
    }

    @Test
    void builtInBottlesUseForgeItemTagPathsAndValues() throws IOException {
        Path thirst2 = TAG_ROOT.resolve("thirst/2_thirst_drinks.json");
        Path thirst3 = TAG_ROOT.resolve("thirst/3_thirst_drinks.json");
        Path thirst4 = TAG_ROOT.resolve("thirst/4_thirst_drinks.json");
        Path hydration50 = TAG_ROOT.resolve("hydration/50_hydration_drinks.json");
        Path hydration70 = TAG_ROOT.resolve("hydration/70_hydration_drinks.json");
        Path hydration100 = TAG_ROOT.resolve("hydration/100_hydration_drinks.json");

        assertAll(
                () -> assertFalse(Files.exists(
                                Path.of("src/main/resources/data/toughasnails/tags/item")),
                        "Minecraft 1.20.1 item tags must use the plural items directory"),
                () -> assertContains(thirst2, "hotbath:hot_water_bottle"),
                () -> assertContains(thirst3,
                        "hotbath:honey_bath_bottle",
                        "hotbath:herbal_bath_bottle",
                        "hotbath:peony_bath_bottle",
                        "hotbath:rose_bath_bottle"),
                () -> assertContains(thirst4, "hotbath:milk_bath_bottle"),
                () -> assertContains(hydration50, "hotbath:hot_water_bottle"),
                () -> assertContains(hydration70,
                        "hotbath:honey_bath_bottle",
                        "hotbath:herbal_bath_bottle",
                        "hotbath:peony_bath_bottle",
                        "hotbath:rose_bath_bottle"),
                () -> assertContains(hydration100, "hotbath:milk_bath_bottle")
        );
    }

    @Test
    void builtInBottleEffectsUseTagsWithoutDroppingForgeWarming() throws IOException {
        String source = read(Path.of(
                "src/main/java/com/crabmod/hotbath/items/BathWaterEffects.java"));
        int start = source.indexOf("private static void applyDrinkTemperatureEffects");
        int end = source.indexOf("private static void applySplashTemperatureEffects", start);

        assertTrue(start >= 0 && end > start,
                "BathWaterEffects drink method should remain discoverable");
        String drinkMethod = source.substring(start, end);

        assertAll(
                () -> assertTrue(drinkMethod.contains("BathWaterBottleTANModifier"),
                        "TAN 1.20.1 lacks a consumed-item heating tag, so warming stays manual"),
                () -> assertFalse(drinkMethod.contains("ToughAsNailsThirstHelper"),
                        "TAN thirst and hydration are supplied by item tags"),
                () -> assertTrue(drinkMethod.contains("BathWaterBottleColdSweatModifier"),
                        "TAN changes must not remove Cold Sweat integration"),
                () -> assertTrue(drinkMethod.contains("BathWaterBottleLSOModifier"),
                        "TAN changes must not remove LSO integration")
        );
    }

    @Test
    void customFluidBottlesKeepDynamicToughAsNailsValues() throws IOException {
        String customBottle = read(Path.of(
                "src/main/java/com/crabmod/hotbath/custom_fluid/CustomFluidBottleItem.java"));

        assertTrue(customBottle.contains(
                        "ToughAsNailsThirstHelper.restoreThirst(player, definition)"),
                "Datapack-defined fluids should keep their per-definition thirst values");

        for (Path path : new Path[] {
                TAG_ROOT.resolve("thirst/2_thirst_drinks.json"),
                TAG_ROOT.resolve("thirst/3_thirst_drinks.json"),
                TAG_ROOT.resolve("thirst/4_thirst_drinks.json"),
                TAG_ROOT.resolve("hydration/50_hydration_drinks.json"),
                TAG_ROOT.resolve("hydration/70_hydration_drinks.json"),
                TAG_ROOT.resolve("hydration/100_hydration_drinks.json")
        }) {
            assertFalse(read(path).contains("hotbath:custom_fluid_bottle"),
                    () -> path + " must not override dynamic custom-fluid values");
        }
    }
}
