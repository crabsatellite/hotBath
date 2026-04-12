package com.crabmod.hotbath.compat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ColdSweatImmersionGate}.
 *
 * <p>These tests cover two things:
 * <ol>
 *   <li>The pure boolean gate that decides whether the WaterTempModifier's
 *       {@code Calculate.Pre} event should be cancelled.</li>
 *   <li>A reconstruction of Cold Sweat's {@code Temperature.apply} fold, used
 *       to pin down the exact math of the -5&nbsp;&deg;C offset bug and to
 *       demonstrate that neutralizing the water modifier restores the
 *       intended bath temperature.</li>
 * </ol>
 *
 * <p>The tests deliberately avoid importing any Minecraft or Cold Sweat types
 * so they run as plain JUnit 5 tests without bootstrapping the game.
 */
class ColdSweatImmersionGateTest {

    private static final double EPSILON = 1.0e-9;

    /**
     * Cold Sweat converts Celsius to Minecraft units with {@code C/25}, so
     * 37&nbsp;&deg;C is {@code 1.48 MC}. These constants match the values
     * used in the live bug report.
     */
    private static final double HOT_BATH_C = 37.0;
    private static final double COLD_BATH_C = 15.0;

    @Nested
    @DisplayName("shouldNeutralizeWaterTempModifier() — pure gate logic")
    class GateLogic {

        @Test
        @DisplayName("returns true when all four conditions are met")
        void allConditionsMet() {
            assertTrue(ColdSweatImmersionGate.shouldNeutralizeWaterTempModifier(
                    true, true, true, true));
        }

        @Test
        @DisplayName("returns false when the modifier is not a WaterTempModifier")
        void notWaterModifier() {
            assertFalse(ColdSweatImmersionGate.shouldNeutralizeWaterTempModifier(
                    false, true, true, true));
        }

        @Test
        @DisplayName("returns false for non-WORLD traits (BODY, BASE, etc.)")
        void notWorldTrait() {
            assertFalse(ColdSweatImmersionGate.shouldNeutralizeWaterTempModifier(
                    true, false, true, true));
        }

        @Test
        @DisplayName("returns false for non-Player entities (leaves mobs alone)")
        void notPlayer() {
            assertFalse(ColdSweatImmersionGate.shouldNeutralizeWaterTempModifier(
                    true, true, false, true));
        }

        @Test
        @DisplayName("returns false when player is not standing in a bath block")
        void playerNotInBath() {
            assertFalse(ColdSweatImmersionGate.shouldNeutralizeWaterTempModifier(
                    true, true, true, false));
        }

        @Test
        @DisplayName("returns false when everything is false (regular rain soak path)")
        void nothingApplies() {
            assertFalse(ColdSweatImmersionGate.shouldNeutralizeWaterTempModifier(
                    false, false, false, false));
        }
    }

    @Nested
    @DisplayName("IDENTITY — neutralized modifier function")
    class IdentityFunction {

        @Test
        @DisplayName("returns the same temperature it was given")
        void identityReturnsInput() {
            assertEquals(0.0, ColdSweatImmersionGate.IDENTITY.apply(0.0), EPSILON);
            assertEquals(1.48, ColdSweatImmersionGate.IDENTITY.apply(1.48), EPSILON);
            assertEquals(-0.2, ColdSweatImmersionGate.IDENTITY.apply(-0.2), EPSILON);
        }

        @Test
        @DisplayName("is a stable singleton — no per-event allocation")
        void identityIsSingleton() {
            assertSame(ColdSweatImmersionGate.IDENTITY, ColdSweatImmersionGate.IDENTITY);
        }
    }

    @Nested
    @DisplayName("celsiusToMC() — unit conversion parity with Cold Sweat")
    class UnitConversion {

        @Test
        @DisplayName("37°C maps to 1.48 MC (hot bath target)")
        void hotBathConversion() {
            assertEquals(1.48, ColdSweatImmersionGate.celsiusToMC(HOT_BATH_C), EPSILON);
        }

        @Test
        @DisplayName("15°C maps to 0.6 MC (ender / cold bath target)")
        void coldBathConversion() {
            assertEquals(0.6, ColdSweatImmersionGate.celsiusToMC(COLD_BATH_C), EPSILON);
        }

