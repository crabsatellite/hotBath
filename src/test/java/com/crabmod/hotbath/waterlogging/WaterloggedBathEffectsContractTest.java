package com.crabmod.hotbath.waterlogging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterloggedBathEffectsContractTest {
    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    @Test
    void waterloggedPlayerTicksDispatchThroughSharedBathEffectEntryPoint() throws IOException {
        String handler = read("src/main/java/com/crabmod/hotbath/waterlogging/WaterloggedBathEffectsHandler.java");
        String abstractBlock = read("src/main/java/com/crabmod/hotbath/fluid_blocks/AbstractHotbathBlock.java");

        int methodStart = abstractBlock.indexOf("public void applyWaterloggedEntityInside");
        int methodEnd = abstractBlock.indexOf("protected void applyBathEffects", methodStart);
        String waterloggedEntryPoint = abstractBlock.substring(methodStart, methodEnd);

        assertAll(
                () -> assertTrue(handler.contains("PlayerTickEvent.Post"),
                        "Waterlogged bath effects should run from a server-side player tick"),
                () -> assertTrue(handler.contains("BlockStateProperties.WATERLOGGED"),
                        "Only actual waterlogged blocks should dispatch stored bath effects"),
                () -> assertTrue(handler.contains("HotbathWaterloggingHelper.getStoredFluidType"),
                        "The dispatcher should use the stored HotBath fluid type, not vanilla water"),
                () -> assertTrue(handler.contains("candidatePositions(player)"),
                        "The dispatcher should cover seated and intersecting player positions"),
                () -> assertTrue(handler.contains("applyWaterloggedEntityInside"),
                        "Waterlogged blocks should reuse the same bath-effect entrypoint as real fluids"),
                () -> assertTrue(waterloggedEntryPoint.contains("applyBathEffects(level, pos, entity);"),
                        "Waterlogged dispatch should reach subclass bath effects"),
                () -> assertFalse(waterloggedEntryPoint.contains("super.entityInside"),
                        "Waterlogged dispatch must not invoke LiquidBlock physics for a block that is not actually placed")
        );
    }

    @Test
    void builtInBathBlocksShareTheExtractedEffectEntryPoint() throws IOException {
        for (String path : List.of(
                "src/main/java/com/crabmod/hotbath/fluid_blocks/HotWaterBlock.java",
                "src/main/java/com/crabmod/hotbath/fluid_blocks/HoneyBathBlock.java",
                "src/main/java/com/crabmod/hotbath/fluid_blocks/MilkBathBlock.java",
                "src/main/java/com/crabmod/hotbath/fluid_blocks/HerbalBathBlock.java",
                "src/main/java/com/crabmod/hotbath/fluid_blocks/PeonyBathBlock.java",
                "src/main/java/com/crabmod/hotbath/fluid_blocks/RoseBathBlock.java",
                "src/main/java/com/crabmod/hotbath/custom_fluid/DynamicCustomFluidBlock.java"
        )) {
            String source = read(path);
            assertTrue(source.contains("protected void applyBathEffects"),
                    path + " should expose bath behavior through the shared effect entrypoint");
            assertFalse(source.contains("public void entityInside"),
                    path + " should not keep a second player-effect path after extraction");
        }
    }

    @Test
    void waterloggedFluidMappingsCoverBuiltInAndDynamicBaths() throws IOException {
        String handler = read("src/main/java/com/crabmod/hotbath/waterlogging/WaterloggedBathEffectsHandler.java");
        String dynamicBlock = read("src/main/java/com/crabmod/hotbath/custom_fluid/DynamicCustomFluidBlock.java");
        String customFluidHandler = read("src/main/java/com/crabmod/hotbath/util/CustomFluidHandler.java");

        assertAll(
                () -> assertTrue(handler.contains("FluidsRegister.HOT_WATER_FLUID.get()")
                                && handler.contains("FluidsRegister.HOT_WATER_BLOCK.get()")),
                () -> assertTrue(handler.contains("FluidsRegister.HONEY_BATH_FLUID.get()")
                                && handler.contains("FluidsRegister.HONEY_BATH_BLOCK.get()")),
                () -> assertTrue(handler.contains("FluidsRegister.MILK_BATH_FLUID.get()")
                                && handler.contains("FluidsRegister.MILK_BATH_BLOCK.get()")),
                () -> assertTrue(handler.contains("FluidsRegister.HERBAL_BATH_FLUID.get()")
                                && handler.contains("FluidsRegister.HERBAL_BATH_BLOCK.get()")),
                () -> assertTrue(handler.contains("FluidsRegister.PEONY_BATH_FLUID.get()")
                                && handler.contains("FluidsRegister.PEONY_BATH_BLOCK.get()")),
                () -> assertTrue(handler.contains("FluidsRegister.ROSE_BATH_FLUID.get()")
                                && handler.contains("FluidsRegister.ROSE_BATH_BLOCK.get()")),
                () -> assertTrue(handler.contains("DynamicFluidRegistry.DYNAMIC_FLUID_STILL.get()")
                                && handler.contains("CustomFluidBlocksRegister.CUSTOM_FLUID_BLOCK.get()"),
                        "Datapack custom bath fluids should dispatch through the dynamic fluid block"),
                () -> assertTrue(dynamicBlock.contains("HotbathWaterloggingHelper.getStoredCustomFluidId(level, pos)")
                                && dynamicBlock.contains("CustomFluidAPI.getFluidDefinition(customFluidId)"),
                        "Dynamic waterlogged fluids should recover their stored datapack definition"),
                () -> assertTrue(customFluidHandler.contains("getStoredBathFluid(Level level, BlockPos pos)")
                                && customFluidHandler.contains("HotbathWaterloggingHelper.getStoredFluidType(level, pos)")
                                && customFluidHandler.contains("getStoredCustomDefinition"),
                        "Temperature and compat helpers should recognize waterlogged stored bath fluids")
        );
    }
}
