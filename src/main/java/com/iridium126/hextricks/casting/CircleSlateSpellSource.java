package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import com.iridium126.hextricks.HexTricks;
import com.iridium126.hextricks.compat.SlateKnotInventory;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class CircleSlateSpellSource {
    private CircleSlateSpellSource() {
    }

    static Object wrap(Object delegate, CircleCastEnv env) {
        try {
            BlockEntityAbstractImpetus impetus = env.getImpetus();
            if (impetus == null || !(impetus.getLevel() instanceof ServerLevel level)) {
                return delegate;
            }

            SlateKnotInventory inventory = SlateKnotInventory.forCircle(level, env.circleState().reachedPositions);
            Object pool = TricksterReflection.cachedInventoryManaPoolCtor.newInstance(inventory);
            Object syncingPool = Proxy.newProxyInstance(
                    TricksterReflection.mutableManaPoolClass.getClassLoader(),
                    new Class<?>[]{TricksterReflection.mutableManaPoolClass},
                    (proxy, method, args) -> invokePool(pool, inventory, method, args)
            );

            return Proxy.newProxyInstance(
                    TricksterReflection.spellSourceClass.getClassLoader(),
                    new Class<?>[]{TricksterReflection.spellSourceClass},
                    (proxy, method, args) -> invokeSource(delegate, syncingPool, method, args)
            );
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to create circle slate Trickster spell source", t);
            return delegate;
        }
    }

    private static Object invokeSource(Object delegate, Object pool, Method method, Object[] args) throws Throwable {
        if ("getManaPool".equals(method.getName()) && method.getParameterCount() == 0) {
            return pool;
        }
        return invoke(method, delegate, args);
    }

    private static Object invokePool(Object pool, SlateKnotInventory inventory, Method method, Object[] args)
            throws Throwable {
        Object result = invoke(method, pool, args);
        if (isMutatingManaPoolMethod(method)) {
            inventory.syncChangedSlots();
        }
        return result;
    }

    private static boolean isMutatingManaPoolMethod(Method method) {
        String name = method.getName();
        return method.getParameterCount() > 0
                && ("use".equals(name) || "refill".equals(name) || "set".equals(name) || "setMax".equals(name));
    }

    private static Object invoke(Method method, Object target, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
