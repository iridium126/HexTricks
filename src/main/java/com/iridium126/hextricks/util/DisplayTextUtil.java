package com.iridium126.hextricks.util;

import java.util.regex.Pattern;

public final class DisplayTextUtil {
    private static final Pattern FORMATTING_CODE_REGEX = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");

    private DisplayTextUtil() {
    }

    public static String stripFormatting(String text) {
        if (text == null || text.indexOf('\u00A7') < 0) {
            return text;
        }
        return FORMATTING_CODE_REGEX.matcher(text).replaceAll("");
    }

    public static String normalizeToken(String token) {
        String stripped = stripFormatting(token);
        return stripped == null ? "" : stripped.trim();
    }
}
