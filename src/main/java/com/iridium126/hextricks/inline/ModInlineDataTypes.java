package com.iridium126.hextricks.inline;

import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.InlineData;
import net.minecraft.resources.ResourceLocation;

public final class ModInlineDataTypes {
    public static final InlineData.InlineDataType<SpellInlineData> SPELL_DATA_TYPE = new SpellInlineDataType();
    private static boolean initialized = false;

    private ModInlineDataTypes() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        InlineAPI.INSTANCE.addDataType(SPELL_DATA_TYPE);
        initialized = true;
    }

    private static final class SpellInlineDataType implements InlineData.InlineDataType<SpellInlineData> {
        private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hextricks", "spell_data");

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public com.mojang.serialization.Codec<SpellInlineData> getCodec() {
            return SpellInlineData.CODEC;
        }
    }
}
