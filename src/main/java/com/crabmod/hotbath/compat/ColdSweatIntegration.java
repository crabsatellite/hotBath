package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.registers.FluidsRegister;
import com.mojang.logging.LogUtils;
import com.momosoftworks.coldsweat.api.event.core.registry.BlockTempRegisterEvent;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;

/**
 * Integration with Cold Sweat mod
 * Provides temperature effects for hot bath blocks
 * Uses Cold Sweat's event system to register block temperatures
 */
@EventBusSubscriber(modid = "hotbath")
public class ColdSweatIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COLD_SWEAT_MOD_ID = "cold_sweat";

    /**
     * Check if Cold Sweat mod is loaded
     */
    public static boolean isColdSweatLoaded() {
        return ModList.get().isLoaded(COLD_SWEAT_MOD_ID);
    }

    /**
     * Event handler for Cold Sweat's BlockTempRegisterEvent
     * This is called when Cold Sweat is ready to accept block temperature registrations
     */
    @SubscribeEvent
    public static void onBlockTempRegister(BlockTempRegisterEvent event) {
        if (!isColdSweatLoaded()) {
            return;
        }

        try {
            LOGGER.info("Registering Hot Bath blocks with Cold Sweat via BlockTempRegisterEvent...");

            // Get all hot bath blocks
            Block hotWaterBlock = FluidsRegister.HOT_WATER_BLOCK.get();
            Block herbalBathBlock = FluidsRegister.HERBAL_BATH_BLOCK.get();
            Block honeyBathBlock = FluidsRegister.HONEY_BATH_BLOCK.get();
            Block milkBathBlock = FluidsRegister.MILK_BATH_BLOCK.get();
            Block peonyBathBlock = FluidsRegister.PEONY_BATH_BLOCK.get();
            Block roseBathBlock = FluidsRegister.ROSE_BATH_BLOCK.get();

            // Register each bath type with Cold Sweat
            // All bath blocks provide the same warmth level
            event.register(new HotBathTempModifier(HotBathTempModifier.BASE_TEMPERATURE, hotWaterBlock));
            event.register(new HotBathTempModifier(HotBathTempModifier.BASE_TEMPERATURE, herbalBathBlock));
            event.register(new HotBathTempModifier(HotBathTempModifier.BASE_TEMPERATURE, honeyBathBlock));
            event.register(new HotBathTempModifier(HotBathTempModifier.BASE_TEMPERATURE, milkBathBlock));
            event.register(new HotBathTempModifier(HotBathTempModifier.BASE_TEMPERATURE, peonyBathBlock));
            event.register(new HotBathTempModifier(HotBathTempModifier.BASE_TEMPERATURE, roseBathBlock));

            LOGGER.info("Successfully registered 6 hot bath block temperatures with Cold Sweat!");
        } catch (Exception e) {
            LOGGER.error("Failed to register Hot Bath blocks with Cold Sweat: {}", e.getMessage(), e);
        }
    }
}
