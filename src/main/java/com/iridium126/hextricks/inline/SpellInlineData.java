package com.iridium126.hextricks.inline;

import com.iridium126.hextricks.HexTricks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.samsthenerd.inline.api.InlineData;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record SpellInlineData(String spellBase64, float scale) implements InlineData<SpellInlineData> {
    public static final ResourceLocation RENDERER_ID = HexTricks.id("spell_circle");
    private static final int BASE_SIZE = 110;

    public static final Codec<SpellInlineData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("spell_base64").forGetter(SpellInlineData::spellBase64),
            Codec.FLOAT.optionalFieldOf("scale", 1.0f).forGetter(SpellInlineData::scale)
    ).apply(instance, SpellInlineData::new));

    public static Optional<SpellInlineData> fromBase64(String base64, float scale) {
        if (base64 == null || base64.isBlank()) {
            return Optional.empty();
        }
        return SpellInlineBridge.decodeSpellPart(base64).map(ignored -> new SpellInlineData(base64, scale));
    }

    public Optional<Object> decodeSpellPart() {
        return SpellInlineBridge.decodeSpellPart(spellBase64);
    }

    public int pixelWidth() {
        return Math.max(1, Math.round(BASE_SIZE * scale));
    }

    public int pixelHeight() {
        return Math.max(1, Math.round(BASE_SIZE * scale));
    }

    @Override
    public InlineDataType<SpellInlineData> getType() {
        return ModInlineDataTypes.SPELL_DATA_TYPE;
    }

    @Override
    public ResourceLocation getRendererId() {
        return RENDERER_ID;
    }
}
