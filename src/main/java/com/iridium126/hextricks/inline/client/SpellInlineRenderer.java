package com.iridium126.hextricks.inline.client;

import com.iridium126.hextricks.inline.SpellInlineData;
import com.samsthenerd.inline.api.client.InlineClientAPI;
import com.samsthenerd.inline.api.client.InlineRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public final class SpellInlineRenderer implements InlineRenderer<SpellInlineData> {
    public static final SpellInlineRenderer INSTANCE = new SpellInlineRenderer();

    private SpellInlineRenderer() {
    }

    public static void register() {
        InlineClientAPI.INSTANCE.addRenderer(INSTANCE);
    }

    @Override
    public ResourceLocation getId() {
        return SpellInlineData.RENDERER_ID;
    }

    @Override
    public int render(SpellInlineData data, GuiGraphics context, int index, Style style, int codepoint, TextRenderingContext trContext) {
        int width = charWidth(data, style, codepoint);
        if (trContext.shadow()) {
            return width;
        }
        data.decodeSpellPart().ifPresent(spellPart -> SpellCircleRenderBridge.renderSpell(
                spellPart,
                context,
                data.scale(),
                0f,
                data.pixelWidth(),
                data.pixelHeight()
        ));
        return width;
    }

    @Override
    public int charWidth(SpellInlineData data, Style style, int codepoint) {
        return data.pixelWidth();
    }
}
