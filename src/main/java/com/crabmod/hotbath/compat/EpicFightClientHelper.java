package com.crabmod.hotbath.compat;

import com.crabmod.hotbath.dirtiness.DirtinessOverlayRenderer;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import yesman.epicfight.api.client.forgeevent.PatchedRenderersEvent;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.FirstPersonRenderer;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;

@SuppressWarnings({"unchecked", "rawtypes"})
public class EpicFightClientHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static FirstPersonRenderer registeredFirstPersonRenderer;

    static void registerLayers(IEventBus modEventBus) {
        modEventBus.addListener(EpicFightClientHelper::onModifyPatchedRenderers);
        MinecraftForge.EVENT_BUS.register(EpicFightClientHelper.class);
    }

    private static void onModifyPatchedRenderers(PatchedRenderersEvent.Modify event) {
        CompatManager.safeEventCall("epicfight", "registerDirtinessLayer", () -> {
            if (event.get(EntityType.PLAYER) instanceof PPlayerRenderer playerRenderer) {
                playerRenderer.addPatchedLayerAlways(DirtinessOverlayRenderer.class,
                        new EpicFightDirtinessPatchedLayer());
                LOGGER.info("Registered dirtiness layer on Epic Fight player renderer (3rd person)");
            }

            registerFirstPersonLayerIfAvailable();
        });
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        CompatManager.safeEventCall("epicfight", "registerFirstPersonDirtinessLayer",
                EpicFightClientHelper::registerFirstPersonLayerIfAvailable);
    }

    private static void registerFirstPersonLayerIfAvailable() {
        ClientEngine clientEngine = ClientEngine.getInstance();
        RenderEngine renderEngine = clientEngine != null ? clientEngine.renderEngine : null;
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