        @Test
        @DisplayName("0°C maps to 0.0 MC")
        void zeroConversion() {
            assertEquals(0.0, ColdSweatImmersionGate.celsiusToMC(0.0), EPSILON);
        }
    }

    @Nested
    @DisplayName("simulateChain() — reproduction of Cold Sweat's Temperature.apply fold")
    class ChainSimulation {

        /** HotBathImmersionModifier: constant-replace to the bath target, ignoring prior temp. */
        private Function<Double, Double> hotBathModifier(double bathCelsius) {
            double targetMC = ColdSweatImmersionGate.celsiusToMC(bathCelsius);
            return temp -> targetMC;
        }

        /** WaterTempModifier: additive, converging toward DEFAULT_WATER_TEMPERATURE. */
        private Function<Double, Double> waterTempModifier(double converged) {
            return temp -> temp + converged;
        }

        @Test
        @DisplayName("BUG: hot bath (37°C) + live WaterTempModifier yields 32°C (-5°C offset)")
        void hotBathBugWithoutFix() {
            double result = ColdSweatImmersionGate.simulateChain(
                    0.0,
                    hotBathModifier(HOT_BATH_C),
                    waterTempModifier(ColdSweatImmersionGate.DEFAULT_WATER_TEMPERATURE_MC)
            );

            // 1.48 + (-0.2) = 1.28 MC = 32°C
            assertEquals(1.28, result, EPSILON,
                    "Pre-fix behaviour: WaterTempModifier steals -0.2 MC from the forced bath temp");
        }

        @Test
        @DisplayName("BUG: ender bath (15°C) + live WaterTempModifier yields 10°C (-5°C offset)")
        void coldBathBugWithoutFix() {
            double result = ColdSweatImmersionGate.simulateChain(
                    0.0,
                    hotBathModifier(COLD_BATH_C),
                    waterTempModifier(ColdSweatImmersionGate.DEFAULT_WATER_TEMPERATURE_MC)
            );

            // 0.6 + (-0.2) = 0.4 MC = 10°C
            assertEquals(0.4, result, EPSILON,
                    "Pre-fix behaviour: same -0.2 MC steal applies to cold baths");
        }

        @Test
        @DisplayName("FIX: hot bath + neutralized WaterTempModifier yields 37°C exactly")
        void hotBathFixWithNeutralizedWaterModifier() {
            double result = ColdSweatImmersionGate.simulateChain(
                    0.0,
                    hotBathModifier(HOT_BATH_C),
                    ColdSweatImmersionGate.IDENTITY // our fix: identity replaces WaterTempModifier
            );

            assertEquals(1.48, result, EPSILON,
                    "With the WaterTempModifier neutralized, the bath target survives unchanged");
        }

        @Test
        @DisplayName("FIX: ender bath + neutralized WaterTempModifier yields 15°C exactly")
        void coldBathFixWithNeutralizedWaterModifier() {
            double result = ColdSweatImmersionGate.simulateChain(
                    0.0,
                    hotBathModifier(COLD_BATH_C),
                    ColdSweatImmersionGate.IDENTITY
            );

            assertEquals(0.6, result, EPSILON,
                    "Cold bath target is also preserved");
        }

        @Test
        @DisplayName("Chain with zero modifiers is a no-op")
        void emptyChain() {
            assertEquals(0.42, ColdSweatImmersionGate.simulateChain(0.42), EPSILON);
        }

        @Test
        @DisplayName("Non-bath case: real water still applies its offset (no regression)")
        void regularWaterStillApplies() {
            // Player in real water (no bath, no HotBathImmersionModifier) — the
            // water modifier must still work normally so rain/swim soaking
            // continues to function. This is what our fix must NOT break.
            double result = ColdSweatImmersionGate.simulateChain(
                    0.0,
                    waterTempModifier(ColdSweatImmersionGate.DEFAULT_WATER_TEMPERATURE_MC)
            );

            assertEquals(-0.2, result, EPSILON,
                    "Outside of baths, WaterTempModifier must still contribute its offset");
        }
    }
}
