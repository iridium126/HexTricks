package com.iridium126.hextricks.mixin;

import at.petrak.hexcasting.api.casting.iota.BooleanIota;
import at.petrak.hexcasting.api.casting.iota.GarbageIota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import com.iridium126.hextricks.util.ListPatternIotaValidator;
import dev.enjarai.trickster.spell.fragment.StringFragment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.Locale;
import java.util.regex.Pattern;

@Mixin(value = StringFragment.class, remap = false)
public abstract class StringFragmentMixin {
    @Unique
    private static final String PATTERN_PREFIX = "HexPattern[";
    @Unique
    private static final String ENTITY_METADATA_PREFIX = "<hextricks:entity:";
    @Unique
    private static final String CONTINUATION_METADATA_PREFIX = "<hextricks:continuation:";
    @Unique
    private static final Pattern NUMBER_TOKEN_REGEX = Pattern.compile("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?");
    @Unique
    private static final Pattern VEC3_TOKEN_REGEX = Pattern.compile(
            "^\\(\\s*([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?)\\s*,\\s*([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?)\\s*,\\s*([-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?)\\s*\\)$"
    );

    @Shadow
    public abstract String value();

    @Overwrite
    public Component asText() {
        String value = value();
        if (ListPatternIotaValidator.isListOrPatternIotaDisplay(value)) {
            return colorizeListOrPatternIotaDisplay(value);
        }
        else {
            return Component.literal("\"").append(value).append("\"");
        }
    }

    @Unique
    private static Component colorizeListOrPatternIotaDisplay(String text) {
        if (text.length() >= 2 && text.charAt(0) == '[' && text.charAt(text.length() - 1) == ']') {
            return colorizeListToken(text);
        }

        int patternIndex = text.indexOf(PATTERN_PREFIX);
        if (patternIndex >= 0) {
            return colorizePatternToken(text, patternIndex);
        }

        return Component.literal(text);
    }

    @Unique
    private static Component colorizeListToken(String text) {
        if (text.length() < 2) {
            return Component.literal(text).withStyle(ChatFormatting.DARK_PURPLE);
        }

        MutableComponent out = Component.empty();
        out.append(Component.literal("[").withStyle(ChatFormatting.DARK_PURPLE));

        String inner = text.substring(1, text.length() - 1);
        int index = 0;
        while (index < inner.length()) {
            char c = inner.charAt(index);

            if (c == ',') {
                out.append(Component.literal(",").withStyle(ChatFormatting.DARK_PURPLE));
                index++;
                continue;
            }

            if (Character.isWhitespace(c)) {
                int start = index;
                while (index < inner.length() && Character.isWhitespace(inner.charAt(index))) {
                    index++;
                }
                out.append(Component.literal(inner.substring(start, index)).withStyle(ChatFormatting.DARK_PURPLE));
                continue;
            }

            int next = findNextTopLevelComma(inner, index);
            out.append(colorizeListElement(inner.substring(index, next)));
            index = next;
        }

        out.append(Component.literal("]").withStyle(ChatFormatting.DARK_PURPLE));
        return out;
    }

    @Unique
    private static int findNextTopLevelComma(String text, int start) {
        int bracketDepth = 0;
        int parenthesisDepth = 0;
        boolean inMetadata = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (!inMetadata && c == '<' && text.startsWith("<hextricks:", i)) {
                inMetadata = true;
            }
            if (inMetadata) {
                if (c == '>') {
                    inMetadata = false;
                }
                continue;
            }

            if (c == '[') {
                bracketDepth++;
            }
            else if (c == ']') {
                bracketDepth--;
            }
            else if (c == '(') {
                parenthesisDepth++;
            }
            else if (c == ')' && parenthesisDepth > 0) {
                parenthesisDepth--;
            }
            else if (c == ',' && bracketDepth == 0 && parenthesisDepth == 0) {
                return i;
            }
        }

        return text.length();
    }

    @Unique
    private static Component colorizeListElement(String element) {
        int leading = 0;
        while (leading < element.length() && Character.isWhitespace(element.charAt(leading))) {
            leading++;
        }

        int trailing = element.length() - 1;
        while (trailing >= leading && Character.isWhitespace(element.charAt(trailing))) {
            trailing--;
        }

        MutableComponent out = Component.empty();
        if (leading > 0) {
            out.append(Component.literal(element.substring(0, leading)).withStyle(ChatFormatting.DARK_PURPLE));
        }

        if (trailing >= leading) {
            String core = element.substring(leading, trailing + 1);
            out.append(colorizeElementCore(core));
        }

        if (trailing + 1 < element.length()) {
            out.append(Component.literal(element.substring(trailing + 1)).withStyle(ChatFormatting.DARK_PURPLE));
        }

        return out;
    }

    @Unique
    private static Component colorizeElementCore(String core) {
        if (core.length() >= 2 && core.charAt(0) == '[' && core.charAt(core.length() - 1) == ']') {
            return colorizeListToken(core);
        }

        int patternIndex = core.indexOf(PATTERN_PREFIX);
        if (patternIndex >= 0) {
            return colorizePatternToken(core, patternIndex);
        }

        if (core.contains(ENTITY_METADATA_PREFIX) || core.contains(CONTINUATION_METADATA_PREFIX)) {
            return Component.literal(core);
        }

        String lower = core.toLowerCase(Locale.ROOT);
        if (NUMBER_TOKEN_REGEX.matcher(core).matches()) {
            return Component.literal(core).withStyle(ChatFormatting.GREEN);
        }
        if (VEC3_TOKEN_REGEX.matcher(core).matches()) {
            return Component.literal(core).withStyle(ChatFormatting.RED);
        }

        String trueDisplay = BooleanIota.display(true).getString().toLowerCase(Locale.ROOT);
        if (lower.equals(trueDisplay)) {
            return Component.literal(core).withStyle(ChatFormatting.DARK_GREEN);
        }

        String falseDisplay = BooleanIota.display(false).getString().toLowerCase(Locale.ROOT);
        if (lower.equals(falseDisplay)) {
            return Component.literal(core).withStyle(ChatFormatting.DARK_RED);
        }

        String nullDisplay = NullIota.DISPLAY.getString().toLowerCase(Locale.ROOT);
        if (lower.equals(nullDisplay)) {
            return Component.literal(core).withStyle(ChatFormatting.GRAY);
        }

        String garbageDisplay = GarbageIota.DISPLAY.getString().toLowerCase(Locale.ROOT);
        if (lower.equals(garbageDisplay)) {
            return Component.literal(core).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.OBFUSCATED);
        }

        String trickDisplayPrefix = Component.translatable("hextricks.iota.trick").getString();
        if (core.startsWith(trickDisplayPrefix)) {
            return Component.literal(core).withStyle(ChatFormatting.YELLOW);
        }

        return Component.literal(core);
    }

    @Unique
    private static Component colorizePatternToken(String token, int patternIndex) {
        int patternEnd = token.lastIndexOf(']');
        if (patternEnd < patternIndex) {
            return Component.literal(token);
        }

        MutableComponent out = Component.empty();

        String prefix = token.substring(0, patternIndex);
        if (!prefix.isEmpty()) {
            out.append(Component.literal(prefix).withStyle(ChatFormatting.GOLD));
        }

        out.append(Component.literal(token.substring(patternIndex, patternEnd + 1)).withStyle(ChatFormatting.WHITE));

        if (patternEnd + 1 < token.length()) {
            out.append(Component.literal(token.substring(patternEnd + 1)).withStyle(ChatFormatting.GOLD));
        }

        return out;
    }
}
