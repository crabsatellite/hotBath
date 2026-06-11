package com.crabmod.hotbath.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomFluidApiContractTest {

    @Test
    void dynamicCustomFluidStackIdentityApiIsPublicAndMechanismLevel() throws IOException {
        String api = read("src/main/java/com/crabmod/hotbath/custom_fluid/CustomFluidAPI.java");
        String helper = read("src/main/java/com/crabmod/hotbath/custom_fluid/CustomFluidStackHelper.java");
        String docs = read("docs/custom_fluids.md");

        assertAll(
                () -> assertTrue(api.contains("import net.neoforged.neoforge.fluids.FluidStack;"),
                        "NeoForge API should expose the branch-local FluidStack type"),
                () -> assertTrue(api.contains("public static boolean isDynamicCustomFluid(Fluid fluid)"),
                        "External tanks should be able to detect Hot Bath's shared dynamic custom Fluid"),
                () -> assertTrue(api.contains("public static boolean isDynamicCustomFluid(FluidStack stack)"),
                        "External tanks should be able to detect dynamic custom FluidStacks"),
                () -> assertTrue(api.contains("public static FluidStack createCustomFluidStack(ResourceLocation fluidId, int amount)"),
                        "External tanks should be able to create custom FluidStacks without touching helpers"),
                () -> assertTrue(api.contains("public static boolean setCustomFluidId(FluidStack stack, ResourceLocation fluidId)"),
                        "External tanks should be able to preserve custom fluid identity on existing stacks"),
                () -> assertTrue(api.contains("@Nullable\n    public static ResourceLocation getCustomFluidId(FluidStack stack)"),
                        "External tanks should be able to read custom fluid identity from stacks"),
                () -> assertTrue(api.contains("public static Optional<CustomFluidDefinition> getCustomFluidDefinition(FluidStack stack)"),
                        "External tanks should be able to resolve a FluidStack to a custom definition"),
                () -> assertTrue(api.contains("public static boolean hasSameCustomFluidId(FluidStack left, FluidStack right)"),
                        "External tanks should be able to compare datapack ids sharing the same dynamic Fluid"),
                () -> assertTrue(api.contains("CustomFluidStackHelper.createStack(fluidId, amount)")
                                && api.contains("CustomFluidStackHelper.setFluidId(stack, fluidId)")
                                && api.contains("CustomFluidStackHelper.getFluidId(stack)")
                                && api.contains("CustomFluidStackHelper.getDefinition(stack)")
                                && api.contains("CustomFluidStackHelper.hasSameFluidId(left, right)"),
                        "The public API should be a stable wrapper over the existing stack identity implementation"),
                () -> assertTrue(helper.contains("stack.set(CustomFluidDataComponents.CUSTOM_FLUID_ID.get(), fluidId.toString())"),
                        "NeoForge FluidStack identity should continue to use data components"),
                () -> assertFalse(api.contains("HotbathWaterloggingHelper"),
                        "Public CustomFluidAPI should not expose waterlogging storage internals"),
                () -> assertTrue(docs.contains("createCustomFluidStack")
                                && docs.contains("getCustomFluidId")
                                && docs.contains("getCustomFluidDefinition"),
                        "Developer docs should mention the public custom FluidStack identity API")
        );
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
