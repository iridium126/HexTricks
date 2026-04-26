package com.iridium126.hextricks.inline.client;

import com.iridium126.hextricks.HexTricks;
import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.data.EntityInlineData;
import com.samsthenerd.inline.api.data.PlayerHeadData;
import com.samsthenerd.inline.api.client.InlineClientAPI;
import com.samsthenerd.inline.api.matching.InlineMatch;
import com.samsthenerd.inline.api.matching.MatcherInfo;
import com.samsthenerd.inline.api.matching.RegexMatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.UUID;
import java.util.regex.Pattern;

public final class IotaInlineMatcher {
    private static final Pattern ENTITY_PATTERN = Pattern.compile("(?<token>[^,\\[\\]]+?)<hextricks:entity:(?<entity>[^>]+)>");
    private static final Pattern CONTINUATION_PATTERN = Pattern.compile("<hextricks:continuation:(?<continuation>[^>]+)>");
    private static final ResourceLocation ENTITY_ID = HexTricks.id("entity_iota_display");
    private static final ResourceLocation CONTINUATION_ID = HexTricks.id("continuation_iota_display");

    private IotaInlineMatcher() {
    }

    public static void register() {
        InlineClientAPI.INSTANCE.addMatcher(new RegexMatcher.Simple(
                ENTITY_PATTERN,
                ENTITY_ID,
                match -> matchEntityInline(match.group("token"), match.group("entity")),
                MatcherInfo.fromId(ENTITY_ID)
        ));
        InlineClientAPI.INSTANCE.addMatcher(new RegexMatcher.Simple(
                CONTINUATION_PATTERN,
                CONTINUATION_ID,
                match -> matchContinuationInline(match.group("continuation")),
                MatcherInfo.fromId(CONTINUATION_ID)
        ));
    }

    private static InlineMatch matchEntityInline(String token, String rawEntityId) {
        try {
            String decodedEntityId = desanitizeMetadataValue(rawEntityId);
            UUID entityId = UUID.fromString(decodedEntityId);
            Component component = buildEntityDisplayComponent(token, entityId);
            return new InlineMatch.TextMatch(component);
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to reconstruct EntityIota display from metadata", t);
            return null;
        }
    }

    private static InlineMatch matchContinuationInline(String rawContinuationPayload) {
        try {
            desanitizeMetadataValue(rawContinuationPayload);
            return new InlineMatch.TextMatch(Component.literal(""));
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to reconstruct ContinuationIota display from metadata", t);
            return null;
        }
    }

    private static Component buildEntityDisplayComponent(String token, UUID entityId) {
        String unknownDisplay = Component.translatable("hexcasting.spelldata.entity.whoknows").getString();
        String trimmedToken = token == null ? "" : token.trim();
        if (unknownDisplay.equals(trimmedToken)) {
            return Component.translatable("hexcasting.spelldata.entity.whoknows");
        }

        Entity entity = resolveClientEntity(entityId);
        if (entity != null) {
            Component inlineEntity;
            if (entity instanceof Player player) {
                Component head = new PlayerHeadData(new ResolvableProfile(player.getGameProfile())).asText(false);
                inlineEntity = head.copy().withStyle(InlineAPI.INSTANCE.withSizeModifier(head.getStyle(), 1.5));
            } else {
                inlineEntity = EntityInlineData.fromType(entity.getType()).asText(false);
            }
            return entity.getName().copy()
                    .append(Component.literal(": "))
                    .append(inlineEntity)
                    .withStyle(ChatFormatting.AQUA);
        }

        return Component.literal(trimmedToken).withStyle(ChatFormatting.AQUA);
    }

    private static Entity resolveClientEntity(UUID entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entityId.equals(entity.getUUID())) {
                return entity;
            }
        }
        return null;
    }

    private static String desanitizeMetadataValue(String value) {
        return value
                .replace("\\u002C", ",")
                .replace("\\u005B", "[")
                .replace("\\u005D", "]")
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
    }
}
