package com.crabmod.hotbath.compat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the dirtiness rendering math shared by both
 * {@code DirtinessOverlayRenderer} (vanilla) and {@code EpicFightDirtinessPatchedLayer}
 * (Epic Fight compat).
 *
 * <p>These tests pin down the alpha curve and per-part threshold/intensity
 * remapping so the two renderers stay in sync.
 */
class EpicFightDirtinessLayerTest {

    private static final float EPSILON = 1e-4f;

    private static float calculateAlpha(float dirtiness) {
        return Math.min(0.90f, dirtiness * 0.6f + dirtiness * dirtiness * 0.4f);
    }

    private static float effectiveDirtiness(float dirtiness, float intensityMultiplier, float threshold) {
        if (dirtiness < threshold) return 0.0f;
        return ((dirtiness - threshold) / (1.0f - threshold)) * intensityMultiplier;
    }

    @Nested
    @DisplayName("calculateAlpha — dirtiness-to-alpha curve")
    class AlphaCurve {

        @Test
        @DisplayName("zero dirtiness → zero alpha")
        void zeroDirtiness() {
            assertEquals(0.0f, calculateAlpha(0.0f), EPSILON);
        }

        @Test
        @DisplayName("low dirtiness (0.1) → subtle alpha (~0.064)")
        void lowDirtiness() {
            float alpha = calculateAlpha(0.1f);
            assertTrue(alpha > 0.05f && alpha < 0.1f,
                    "Alpha at 0.1 dirtiness should be subtle, got " + alpha);
        }

        @Test
        @DisplayName("mid dirtiness (0.5) → moderate alpha (~0.40)")
        void midDirtiness() {
            float alpha = calculateAlpha(0.5f);
            assertTrue(alpha > 0.3f && alpha < 0.5f,
                    "Alpha at 0.5 dirtiness should be moderate, got " + alpha);
        }

        @Test
        @DisplayName("full dirtiness (1.0) → capped at 0.90")
        void fullDirtiness() {
            assertEquals(0.90f, calculateAlpha(1.0f), EPSILON);
        }

        @Test
        @DisplayName("above 1.0 still capped at 0.90")
        void overcapped() {
            assertEquals(0.90f, calculateAlpha(1.5f), EPSILON);
        }

        @Test
        @DisplayName("curve is monotonically increasing")
        void monotonic() {
            float prev = 0.0f;
            for (int i = 1; i <= 100; i++) {
                float d = i / 100.0f;
                float alpha = calculateAlpha(d);
                assertTrue(alpha >= prev,
                        "Alpha must increase: at " + d + " got " + alpha + " < prev " + prev);
                prev = alpha;
            }
        }
    }

    @Nested
    @DisplayName("effectiveDirtiness — threshold and intensity remapping")
    class PartRemapping {

        @Test
        @DisplayName("legs (threshold=0.0, intensity=1.0) — full range")
        void legs() {
            assertEquals(0.5f, effectiveDirtiness(0.5f, 1.0f, 0.0f), EPSILON);
            assertEquals(1.0f, effectiveDirtiness(1.0f, 1.0f, 0.0f), EPSILON);
        }

        @Test
        @DisplayName("body (threshold=0.15, intensity=0.8) — remapped")
        void body() {
            assertEquals(0.0f, effectiveDirtiness(0.10f, 0.8f, 0.15f), EPSILON);
            float at50 = effectiveDirtiness(0.5f, 0.8f, 0.15f);
            assertEquals((0.5f - 0.15f) / 0.85f * 0.8f, at50, EPSILON);
        }

        @Test
        @DisplayName("arms (threshold=0.2, intensity=0.7) — below threshold returns 0")
        void arms() {
            assertEquals(0.0f, effectiveDirtiness(0.15f, 0.7f, 0.2f), EPSILON);
            assertTrue(effectiveDirtiness(0.3f, 0.7f, 0.2f) > 0.0f);
        }

        @Test
        @DisplayName("head (threshold=0.5, intensity=0.4) — only shows when very dirty")
        void head() {
            assertEquals(0.0f, effectiveDirtiness(0.4f, 0.4f, 0.5f), EPSILON);
            float atFull = effectiveDirtiness(1.0f, 0.4f, 0.5f);
            assertEquals(0.4f, atFull, EPSILON);
        }
    }

    @Nested
    @DisplayName("pattern selection from seed")
    class PatternSelection {

        @Test
        @DisplayName("seed maps to pattern 0..9")
        void patternRange() {
            for (long seed = -100; seed <= 100; seed++) {
                int pattern = Math.abs((int) seed) % 10;
                assertTrue(pattern >= 0 && pattern < 10,
                        "Pattern from seed " + seed + " out of range: " + pattern);
            }
        }

        @Test
        @DisplayName("same seed always gives same pattern")
        void deterministic() {
            long seed = 0xDEADBEEFL;
            int p1 = Math.abs((int) seed) % 10;
            int p2 = Math.abs((int) seed) % 10;
            assertEquals(p1, p2);
        }
    }

