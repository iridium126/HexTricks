package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadOffhandItem;
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds;

import java.util.ArrayList;
import java.util.List;

public enum OpReadTrickFromItem implements Action {
    INSTANCE;

    private List<Iota> execute(CastingEnvironment env) throws Mishap {
        CastingEnvironment.HeldItemInfo held = env.getHeldItemToOperateOn(
                stack -> SpellExecutionBridge.readSpellDataFromStack(stack) != null
        );
        if (held == null) {
            CastingEnvironment.HeldItemInfo fallback = env.getHeldItemToOperateOn(stack -> !stack.isEmpty());
            throw MishapBadOffhandItem.of(fallback != null ? fallback.stack() : null, "trick.read");
        }

        String spellData = SpellExecutionBridge.readSpellDataFromStack(held.stack());
        if (spellData == null) {
            throw MishapBadOffhandItem.of(held.stack(), "trick.read");
        }

        return List.of(new TrickIota(spellData));
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation continuation) throws Mishap {
        List<Iota> stack = new ArrayList<>(image.getStack());
        stack.addAll(execute(env));

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
