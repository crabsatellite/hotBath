package com.crabmod.hotbath.compat;

import java.util.function.Function;

/**
 * Pure logic for the Cold Sweat "bath immersion" integration.
 *
 * <p>This class is intentionally free of Minecraft, NeoForge, and Cold Sweat imports
 * so that it can be unit-tested without bootstrapping the game runtime. The event
 * handler in {@link ColdSweatEventHandler} delegates its decision to
 * {@link #shouldNeutralizeWaterTempModifier(boolean, boolean, boolean, boolean)}
 * and uses {@link #IDENTITY} as the replacement function.
 *
 * <h2>Why this gate exists</h2>
 * Hot Bath's fluid blocks extend vanilla {@code LiquidBlock} and are tagged into
 * {@code minecraft:water}, so {@code player.isInWater()} returns {@code true}
 * while the player is bathing. Cold Sweat's {@code handleWaterFreezingFire} tick
 * handler then dynamically appends a {@code WaterTempModifier} to the WORLD
 * trait chain via {@code Placement.LAST}, which runs <em>after</em> our
 * {@code HotBathImmersionModifier}. The water modifier's function is
 * {@code temp -> temp + newTemperature}, where {@code newTemperature}
 * converges toward Cold Sweat's {@code DEFAULT_WATER_TEMPERATURE}
 * ({@code -0.2 MC} = -5&nbsp;&deg;C). The result is a persistent -5&nbsp;&deg;C
 * offset on top of the bath's forced temperature.
 *
 * <p>Our fix subscribes to {@code TempModifierEvent.Calculate.Pre} and, when
 * the gate returns {@code true}, cancels the event and installs {@link #IDENTITY}
 * as the new function, neutralizing the water modifier while the player is bathing.
 */
public final class ColdSweatImmersionGate {

    private ColdSweatImmersionGate() {}

    /**
     * Shared identity function used to neutralize a TempModifier's output when
     * the player is soaking in a hot bath. Applying this function to any input
     * temperature returns it unchanged, effectively disabling the modifier for
     * this tick without removing it from the chain.
     */
    public static final Function<Double, Double> IDENTITY = temp -> temp;

    /**
     * Decides whether an in-progress {@code WaterTempModifier} calculation
     * should be neutralized because the player is currently in a Hot Bath fluid.
     *
     * <p>The decision is a pure conjunction of four conditions — extracted
     * into this helper so it can be exercised by unit tests without any
     * Minecraft or Cold Sweat types on the classpath.
     *
     * @param modifierIsWaterTempModifier  {@code event.getModifier() instanceof WaterTempModifier}
     * @param traitIsWorld                 {@code event.getTrait() == Temperature.Trait.WORLD}
     * @param entityIsPlayer               {@code event.getEntity() instanceof Player}
     * @param playerIsInHotBathBlock       {@code CustomFluidHandler.isPlayerInHotBathBlock(player)}
     * @return {@code true} if the Calculate.Pre event should be canceled and
     *         the modifier function replaced with {@link #IDENTITY}.
     */
    public static boolean shouldNeutralizeWaterTempModifier(
            boolean modifierIsWaterTempModifier,
            boolean traitIsWorld,
            boolean entityIsPlayer,
            boolean playerIsInHotBathBlock) {
        return modifierIsWaterTempModifier
                && traitIsWorld
                && entityIsPlayer
                && playerIsInHotBathBlock;
    }

    /**
     * Replays the single-pass fold used by Cold Sweat's
     * {@code Temperature.apply(double, LivingEntity, Trait, TempModifier...)}
     * method. Tests use this to reproduce the -5&nbsp;&deg;C offset bug and
     * demonstrate the fix, entirely in pure Java.
     *
     * @param initialTemp the starting temperature (Cold Sweat starts from 0.0
     *                    for the WORLD trait when the attribute base is NaN)
     * @param functions   the per-modifier functions in chain order
     * @return the final temperature after all functions have been applied
     */
    @SafeVarargs
    public static double simulateChain(double initialTemp, Function<Double, Double>... functions) {
        double temp = initialTemp;
        for (Function<Double, Double> fn : functions) {
            temp = fn.apply(temp);
        }
        return temp;
    }

    /**
     * Cold Sweat's default water temperature, in Minecraft units (MC).
     * Matches {@code ConfigSettings.DEFAULT_WATER_TEMPERATURE}'s default of
     * {@code -0.2} from {@code ConfigSettings.java:886}. Documented here so
     * tests can pin the exact magnitude of the bug.
     */
    public static final double DEFAULT_WATER_TEMPERATURE_MC = -0.2;

    /**
     * Converts Celsius to Minecraft units using Cold Sweat's linear mapping
     * ({@code 25 &deg;C = 1.0 MC}). Provided for test readability so that
     * chain simulations can be written in human units.
     */
    public static double celsiusToMC(double celsius) {
        return celsius / 25.0;
    }
}
