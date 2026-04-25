package com.iridium126.hextricks.util;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ListPatternIotaParser {
    private static final Pattern FORMATTING_CODE_REGEX = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
    private static final String PATTERN_PREFIX = "HexPattern[";

    private ListPatternIotaParser() {
    }

    public static Optional<Iota> restoreFromDisplay(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String stripped = FORMATTING_CODE_REGEX.matcher(input).replaceAll("").trim();
        if (stripped.isEmpty()) {
            return Optional.empty();
        }

        Iota parsedList = parseListToken(stripped);
        if (parsedList != null) {
            return Optional.of(parsedList);
        }

        Iota parsedPattern = parsePatternToken(stripped);
        return Optional.ofNullable(parsedPattern);
    }

    private static ListIota parseListToken(String token) {
        if (token.length() < 2 || token.charAt(0) != '[' || token.charAt(token.length() - 1) != ']') {
            return null;
        }

        String content = token.substring(1, token.length() - 1).trim();
        if (content.isEmpty()) {
            return new ListIota(List.of());
        }

        List<Iota> parsed = new ArrayList<>();
        int index = 0;
        while (index < content.length()) {
            index = skipDelimiters(content, index);
            if (index >= content.length()) {
                break;
            }

            ParseResult result = parseNextElement(content, index);
            if (result == null) {
                return null;
            }

            parsed.add(result.iota());
            index = result.nextIndex();
        }

        return new ListIota(parsed);
    }

    private static PatternIota parsePatternToken(String token) {
        int patternStart = token.indexOf(PATTERN_PREFIX);
        if (patternStart < 0) {
            return null;
        }

        int patternEnd = token.lastIndexOf(']');
        if (patternEnd < patternStart + PATTERN_PREFIX.length()) {
            return null;
        }

        if (!token.substring(patternEnd + 1).trim().isEmpty()) {
            return null;
        }

        String payload = token.substring(patternStart + PATTERN_PREFIX.length(), patternEnd);
        int commaIndex = payload.indexOf(',');
        if (commaIndex < 0) {
            return null;
        }

        String startDirRaw = payload.substring(0, commaIndex).trim();
        String signatureRaw = payload.substring(commaIndex + 1).trim();

        try {
            HexDir startDir = HexDir.valueOf(startDirRaw);
            HexPattern pattern = HexPattern.fromAngles(signatureRaw, startDir);
            return new PatternIota(pattern);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return null;
        }
    }

    private static ParseResult parseNextElement(String content, int index) {
        char c = content.charAt(index);
        if (c == '[') {
            int end = findMatchingBracket(content, index);
            if (end < 0) {
                return null;
            }

            ListIota nested = parseListToken(content.substring(index, end + 1));
            if (nested == null) {
                return null;
            }
            return new ParseResult(nested, end + 1);
        }

        if (content.startsWith(PATTERN_PREFIX, index)) {
            int end = content.indexOf(']', index + PATTERN_PREFIX.length());
            if (end < 0) {
                return null;
            }

            PatternIota pattern = parsePatternToken(content.substring(index, end + 1));
            if (pattern == null) {
                return null;
            }
            return new ParseResult(pattern, end + 1);
        }

        return null;
    }

    private static int skipDelimiters(String content, int index) {
        int i = index;
        while (i < content.length()) {
            char c = content.charAt(i);
            if (Character.isWhitespace(c) || c == ',') {
                i++;
                continue;
            }
            break;
        }
        return i;
    }

    private static int findMatchingBracket(String content, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
                if (depth < 0) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private record ParseResult(Iota iota, int nextIndex) {
    }
}
