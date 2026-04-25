package com.iridium126.hextricks.util;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class ListPatternIotaValidator {
    // PatternIota uses GOLD (§6) formatting
    private static final Pattern PATTERN_IOTA_REGEX = Pattern.compile(
            "^§6(EAST|WEST|NORTHEAST|NORTHWEST|SOUTHEAST|SOUTHWEST) [aqweds]*$"
    );

    private ListPatternIotaValidator() {
    }

    public static boolean isListOrPatternIotaDisplay(@NotNull String input) {
        if (PATTERN_IOTA_REGEX.matcher(input).matches()) {
            return true;
        }

        return isListIotaDisplay(input);
    }

    /**
     * Check if input matches ListIota.display().getString() format.
     * ListIota uses DARK_PURPLE (§5) and wraps content in [].
     */
    private static boolean isListIotaDisplay(@NotNull String text) {
        // Must start with §5[ and end with ]
        if (!text.startsWith("§5[") || !text.endsWith("]")) {
            return false;
        }

        // Check bracket balance in the content (excluding the outer §5[ and ])
        int bracketDepth = 0;
        for (char c : text.toCharArray()) {
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
