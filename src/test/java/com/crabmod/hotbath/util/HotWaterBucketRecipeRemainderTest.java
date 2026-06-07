package com.crabmod.hotbath.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotWaterBucketRecipeRemainderTest {

    @Test
    void noManualEmptyBucketReturnForHotWaterBucketCrafts() throws IOException {
        Path javaRoot = Path.of("src/main/java");

        try (Stream<Path> files = Files.walk(javaRoot)) {
            List<Path> offenders = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(HotWaterBucketRecipeRemainderTest::manuallyReturnsEmptyBucketForHotWaterBucket)
                    .toList();

            assertTrue(offenders.isEmpty(),
                    () -> "Hot water bucket recipes already output a filled bucket; manual empty bucket returns duplicate buckets: " + offenders);
        }
    }

    @Test
    void herbalBathRecipeTransformsHotWaterBucketIntoHerbalBathBucket() throws IOException {
        String json = Files.readString(herbalBathRecipePath(), StandardCharsets.UTF_8);

        assertAll(
                () -> assertTrue(json.contains("\"hotbath:hot_water_bucket\""),
                        "Herbal bath recipe should consume a hot water bucket ingredient"),
                () -> assertTrue(json.contains("\"hotbath:herbal_bath_bucket\""),
                        "Herbal bath recipe should output a herbal bath bucket")
        );
    }

    private static boolean manuallyReturnsEmptyBucketForHotWaterBucket(Path path) {
        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            return source.contains("ItemCraftedEvent")
                    && source.contains("HOT_WATER_BUCKET")
                    && source.contains("Items.BUCKET");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path herbalBathRecipePath() {
        Path singular = Path.of("src/main/resources/data/hotbath/recipe/herbal_bath_bucket.json");
        if (Files.exists(singular)) {
            return singular;
        }
        return Path.of("src/main/resources/data/hotbath/recipes/herbal_bath_bucket.json");
    }
}
