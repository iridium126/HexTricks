package com.iridium126.hextricks.casting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

final class TricksterBridge {
    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    private static Method fragmentGetMethod;
    private static Method fragmentToBase64Method;
    private static Method fragmentFromBase64Method;
    private static Class<?> spellPartClass;
    private static Constructor<?> spellPartCtor;
    private static Field casterKeyField;
    private static Method componentKeyGetMethod;
    private static Method queueSpellAndCastMethod;

    private TricksterBridge() {
    }

    static String readSpellDataFromStack(ItemStack stack) {
        try {
            if (!ensureInit()) {
                return null;
            }

            Object fragmentOptional = fragmentGetMethod.invoke(null, stack);
            if (!(fragmentOptional instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }

            Object fragment = optional.get();
            Object base64 = fragmentToBase64Method.invoke(fragment);
            if (!(base64 instanceof String s) || s.isBlank()) {
                return null;
            }
            return s;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean tryQueueBySpellData(ServerPlayer player, String spellData) {
        try {
            if (!ensureInit() || spellData == null || spellData.isBlank()) {
                return false;
            }

            Object decoded = fragmentFromBase64Method.invoke(null, spellData);
            if (decoded == null) {
                return false;
            }

            Object spellPart = spellPartClass.isInstance(decoded) ? decoded : spellPartCtor.newInstance(decoded);

            Object casterKey = casterKeyField.get(null);
            Object casterComponent = componentKeyGetMethod.invoke(casterKey, player);
            if (casterComponent == null) {
                return false;
            }

            Object queueResult = queueSpellAndCastMethod.invoke(casterComponent, spellPart, List.of(), Optional.empty());
            return queueResult != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static synchronized boolean ensureInit() {
        if (initialized) {
            return available;
        }
        initialized = true;
        try {
            Class<?> fragmentComponentClass = Class.forName("dev.enjarai.trickster.item.component.FragmentComponent");
            Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            fragmentGetMethod = fragmentComponentClass.getMethod("getFragment", itemStackClass);

            Class<?> fragmentClass = Class.forName("dev.enjarai.trickster.spell.Fragment");
            fragmentToBase64Method = fragmentClass.getMethod("toBase64");
            fragmentFromBase64Method = fragmentClass.getMethod("fromBase64", String.class);

            spellPartClass = Class.forName("dev.enjarai.trickster.spell.SpellPart");
            spellPartCtor = spellPartClass.getConstructor(fragmentClass);

            Class<?> modEntityComponentsClass = Class.forName("dev.enjarai.trickster.cca.ModEntityComponents");
            casterKeyField = modEntityComponentsClass.getField("CASTER");

            Class<?> componentKeyClass = Class.forName("org.ladysnake.cca.api.v3.component.ComponentKey");
            componentKeyGetMethod = componentKeyClass.getMethod("get", Object.class);

            Class<?> casterComponentClass = Class.forName("dev.enjarai.trickster.cca.CasterComponent");
            queueSpellAndCastMethod = casterComponentClass.getMethod(
                    "queueSpellAndCast",
                    spellPartClass,
                    List.class,
                    Optional.class
            );

            available = true;
        } catch (Throwable ignored) {
            available = false;
        }
        return available;
    }
}
