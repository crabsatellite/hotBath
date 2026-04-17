package com.crabmod.hotbath.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * API contract tests for Epic Fight integration.
 * Verifies that Epic Fight's key classes and methods have the signatures
 * our {@code EpicFightDirtinessPatchedLayer} and {@code EpicFightCompat} rely on.
 *
 * <p>All tests gracefully succeed when Epic Fight is not installed.
 */
@GameTestHolder("hotbath")
@PrefixGameTestTemplate(false)
public class EpicFightApiContractTest {

    private static boolean isEpicFightPresent() {
        return ModList.get().isLoaded("epicfight");
    }

    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_epicfight_patchedlayer_renderLayer_method(GameTestHelper helper) {
        if (!isEpicFightPresent()) { helper.succeed(); return; }
        try {
            Class<?> cls = Class.forName("yesman.epicfight.client.renderer.patched.layer.PatchedLayer");
            boolean found = false;
            for (Method m : cls.getDeclaredMethods()) {
                if ("renderLayer".equals(m.getName()) && Modifier.isPublic(m.getModifiers())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                helper.fail("PatchedLayer.renderLayer(public) not found — EpicFightDirtinessPatchedLayer will break");
                return;
            }
        } catch (ClassNotFoundException e) {
            helper.fail("PatchedLayer class not found on classpath: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_epicfight_pplayerrenderer_addPatchedLayer(GameTestHelper helper) {
        if (!isEpicFightPresent()) { helper.succeed(); return; }
        try {
            Class<?> cls = Class.forName("yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer");
            Method m = findMethod(cls, "addPatchedLayer", Class.class);
            if (m == null) {
                m = findMethod(cls, "addPatchedLayer");
            }
            if (m == null) {
                helper.fail("PPlayerRenderer.addPatchedLayer not found — EpicFightCompat registration will fail");
                return;
            }
        } catch (ClassNotFoundException e) {
            helper.fail("PPlayerRenderer class not found: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_epicfight_firstpersonrenderer_exists(GameTestHelper helper) {
        if (!isEpicFightPresent()) { helper.succeed(); return; }
        try {
            Class<?> cls = Class.forName("yesman.epicfight.client.renderer.FirstPersonRenderer");
            Method m = findMethod(cls, "addPatchedLayer", Class.class);
            if (m == null) {
                m = findMethod(cls, "addPatchedLayer");
            }
            if (m == null) {
                helper.fail("FirstPersonRenderer.addPatchedLayer not found — first-person compat will fail");
                return;
            }
        } catch (ClassNotFoundException e) {
            helper.fail("FirstPersonRenderer class not found: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_epicfight_renderengine_getFirstPersonRenderer(GameTestHelper helper) {
        if (!isEpicFightPresent()) { helper.succeed(); return; }
        try {
            Class<?> cls = Class.forName("yesman.epicfight.client.events.engine.RenderEngine");
            Method getInstance = cls.getDeclaredMethod("getInstance");
            if (!Modifier.isPublic(getInstance.getModifiers()) || !Modifier.isStatic(getInstance.getModifiers())) {
                helper.fail("RenderEngine.getInstance() is not public static");
                return;
            }
            Method getFP = cls.getDeclaredMethod("getFirstPersonRenderer");
            if (!Modifier.isPublic(getFP.getModifiers())) {
                helper.fail("RenderEngine.getFirstPersonRenderer() is not public");
                return;
            }
        } catch (NoSuchMethodException e) {
            helper.fail("RenderEngine method missing: " + e.getMessage());
            return;
        } catch (ClassNotFoundException e) {
            helper.fail("RenderEngine class not found: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_epicfight_modify_event_hook_exists(GameTestHelper helper) {
        if (!isEpicFightPresent()) { helper.succeed(); return; }
        try {
            Class<?> hooks = Class.forName("yesman.epicfight.api.client.event.EpicFightClientEventHooks");
            Class<?> registry = null;
            for (Class<?> inner : hooks.getDeclaredClasses()) {
                if ("Registry".equals(inner.getSimpleName())) {
                    registry = inner;
                    break;
                }
            }
            if (registry == null) {
                helper.fail("EpicFightClientEventHooks.Registry inner class not found");
                return;
            }
            Field field = registry.getDeclaredField("MODIFY_PATCHED_ENTITY");
            if (!Modifier.isPublic(field.getModifiers()) || !Modifier.isStatic(field.getModifiers())) {
                helper.fail("MODIFY_PATCHED_ENTITY is not public static");
                return;
            }
        } catch (NoSuchFieldException e) {
            helper.fail("MODIFY_PATCHED_ENTITY field not found: " + e.getMessage());
            return;
        } catch (ClassNotFoundException e) {
            helper.fail("EpicFightClientEventHooks not found: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty_1x1", timeoutTicks = 20)
    public static void test_epicfight_humanoidmesh_bodyparts(GameTestHelper helper) {
        if (!isEpicFightPresent()) { helper.succeed(); return; }
        try {
            Class<?> cls = Class.forName("yesman.epicfight.client.mesh.HumanoidMesh");
            String[] requiredParts = {
                "head", "torso", "leftArm", "rightArm", "leftLeg", "rightLeg",
                "hat", "jacket", "leftSleeve", "rightSleeve", "leftPants", "rightPants"
            };
            for (String part : requiredParts) {
                try {
                    Field f = cls.getDeclaredField(part);
                    if (!Modifier.isPublic(f.getModifiers())) {
                        helper.fail("HumanoidMesh." + part + " is not public");
                        return;
                    }
                } catch (NoSuchFieldException e) {
                    helper.fail("HumanoidMesh." + part + " field missing — dirt overlay part mapping will fail");
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            helper.fail("HumanoidMesh class not found: " + e.getMessage());
            return;
        }
        helper.succeed();
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... firstParamTypes) {
        for (Method m : cls.getMethods()) {
            if (!m.getName().equals(name)) continue;
            Class<?>[] params = m.getParameterTypes();
            if (firstParamTypes.length > 0) {
                if (params.length < firstParamTypes.length) continue;
                boolean match = true;
                for (int i = 0; i < firstParamTypes.length; i++) {
                    if (!firstParamTypes[i].isAssignableFrom(params[i])) {
                        match = false;
                        break;
                    }
                }
                if (!match) continue;
            }
            return m;
        }
        return null;
    }
}
