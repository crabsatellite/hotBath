package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.dirtiness.DirtinessOverlayRenderer;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;

@SuppressWarnings({"unchecked", "rawtypes"})
class EpicFightClientHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    static void registerLayers() {
        EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY.registerEvent(event -> {
            CompatManager.safeEventCall("epicfight", "registerDirtinessLayer", () -> {
                if (event.get(EntityType.PLAYER) instanceof PPlayerRenderer playerRenderer) {
                    playerRenderer.addPatchedLayer(DirtinessOverlayRenderer.class,
                            new EpicFightDirtinessPatchedLayer());
                    LOGGER.info("Registered dirtiness layer on Epic Fight player renderer (3rd person)");
                }

                FirstPersonRenderer fpRenderer = RenderEngine.getInstance().getFirstPersonRenderer();
                if (fpRenderer != null) {
                    fpRenderer.addPatchedLayer(DirtinessOverlayRenderer.class,
                            new EpicFightDirtinessPatchedLayer());
                    LOGGER.info("Registered dirtiness layer on Epic Fight first-person renderer");
                }
            });
        });
    }
}
