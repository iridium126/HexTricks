package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
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
        Iota input = args.getFirst();
        if (!(input instanceof TrickIota trickIota)) {
            throw MishapInvalidIota.of(input, 0, "class.hextricks_trick");
        }

        if (!(env.getCastingEntity() instanceof ServerPlayer player)) {
            return List.of();
        }

        boolean queued = TricksterBridge.tryQueueById(player, trickIota.getTrickId().toString());
        if (!queued) {
            HexTricks.LOGGER.warn("Failed to execute Trickster trick {}", trickIota.getTrickId());
        }

        return List.of();
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) throws Mishap {
        List<Iota> stack = new ArrayList<>(image.getStack());
        if (stack.isEmpty()) {
            throw new MishapNotEnoughArgs(1, 0);
        }

        Iota arg = stack.removeLast();
        stack.addAll(execute(List.of(arg), env));

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
