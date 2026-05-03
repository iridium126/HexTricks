package com.iridium126.hextricks.util;

import org.jetbrains.annotations.NotNull;
import java.util.regex.Pattern;

public final class ListPatternIotaValidator {
    private static final Pattern FORMATTING_CODE_REGEX = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
    private static final Pattern PATTERN_IOTA_REGEX = Pattern.compile(
            "^(.* )?HexPattern\\[[A-Z_]+, [aqweds]*\\]$"
    );

    private ListPatternIotaValidator() {
    }

    public static boolean isListOrPatternIotaDisplay(@NotNull String input) {
        String stripped = FORMATTING_CODE_REGEX.matcher(input).replaceAll("");
        
        if (PATTERN_IOTA_REGEX.matcher(stripped).matches()) {
            return true;
        }

        return isListIotaPureText(stripped);
    }

    public static boolean isListIotaDisplay(@NotNull String input) {
        String stripped = FORMATTING_CODE_REGEX.matcher(input).replaceAll("");
        return isListIotaPureText(stripped);
    }

    private static boolean isListIotaPureText(@NotNull String text) {
        if (text.length() < 2) {
            return false;
        }
        if (text.charAt(0) != '[' || text.charAt(text.length() - 1) != ']') {
            return false;
        }

        int bracketDepth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') {
                bracketDepth++;
            } else if (c == ']') {
                bracketDepth--;
                if (bracketDepth < 0) {
                    return false;
                }
            }
        }

        return bracketDepth == 0;
    }
}
