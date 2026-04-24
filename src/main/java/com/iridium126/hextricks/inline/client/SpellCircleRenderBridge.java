package com.iridium126.hextricks.inline.client;

import com.iridium126.hextricks.HexTricks;
import com.iridium126.hextricks.inline.SpellInlineBridge;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Function;

final class SpellCircleRenderBridge {
    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    private static Object renderer;
    private static Method renderPartMethod;

    private SpellCircleRenderBridge() {
    }

    static boolean renderSpell(Object spellPart, Object guiGraphics, float scale, float tickDelta, int width, int height) {
        if (spellPart == null || guiGraphics == null || !ensureInit()) {
            return false;
        }

        Object matrices = invokeNoArg(guiGraphics, "pose", "getMatrices");
        Object vertices = invokeNoArg(guiGraphics, "bufferSource", "getVertexConsumers");
        if (matrices == null || vertices == null) {
            return false;
        }

        float circleSize = SpellInlineBridge.hasSubParts(spellPart) ? 30f : 50f;
        float centerX = width / 2f;
        float centerY = height / 2f;

        try {
            invokeNoArg(matrices, "pushPose", "push");
            invoke(matrices, "translate", centerX, centerY, 0f);
            invoke(matrices, "scale", scale, scale, 1f);

            Function<Float, Float> alpha = value -> value / circleSize;
            renderPartMethod.invoke(
                    renderer,
                    matrices,
                    vertices,
                    spellPart,
                    0d,
                    0d,
                    (double) circleSize,
                    0d,
                    tickDelta,
                    alpha,
                    new Vec3(0, 0, -1)
            );
            return true;
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to render Trickster spell inline", t);
            return false;
        } finally {
            invokeNoArg(matrices, "popPose", "pop");
        }
    }

    private static synchronized boolean ensureInit() {
        if (initialized) {
            return available;
        }
        initialized = true;
        try {
            Class<?> rendererClass = Class.forName("dev.enjarai.trickster.render.SpellCircleRenderer");

            Constructor<?> ctor;
            try {
                ctor = rendererClass.getConstructor(Boolean.class, double.class);
            } catch (NoSuchMethodException ignored) {
                ctor = rendererClass.getConstructor(boolean.class, double.class);
            }
            renderer = ctor.newInstance(true, 1d);

            for (Method method : rendererClass.getMethods()) {
                if ("renderPart".equals(method.getName()) && method.getParameterCount() == 10) {
                    renderPartMethod = method;
                    break;
                }
            }
            available = renderPartMethod != null;
        } catch (Throwable ignored) {
            available = false;
        }
        return available;
    }

    private static Object invokeNoArg(Object target, String... names) {
        if (target == null) {
            return null;
        }
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean invoke(Object target, String name, Object... args) {
        if (target == null) {
            return false;
        }
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) {
                continue;
            }
            try {
                method.invoke(target, args);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }
}
