package com.iridium126.hextricks.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public final class FragmentTextAccessor {
    private static final ClassValue<Method> AS_FORMATTED_TEXT_METHODS = new ClassValue<>() {
        @Override
        protected Method computeValue(@NotNull Class<?> type) {
            try {
                return type.getMethod("asFormattedText");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Missing asFormattedText method on fragment type: " + type.getName(), e);
            }
        }
    };

    private static final ClassValue<Method> GET_STRING_METHODS = new ClassValue<>() {
        @Override
        protected Method computeValue(@NotNull Class<?> type) {
            try {
                return type.getMethod("getString");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Missing getString method on text type: " + type.getName(), e);
            }
        }
    };

    private FragmentTextAccessor() {
    }

    public static String getFormattedTextString(Object fragment) {
        if (fragment == null) {
            return "";
        }

        try {
            Method asFormattedText = AS_FORMATTED_TEXT_METHODS.get(fragment.getClass());
            Object text = asFormattedText.invoke(fragment);
            if (text == null) {
                return "";
            }

            Method getString = GET_STRING_METHODS.get(text.getClass());
            Object value = getString.invoke(text);
            return value instanceof String string ? string : "";
        } catch (Throwable ignored) {
            return "";
        }
    }
}
