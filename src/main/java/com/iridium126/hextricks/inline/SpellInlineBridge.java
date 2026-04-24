package com.iridium126.hextricks.inline;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public final class SpellInlineBridge {
    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    private static Class<?> fragmentClass;
    private static Class<?> spellPartClass;
    private static Method fragmentFromBase64Method;
    private static Method fragmentToBase64Method;
    private static Constructor<?> spellPartCtor;
    private static Method spellPartSubPartsMethod;
    private static Field spellPartSubPartsField;

    private SpellInlineBridge() {
    }

    public static Optional<Object> decodeSpellPart(String base64) {
        try {
            if (!ensureInit() || base64 == null || base64.isBlank()) {
                return Optional.empty();
            }
            Object fragment = fragmentFromBase64Method.invoke(null, base64);
            if (fragment == null) {
                return Optional.empty();
            }
            if (spellPartClass.isInstance(fragment)) {
                return Optional.of(fragment);
            }
            return Optional.of(spellPartCtor.newInstance(fragment));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    public static boolean hasSubParts(Object spellPart) {
        if (spellPart == null || !ensureInit() || !spellPartClass.isInstance(spellPart)) {
            return false;
        }
        try {
            if (spellPartSubPartsMethod != null) {
                Object maybeList = spellPartSubPartsMethod.invoke(spellPart);
                if (maybeList instanceof List<?> list) {
                    return !list.isEmpty();
                }
            }
            if (spellPartSubPartsField != null) {
                Object maybeList = spellPartSubPartsField.get(spellPart);
                if (maybeList instanceof List<?> list) {
                    return !list.isEmpty();
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static synchronized boolean ensureInit() {
        if (initialized) {
            return available;
        }
        initialized = true;

        try {
            fragmentClass = Class.forName("dev.enjarai.trickster.spell.Fragment");
            spellPartClass = Class.forName("dev.enjarai.trickster.spell.SpellPart");

            fragmentFromBase64Method = fragmentClass.getMethod("fromBase64", String.class);
            fragmentToBase64Method = fragmentClass.getMethod("toBase64");
            spellPartCtor = spellPartClass.getConstructor(fragmentClass);

            try {
                spellPartSubPartsMethod = spellPartClass.getMethod("getSubParts");
            } catch (NoSuchMethodException ignored) {
                spellPartSubPartsMethod = null;
            }
            try {
                spellPartSubPartsField = spellPartClass.getField("subParts");
            } catch (NoSuchFieldException ignored) {
                spellPartSubPartsField = null;
            }

            available = true;
        } catch (Throwable ignored) {
            available = false;
        }
        return available;
    }
}
