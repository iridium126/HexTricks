package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import com.iridium126.hextricks.HexTricks;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public class TrickIota extends Iota {
    private final ResourceLocation trickId;

    public TrickIota(ResourceLocation trickId) {
        super(() -> HexTricksIotaTypes.TRICK.get());
        this.trickId = trickId;
    }

    public ResourceLocation getTrickId() {
        return trickId;
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    protected boolean toleratesOther(Iota that) {
        return typesMatch(this, that)
                && that instanceof TrickIota other
                && trickId.equals(other.trickId);
    }

    @Override
    public int hashCode() {
        return trickId.hashCode();
    }

    @Override
    public Component display() {
        return Component.translatable("hextricks.iota.trick", trickId.toString())
                .withStyle(ChatFormatting.AQUA);
    }

    public static final IotaType<TrickIota> TYPE = new IotaType<>() {
        public static final MapCodec<TrickIota> CODEC = ResourceLocation.CODEC
                .xmap(TrickIota::new, TrickIota::getTrickId)
                .fieldOf("trick");

        public static final StreamCodec<RegistryFriendlyByteBuf, TrickIota> STREAM_CODEC =
                ResourceLocation.STREAM_CODEC.map(TrickIota::new, TrickIota::getTrickId).mapStream(buf -> buf);

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
