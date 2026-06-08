package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.dirtiness.DirtinessOverlayRenderer;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;

@SuppressWarnings({"unchecked", "rawtypes"})
class EpicFightClientHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static FirstPersonRenderer registeredFirstPersonRenderer;

    static void registerLayers() {
        NeoForge.EVENT_BUS.addListener(EpicFightClientHelper::onClientTick);

        EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY.registerEvent(event -> {
            CompatManager.safeEventCall("epicfight", "registerDirtinessLayer", () -> {
                // Replace Epic Fight's default RenderOriginalModelLayer fallback if it already saw our vanilla layer.
                if (event.get(EntityType.PLAYER) instanceof PPlayerRenderer playerRenderer) {
                    playerRenderer.addPatchedLayerAlways(DirtinessOverlayRenderer.class,
                            new EpicFightDirtinessPatchedLayer());
                    LOGGER.info("Registered dirtiness layer on Epic Fight player renderer (3rd person)");
                }

                registerFirstPersonLayerIfAvailable();
            });
        });
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        CompatManager.safeEventCall("epicfight", "registerFirstPersonDirtinessLayer",
                EpicFightClientHelper::registerFirstPersonLayerIfAvailable);
    }

    private static void registerFirstPersonLayerIfAvailable() {
        RenderEngine renderEngine = RenderEngine.getInstance();
        if (renderEngine == null) {
            return;
        }

        FirstPersonRenderer fpRenderer = renderEngine.getFirstPersonRenderer();
        if (fpRenderer == null || fpRenderer == registeredFirstPersonRenderer) {
            return;
        }

        fpRenderer.addPatchedLayerAlways(DirtinessOverlayRenderer.class,
                new EpicFightDirtinessPatchedLayer(true));
        registeredFirstPersonRenderer = fpRenderer;
        LOGGER.info("Registered dirtiness layer on Epic Fight first-person renderer");
    }
}
