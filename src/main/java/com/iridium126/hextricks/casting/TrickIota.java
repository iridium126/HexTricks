package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;

import java.util.Objects;

public class TrickIota extends Iota {
    private final String spellData;

    public TrickIota(String spellData) {
        super(() -> HexTricksIotaTypes.TRICK.get());
        this.spellData = spellData;
    }

    public String getSpellData() {
        return spellData;
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    protected boolean toleratesOther(Iota that) {
        return typesMatch(this, that)
                && that instanceof TrickIota other
                && spellData.equals(other.spellData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spellData);
    }

    @Override
    public Component display() {
        if (spellData == null || spellData.isBlank()) {
            return Component.translatable("hextricks.iota.trick")
                    .withStyle(ChatFormatting.YELLOW);
        }
        return Component.translatable("hextricks.iota.trick")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("[spell." + spellData + "]"));
    }

    public static final IotaType<TrickIota> TYPE = new IotaType<>() {
        public static final MapCodec<TrickIota> CODEC = Codec.STRING
                .xmap(TrickIota::new, TrickIota::getSpellData)
                .fieldOf("spell_data");

        public static final StreamCodec<RegistryFriendlyByteBuf, TrickIota> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.map(TrickIota::new, TrickIota::getSpellData).mapStream(buf -> buf);

        @Override
        public MapCodec<TrickIota> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TrickIota> streamCodec() {
            return STREAM_CODEC;
        }

        @Override
        public int color() {
            return 0xff_33ccff;
        }
    };
}
