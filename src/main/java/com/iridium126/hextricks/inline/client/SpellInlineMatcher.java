package com.iridium126.hextricks.inline.client;

import com.iridium126.hextricks.HexTricks;
import com.iridium126.hextricks.inline.SpellInlineData;
import com.samsthenerd.inline.api.client.InlineClientAPI;
import com.samsthenerd.inline.api.matching.InlineMatch;
import com.samsthenerd.inline.api.matching.MatcherInfo;
import com.samsthenerd.inline.api.matching.RegexMatcher;
import net.minecraft.resources.ResourceLocation;

import java.util.regex.Pattern;

public final class SpellInlineMatcher {
    private static final Pattern SPELL_PATTERN = Pattern.compile("(?<!\\\\)\\[spell([:.,!+])([A-Za-z0-9+/]+={0,2})\\]");
    private static final ResourceLocation ID = HexTricks.id("spell");

    private SpellInlineMatcher() {
    }

    public static void register() {
        InlineClientAPI.INSTANCE.addMatcher(new RegexMatcher.Simple(
                SPELL_PATTERN,
                ID,
                match -> matchInline(match.group(1), match.group(2)),
                MatcherInfo.fromId(ID)
        ));
    }

    private static InlineMatch matchInline(String separator, String base64) {
        return SpellInlineData.fromBase64(base64, scaleFromSeparator(separator))
                .<InlineMatch>map(InlineMatch.DataMatch::new)
                .orElse(null);
    }

    private static float scaleFromSeparator(String separator) {
        return switch (separator) {
            case "." -> 0.1f;
            case "," -> 0.75f;
            case "!" -> 1.5f;
            case "+" -> 2f;
            default -> 1f;
        };
    }
}