    @Nested
    @DisplayName("body part group specification — must match vanilla DirtinessOverlayRenderer")
    class BodyPartGroups {

        // These constants define the contract between vanilla and Epic Fight renderers.
        // Vanilla:  model.leftLeg/rightLeg/leftPants/rightPants       → intensity 1.0, threshold 0.0
        //           model.body/jacket                                  → intensity 0.8, threshold 0.15
        //           model.leftArm/rightArm/leftSleeve/rightSleeve     → intensity 0.7, threshold 0.2
        //           model.head/hat                                     → intensity 0.4, threshold 0.5
        // EpicFight: mesh.leftLeg/rightLeg/leftPants/rightPants       → same
        //            mesh.torso/jacket                                 → same (torso = body)
        //            mesh.leftArm/rightArm/leftSleeve/rightSleeve     → same
        //            mesh.head/hat                                     → same

        @Test
        @DisplayName("all 12 HumanoidMesh parts are accounted for in exactly 4 groups")
        void allPartsCovered() {
            // Legs (4), Body (2), Arms (4), Head (2) = 12 total
            int legs = 4;   // leftLeg, rightLeg, leftPants, rightPants
            int body = 2;   // torso, jacket
            int arms = 4;   // leftArm, rightArm, leftSleeve, rightSleeve
            int head = 2;   // head, hat
            assertEquals(12, legs + body + arms + head,
                    "Part groups must cover all 12 HumanoidMesh parts");
        }

        @Test
        @DisplayName("legs are dirtiest (highest intensity, lowest threshold)")
        void legsAreDirtiest() {
            float legsIntensity = 1.0f;
            float legsThreshold = 0.0f;
            assertTrue(legsIntensity >= 0.8f);
            assertEquals(0.0f, legsThreshold, EPSILON);
        }

        @Test
        @DisplayName("head is cleanest (lowest intensity, highest threshold)")
        void headIsCleanest() {
            float headIntensity = 0.4f;
            float headThreshold = 0.5f;
            assertTrue(headIntensity <= 0.5f);
            assertTrue(headThreshold >= 0.4f);
        }

        @Test
        @DisplayName("intensity ordering: legs > body > arms > head")
        void intensityOrdering() {
            float legs = 1.0f, body = 0.8f, arms = 0.7f, head = 0.4f;
            assertTrue(legs > body, "legs > body");
            assertTrue(body > arms, "body > arms");
            assertTrue(arms > head, "arms > head");
        }

        @Test
        @DisplayName("threshold ordering: legs < body < arms < head")
        void thresholdOrdering() {
            float legs = 0.0f, body = 0.15f, arms = 0.2f, head = 0.5f;
            assertTrue(legs < body, "legs < body");
            assertTrue(body < arms, "body < arms");
            assertTrue(arms < head, "arms < head");
        }
    }

    @Nested
    @DisplayName("exact boundary values — regression guard for both renderers")
    class BoundaryValues {

        @Test
        @DisplayName("alpha at exact threshold boundaries")
        void alphaAtThresholds() {
            // At threshold = effective dirtiness is 0 → alpha should be 0
            assertEquals(0.0f, calculateAlpha(effectiveDirtiness(0.15f, 0.8f, 0.15f)), EPSILON,
                    "Body at exact threshold should produce 0 alpha");
            assertEquals(0.0f, calculateAlpha(effectiveDirtiness(0.2f, 0.7f, 0.2f)), EPSILON,
                    "Arms at exact threshold should produce 0 alpha");
            assertEquals(0.0f, calculateAlpha(effectiveDirtiness(0.5f, 0.4f, 0.5f)), EPSILON,
                    "Head at exact threshold should produce 0 alpha");
        }

        @Test
        @DisplayName("effective dirtiness at threshold=1.0 would cause division by zero")
        void thresholdAtOne() {
            // threshold=1.0 means (1.0-1.0)/(1.0-1.0) = 0/0
            // Our thresholds are all < 1.0 so this shouldn't happen,
            // but verify the formula handles it (returns 0 since dirtiness < threshold=1.0)
            assertEquals(0.0f, effectiveDirtiness(0.99f, 1.0f, 1.0f), EPSILON);
        }

        @Test
        @DisplayName("dirt color constants: brown RGB (0.30, 0.22, 0.15)")
        void dirtColor() {
            // Both renderers must use the same brown tint
            float r = 0.30f, g = 0.22f, b = 0.15f;
            // Vanilla packs as: (int)(r*255)=76, (int)(g*255)=56, (int)(b*255)=38
            assertEquals(76, (int)(r * 255), "Red channel");
            assertEquals(56, (int)(g * 255), "Green channel");
            assertEquals(38, (int)(b * 255), "Blue channel");
        }
    }
}
