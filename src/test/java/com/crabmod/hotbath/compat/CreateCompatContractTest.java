package com.crabmod.hotbath.compat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateCompatContractTest {

    @Test
    void createCompatIsRegisteredThroughApiFreeLoader() throws IOException {
        String source = read("src/main/java/com/crabmod/hotbath/HotBath.java");

        assertAll(
                () -> assertTrue(source.contains("import com.crabmod.hotbath.compat.CreateCompat;"),
                        "HotBath should import the API-free Create loader"),
                () -> assertFalse(source.contains("Create mod integration - temporarily disabled"),
                        "Create integration should not be left disabled"),
                () -> assertTrue(source.contains("\"create\""),
                        "Create compat should be registered with CompatManager"),
                () -> assertTrue(source.contains("CreateCompat::isCreateLoaded"),
                        "Create load check must avoid loading CreateIntegration before Create is present"),
                () -> assertTrue(source.contains("CreateCompat::init"),
                        "Create compat initializer should be registered"),
                () -> assertFalse(source.contains("CreateIntegration::isCreateLoaded"),
                        "CreateIntegration imports Create API and must not be used as the load check"),
                () -> assertTrue(source.contains("com.simibubi.create.api.effect.OpenPipeEffectHandler"),
                        "Create open pipe API should be verified before initialization"),
                () -> assertTrue(source.contains("com.simibubi.create.content.fluids.spout.FillingBySpout"),
                        "Create spout item filling support should be verified before initialization")
        );
    }

    @Test
    void createLoadCheckQueriesModListAndInitializationFailuresPropagate() throws IOException {
        String source = read("src/main/java/com/crabmod/hotbath/compat/CreateCompat.java");

        assertAll(
                () -> assertTrue(source.contains("return ModList.get().isLoaded(CREATE_MOD_ID);"),
                        "CreateCompat.isCreateLoaded should query the actual mod list, not the init flag"),
                () -> assertTrue(source.contains("catch (Throwable e)"),
                        "Create API linkage failures are Errors, not only Exceptions"),
                () -> assertTrue(source.contains("throw runtimeException;"),
                        "Runtime initialization failures must reach CompatManager"),
                () -> assertTrue(source.contains("throw error;"),
                        "NoClassDefFoundError and similar linkage errors must reach CompatManager")
        );
    }

    @Test
    void openPipeEffectsCoverSourceAndFlowingFluids() throws IOException {
        String source = read("src/main/java/com/crabmod/hotbath/compat/CreateIntegration.java");

        assertAll(
                () -> assertTrue(source.contains("private static void registerPipeEffect(Fluid sourceFluid, Fluid flowingFluid"),
                        "Open pipe handlers should be registered by a source/flowing helper"),
                () -> assertPipePair(source, "HOT_WATER_FLUID", "HOT_WATER_FLOWING"),
                () -> assertPipePair(source, "HONEY_BATH_FLUID", "HONEY_BATH_FLOWING"),
                () -> assertPipePair(source, "MILK_BATH_FLUID", "MILK_BATH_FLOWING"),
                () -> assertPipePair(source, "HERBAL_BATH_FLUID", "HERBAL_BATH_FLOWING"),
                () -> assertPipePair(source, "PEONY_BATH_FLUID", "PEONY_BATH_FLOWING"),
                () -> assertPipePair(source, "ROSE_BATH_FLUID", "ROSE_BATH_FLOWING"),
                () -> assertFalse(source.contains("import com.simibubi.create.api.behaviour.spouting.BlockSpoutingBehaviour;"),
                        "HotBath should not import block spouting API without a custom block behavior"),
                () -> assertFalse(source.contains("BlockSpoutingBehaviour.BY_BLOCK"),
                        "HotBath has no dedicated cauldron blocks, so spouts should rely on Create filling recipes")
        );
    }

    @Test
    void createRecipesUseNeoForgeCreateFormats() throws IOException {
        List<Path> recipes = listJson("src/main/resources/data/hotbath/recipe/create");

        assertEquals(11, recipes.size(), "HotBath should keep all Create mixing and filling recipes");
        for (Path recipe : recipes) {
            String json = read(recipe.toString());
            String name = recipe.getFileName().toString();

            assertAll(name,
                    () -> assertTrue(json.contains("\"neoforge:conditions\""),
                            "NeoForge 1.21 recipes should use neoforge:conditions"),
                    () -> assertTrue(json.contains("\"modid\": \"create\""),
                            "Create recipes should be gated on Create being loaded"),
                    () -> assertFalse(json.contains("\"forge:conditions\""),
                            "NeoForge recipes must not use Forge 1.20 condition keys"),
                    () -> assertFalse(json.contains("\"conditions\""),
                            "NeoForge recipes should use neoforge:conditions, not Forge's conditions key")
            );

            if (name.startsWith("filling_")) {
                assertAll(name,
                        () -> assertTrue(json.contains("\"type\": \"create:filling\""),
                                "Bottle recipes should be Create filling recipes"),
                        () -> assertTrue(json.contains("\"type\": \"neoforge:single\""),
                                "NeoForge Create filling recipes use SizedFluidIngredient JSON"),
                        () -> assertTrue(json.contains("\"id\": \"hotbath:"),
                                "NeoForge recipe results should use id")
                );
            } else if (name.startsWith("mixing_")) {
                assertAll(name,
                        () -> assertTrue(json.contains("\"type\": \"create:mixing\""),
                                "Bath fluid recipes should be Create mixing recipes"),
                        () -> assertTrue(json.contains("\"heat_requirement\": \"heated\""),
                                "Create 1.21 mixing recipes use snake_case heat_requirement"),
                        () -> assertTrue(json.contains("\"id\": \"hotbath:"),
                                "NeoForge fluid results should use id"),
                        () -> assertFalse(json.contains("\"heatRequirement\""),
                                "Create 1.21 mixing recipes must not use the Forge 1.20 key")
                );
            }
        }
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static List<Path> listJson(String path) throws IOException {
        try (Stream<Path> stream = Files.walk(Path.of(path))) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".json"))
                    .toList();
        }
    }

    private static void assertPipePair(String source, String sourceFluid, String flowingFluid) {
        assertTrue(source.contains(
                "registerPipeEffect(\n" +
                        "            FluidsRegister." + sourceFluid + ".get(),\n" +
                        "            FluidsRegister." + flowingFluid + ".get(),"),
                sourceFluid + " should register the same open pipe effect for " + flowingFluid);
    }
}
