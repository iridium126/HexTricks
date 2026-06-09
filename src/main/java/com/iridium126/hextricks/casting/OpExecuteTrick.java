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
        TrickIota trickIota;
        ListIota listIota = null;

        if (args.size() == 1) {
            trickIota = (TrickIota) args.get(0);
        } else {
            trickIota = (TrickIota) args.get(0);
            listIota = (ListIota) args.get(1);
        }

        if (!(env.getCastingEntity() instanceof ServerPlayer player)) {
            return List.of();
        }

        List<Iota> params = new ArrayList<>();
        if (listIota != null) {
            Iterable<Iota> iterable = listIota.subIotas();
            if (iterable != null) {
                for (Iota iota : iterable) {
                    params.add(iota);
                }
            }
        }

        SpellExecutionBridge.SpellExecutionResult execution = SpellExecutionBridge.tryExecuteBySpellData(player, trickIota.getSpellData(), params, env);
        if (execution.status() == SpellExecutionBridge.SpellExecutionStatus.COMPLETED) {
            Iota result = execution.result();
            return result != null ? List.of(result) : List.of();
        }
        if (execution.status() == SpellExecutionBridge.SpellExecutionStatus.HANDED_OFF) {
            return List.of();
        }

        HexTricks.LOGGER.warn("Failed to execute Trickster spell fragment");
        return List.of();
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) throws Mishap {
        List<Iota> stack = new ArrayList<>(image.getStack());
        if (stack.isEmpty()) {
            throw new MishapNotEnoughArgs(1, 0);
        }

        List<Iota> argsToPass = new ArrayList<>();
        Iota top = stack.removeLast();

        if (top instanceof ListIota list) {
            if (stack.isEmpty()) {
                throw new MishapNotEnoughArgs(2, 1);
            }
            Iota next = stack.removeLast();
            if (next instanceof TrickIota trick) {
                argsToPass.add(trick);
                argsToPass.add(list);
            } else {
                throw MishapInvalidIota.of(next, stack.size(), "class.hextricks_trick");
            }
        } else if (top instanceof TrickIota trick) {
            argsToPass.add(trick);
        } else {
            throw MishapInvalidIota.of(top, stack.size(), "class.hextricks_trick");
        }

        stack.addAll(execute(argsToPass, env));

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
