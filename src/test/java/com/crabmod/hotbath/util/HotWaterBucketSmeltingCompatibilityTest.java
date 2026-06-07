package com.crabmod.hotbath.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotWaterBucketSmeltingCompatibilityTest {

    @Test
    void smeltingRecipeIsGuardedFromPolymorphThirstWasTakenConflict() throws IOException {
        String json = Files.readString(
                Path.of("src/main/resources/data/hotbath/recipe/hot_water_bucket.json"),
                StandardCharsets.UTF_8);

        assertAll(
                () -> assertTrue(json.contains("\"neoforge:conditions\""),
                        "Hot water smelting should use NeoForge recipe conditions"),
                () -> assertTrue(json.contains("\"type\": \"neoforge:not\""),
                        "Recipe should be disabled for the known conflicting mod set"),
                () -> assertTrue(json.contains("\"type\": \"neoforge:and\""),
                        "Conflict guard should require both mods, not either mod alone"),
                () -> assertTrue(json.contains("\"modid\": \"polymorph\""),
                        "Conflict guard should include Polymorph"),
                () -> assertTrue(json.contains("\"modid\": \"thirst\""),
                        "Conflict guard should include Thirst Was Taken's mod id"),
                () -> assertTrue(json.contains("\"item\": \"minecraft:water_bucket\""),
                        "Recipe should still heat vanilla water buckets when the conflict is absent"),
                () -> assertTrue(json.contains("\"hotbath:hot_water_bucket\""),
                        "Recipe should still produce HotBath hot water buckets when loaded")
        );
    }
}
