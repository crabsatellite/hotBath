package com.crabmod.hotbath.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmersDelightCompatContractTest {

    @Test
    void hotBathUsesNourishmentAfterComfortRetirement() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/com/crabmod/hotbath/compat/FarmersDelightEventHandler.java"),
                StandardCharsets.UTF_8);

        assertAll(
                () -> assertTrue(source.contains("ModEffects.NOURISHMENT"),
                        "Farmer's Delight 1.3 retired Comfort behavior in favor of Nourishment"),
                () -> assertFalse(source.contains("ModEffects.COMFORT"),
                        "HotBath should not grant Farmer's Delight's retired Comfort effect")
        );
    }

    @Test
    void dependencyTargetsFarmersDelightOneThreeTwo() throws IOException {
        String build = Files.readString(Path.of("build.gradle"), StandardCharsets.UTF_8);

        assertTrue(build.contains("farmers-delight-398521:8083474"),
                "Forge 1.20.1 should compile against Farmer's Delight 1.3.2");
    }
}
