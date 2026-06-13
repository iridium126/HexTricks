package com.iridium126.hextricks.util;

import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.BooleanIota;
import at.petrak.hexcasting.api.casting.iota.ContinuationIota;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.EntityIota;
import at.petrak.hexcasting.api.casting.iota.GarbageIota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.iota.Vec3Iota;
import com.iridium126.hextricks.casting.TrickIota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ListPatternIotaParser {
    private static final Pattern NUMBER_TOKEN_REGEX = Pattern.compile("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?");
    private static final Pattern VEC3_TOKEN_REGEX = Pattern.compile(
            "^\\(\\s*([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?)\\s*,\\s*([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?)\\s*,\\s*([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?)\\s*\\)$"
    );
    private static final String PATTERN_PREFIX = "HexPattern[";
    private static final String TRICK_DISPLAY_PREFIX = normalizeToken(new TrickIota("").display().getString());
    private static final String TRICK_DATA_PREFIX = "[spell.";
    private static final String TRUE_DISPLAY = normalizeToken(BooleanIota.display(true).getString()).toLowerCase(Locale.ROOT);
    private static final String FALSE_DISPLAY = normalizeToken(BooleanIota.display(false).getString()).toLowerCase(Locale.ROOT);
    private static final String NULL_DISPLAY = normalizeToken(new NullIota().display().getString()).toLowerCase(Locale.ROOT);
    private static final String GARBAGE_DISPLAY = normalizeToken(new GarbageIota().display().getString()).toLowerCase(Locale.ROOT);
    private static final String ENTITY_UNKNOWN_DISPLAY = normalizeToken(Component.translatable("hexcasting.spelldata.entity.whoknows").getString()).toLowerCase(Locale.ROOT);
    private static final String ENTITY_METADATA_PREFIX = "<hextricks:entity:";
    private static final String CONTINUATION_METADATA_PREFIX = "<hextricks:continuation:";
    private static final String METADATA_SUFFIX = ">";

    private ListPatternIotaParser() {
    }

    public static Optional<Iota> restoreFromDisplay(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        String stripped = normalizeToken(input);
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

    private static EntityIota parseEntityToken(String token) {
        String normalized = normalizeToken(token);
        if (normalized.isEmpty()) {
            return null;
        }

        MetadataToken metadataToken = extractTrailingMetadata(normalized, ENTITY_METADATA_PREFIX);
        if (metadataToken != null) {
            String decodedEntityId = MetadataEscaper.desanitize(metadataToken.metadataValue());
            UUID uuid;
            try {
                uuid = UUID.fromString(decodedEntityId);
            } catch (IllegalArgumentException ignored) {
                return null;
            }

            String displayToken = metadataToken.displayToken();
            String displayTokenLower = displayToken.toLowerCase(Locale.ROOT);
            if (ENTITY_UNKNOWN_DISPLAY.equals(displayTokenLower)) {
                return new EntityIota(uuid, null);
            }
            return new EntityIota(uuid, Component.literal(displayToken));
        }

        return null;
    }

    private static ParseResult parseNextElement(String content, int index) {
        char c = content.charAt(index);
        if (c == '[') {
            int end = findMatchingBracket(content, index);
            
            if (end >= 0 && (end + 1 >= content.length() || content.charAt(end + 1) != '<')) { 
                ListIota nested = parseListToken(content.substring(index, end + 1));
                if (nested == null) {
                    return null;
                }
                return new ParseResult(nested, end + 1);
            }
            // If not a valid list, fall through to parseScalarElement
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

        return parseScalarElement(content, index);
    }

    private static ParseResult parseScalarElement(String content, int index) {
        int end = findScalarElementEnd(content, index);
        if (end <= index) {
            return null;
        }

        String token = content.substring(index, end).trim();
        if (token.isEmpty()) {
            return null;
        }

        Iota scalar = parseScalarToken(token);
        if (scalar == null) {
            return null;
        }

        return new ParseResult(scalar, end);
    }

    private static int findScalarElementEnd(String content, int index) {
        int parenthesisDepth = 0;
        int bracketDepth = 0;
        int angleDepth = 0;
        for (int i = index; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '(') {
                parenthesisDepth++;
                continue;
            }
            if (c == ')') {
                parenthesisDepth--;
                if (parenthesisDepth < 0) {
                    return -1;
                }
                continue;
            }
            if (c == '[') {
                bracketDepth++;
                continue;
            }
            if (c == ']') {
                bracketDepth--;
                if (bracketDepth < 0) {
                    return -1;
                }
                continue;
            }
            if (c == '<') {
                angleDepth++;
                continue;
            }
            if (c == '>') {
                angleDepth--;
                if (angleDepth < 0) {
                    return -1;
                }
                continue;
            }
            if (c == ',' && parenthesisDepth == 0 && bracketDepth == 0 && angleDepth == 0) {
                return i;
            }
        }
        return (parenthesisDepth == 0 && bracketDepth == 0 && angleDepth == 0) ? content.length() : -1;
    }

    private static Iota parseScalarToken(String token) {
        Vec3Iota vec = parseVec3Token(token);
        if (vec != null) {
            return vec;
        }

        TrickIota trick = parseTrickToken(token);
        if (trick != null) {
            return trick;
        }

        ContinuationIota continuation = parseContinuationToken(token);
        if (continuation != null) {
            return continuation;
        }
        if (normalizeToken(token).contains(CONTINUATION_METADATA_PREFIX)) {
            return null;
        }

        if (NUMBER_TOKEN_REGEX.matcher(token).matches()) {
            try {
                return new DoubleIota(Double.parseDouble(token));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        String normalized = normalizeToken(token).toLowerCase(Locale.ROOT);
        if (TRUE_DISPLAY.equals(normalized)) {
            return new BooleanIota(true);
        }
        if (FALSE_DISPLAY.equals(normalized)) {
            return new BooleanIota(false);
        }
        if (NULL_DISPLAY.equals(normalized)) {
            return new NullIota();
        }
        if (GARBAGE_DISPLAY.equals(normalized)) {
            return new GarbageIota();
        }

        EntityIota entity = parseEntityToken(token);
        if (entity != null) {
            return entity;
        }
        if (normalizeToken(token).contains(ENTITY_METADATA_PREFIX)) {
            return null;
        }

        return null;
    }

    private static ContinuationIota parseContinuationToken(String token) {
        String normalized = normalizeToken(token);
        MetadataToken metadataToken = extractTrailingMetadata(normalized, CONTINUATION_METADATA_PREFIX);
        if (metadataToken == null) {
            return null;
        }

        SpellContinuation continuation = parseContinuationPayload(metadataToken.metadataValue());
        if (continuation == null) {
            return null;
        }

        return new ContinuationIota(continuation);
    }

    private static SpellContinuation parseContinuationPayload(String rawPayload) {
        String payload = MetadataEscaper.desanitize(rawPayload).trim();
        if (payload.isEmpty()) {
            return null;
        }

        if ("Done".equals(payload)) {
            return SpellContinuation.Done.INSTANCE;
        }

        try {
            var parsed = SpellContinuation.getCODEC()
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(payload));
            return parsed.result().orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static MetadataToken extractTrailingMetadata(String token, String prefix) {
        if (!token.endsWith(METADATA_SUFFIX)) {
            return null;
        }

        int metadataStart = token.lastIndexOf(prefix);
        if (metadataStart < 0) {
            return null;
        }

        int valueStart = metadataStart + prefix.length();
        int valueEnd = token.length() - METADATA_SUFFIX.length();
        if (valueEnd < valueStart) {
            return null;
        }

        String displayToken = token.substring(0, metadataStart).trim();
        if (displayToken.isEmpty()) {
            return null;
        }

        String metadataValue = token.substring(valueStart, valueEnd);
        return new MetadataToken(displayToken, metadataValue);
    }

    private static TrickIota parseTrickToken(String token) {
        String normalized = normalizeToken(token);
        if (!normalized.startsWith(TRICK_DISPLAY_PREFIX)) {
            return null;
        }

        String suffix = normalized.substring(TRICK_DISPLAY_PREFIX.length()).trim();
        if (!suffix.startsWith(TRICK_DATA_PREFIX) || !suffix.endsWith("]")) {
            return null;
        }

        int dataStart = TRICK_DATA_PREFIX.length();
        int dataEnd = suffix.length() - 1;
        if (dataEnd <= dataStart) {
            return null;
        }

        String spellData = suffix.substring(dataStart, dataEnd).trim();
        if (spellData.isEmpty()) {
            return null;
        }

        return new TrickIota(spellData);
    }

    private static Vec3Iota parseVec3Token(String token) {
        var matcher = VEC3_TOKEN_REGEX.matcher(token);
        if (!matcher.matches()) {
            return null;
        }

        try {
            double x = Double.parseDouble(matcher.group(1));
            double y = Double.parseDouble(matcher.group(2));
            double z = Double.parseDouble(matcher.group(3));
            return new Vec3Iota(new Vec3(x, y, z));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String normalizeToken(String token) {
        return DisplayTextUtil.normalizeToken(token);
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

    private record MetadataToken(String displayToken, String metadataValue) {
    }
}
