package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import at.petrak.hexcasting.api.casting.mishaps.MishapNotEnoughArgs;
import com.iridium126.hextricks.HexTricks;
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public enum OpExecuteTrick implements Action {
    INSTANCE;

    private List<Iota> execute(List<Iota> args, CastingEnvironment env) throws Mishap {
        Iota first = args.get(0);
        Iota second = args.get(1);

        TrickIota trickIota;
        ListIota listIota;

        if (first instanceof TrickIota trick && second instanceof ListIota list) {
            trickIota = trick;
            listIota = list;
        } else if (first instanceof ListIota list && second instanceof TrickIota trick) {
            trickIota = trick;
            listIota = list;
        } else if (!(first instanceof TrickIota) && !(first instanceof ListIota)) {
            throw MishapInvalidIota.of(first, 0, "class.hextricks_trick");
        } else if (!(second instanceof ListIota) && !(second instanceof TrickIota)) {
            throw MishapInvalidIota.of(second, 1, "class.hexcasting_list");
        } else if (first instanceof TrickIota) {
            throw MishapInvalidIota.of(second, 1, "class.hexcasting_list");
        } else {
            throw MishapInvalidIota.of(second, 1, "class.hextricks_trick");
        }

        if (!(env.getCastingEntity() instanceof ServerPlayer player)) {
            return List.of();
        }

        List<Iota> params = new ArrayList<>();
        Iterable<Iota> iterable = listIota.subIotas();
        if (iterable != null) {
            for (Iota iota : iterable) {
                params.add(iota);
            }
        }

        Iota result = TricksterBridge.tryExecuteBySpellData(player, trickIota.getSpellData(), params);
        if (result == null) {
            HexTricks.LOGGER.warn("Failed to execute Trickster spell fragment");
            return List.of();
        }

        return List.of(result);
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) throws Mishap {
        List<Iota> stack = new ArrayList<>(image.getStack());
        if (stack.size() < 2) {
            throw new MishapNotEnoughArgs(2, stack.size());
        }

        Iota arg0 = stack.removeLast();
        Iota arg1 = stack.removeLast();
        stack.addAll(execute(List.of(arg0, arg1), env));

        CastingImage nextImage = image.copy(
                stack,
                image.getParenCount(),
                image.getParenthesized(),
                image.getEscapeNext(),
                image.getOpsConsumed() + 1,
                image.getUserData()
        );
        return new OperationResult(nextImage, List.of(), continuation, HexEvalSounds.NORMAL_EXECUTE);
    }
}
