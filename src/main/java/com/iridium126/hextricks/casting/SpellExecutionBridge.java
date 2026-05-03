package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.iota.Iota;
import com.iridium126.hextricks.HexTricks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SpellExecutionBridge {
    private SpellExecutionBridge() {}

    public enum SpellExecutionStatus {
        COMPLETED,
        HANDED_OFF,
        FAILED
    }

    public record SpellExecutionResult(SpellExecutionStatus status, Iota result) {
        static SpellExecutionResult completed(Iota result) {
            return new SpellExecutionResult(SpellExecutionStatus.COMPLETED, result);
        }
        static SpellExecutionResult handedOff() {
            return new SpellExecutionResult(SpellExecutionStatus.HANDED_OFF, null);
        }
        static SpellExecutionResult failed() {
            return new SpellExecutionResult(SpellExecutionStatus.FAILED, null);
        }
    }

    static String readSpellDataFromStack(ItemStack stack) {
        try {
            if (!TricksterReflection.ensureReadInit()) return null;
            Object fragmentOptional = TricksterReflection.fragmentGetMethod.invoke(null, stack);
            if (!(fragmentOptional instanceof Optional<?> optional) || optional.isEmpty()) return null;
            Object fragment = optional.get();
            Object base64 = TricksterReflection.fragmentToBase64Method.invoke(fragment);
            return (base64 instanceof String s && !s.isBlank()) ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static SpellExecutionResult tryExecuteBySpellData(ServerPlayer player, String spellData, List<Iota> arguments) {
        try {
            if (!TricksterReflection.ensureExecuteInit() || spellData == null || spellData.isBlank()) return SpellExecutionResult.failed();
            Object decoded = TricksterReflection.fragmentFromBase64Method.invoke(null, spellData);
            if (decoded == null) return SpellExecutionResult.failed();

            Object spellPart = TricksterReflection.spellPartClass.isInstance(decoded) ? decoded : TricksterReflection.spellPartCtor.newInstance(decoded);
            Object source = newPlayerSpellSource(player);
            if (source == null) return SpellExecutionResult.failed();

            List<Object> tricksterArgs = new ArrayList<>(arguments.size());
            for (Iota arg : arguments) {
                Object fragmentArg = FragmentConverter.iotaToFragment(arg, false);
                if (fragmentArg == null) {
                    HexTricks.LOGGER.warn("Unsupported iota argument type for Trickster execution: {}", arg.getClass().getName());
                    return SpellExecutionResult.failed();
                }
                tricksterArgs.add(fragmentArg);
            }

            Object executor = TricksterReflection.defaultSpellExecutorCtor.newInstance(spellPart, tricksterArgs);
            Object runResult = TricksterReflection.spellExecutorRunMethod.invoke(executor, source);
            if (!(runResult instanceof Optional<?> optional)) return SpellExecutionResult.failed();

            if (optional.isPresent()) {
                Iota converted = FragmentConverter.fragmentToIota(optional.get());
                return SpellExecutionResult.completed(converted);
            }

            if (queueExecutorForContinuation(source, executor)) return SpellExecutionResult.handedOff();

            HexTricks.LOGGER.warn("Trickster spell requires continuation but failed to queue");
            return SpellExecutionResult.failed();
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to execute Trickster spell fragment bridge", t);
            return SpellExecutionResult.failed();
        }
    }

    private static Object newPlayerSpellSource(ServerPlayer player) {
        try {
            java.lang.reflect.Constructor<?> ctor = TricksterReflection.resolvePlayerSpellSourceCtor(player.getClass());
            return ctor != null ? ctor.newInstance(player) : null;
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to construct Trickster PlayerSpellSource", t);
            return null;
        }
    }

    private static boolean queueExecutorForContinuation(Object source, Object executor) {
        if (TricksterReflection.spellSourceGetExecutionManagerMethod == null || TricksterReflection.spellExecutionManagerQueueMethod == null) return false;
        try {
            Object managerOptional = TricksterReflection.spellSourceGetExecutionManagerMethod.invoke(source);
            if (!(managerOptional instanceof Optional<?> optional) || optional.isEmpty()) return false;
            Object queueResult = TricksterReflection.spellExecutionManagerQueueMethod.invoke(optional.get(), executor);
            return queueResult instanceof Optional<?> queueOptional && queueOptional.isPresent();
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to queue Trickster spell executor continuation", t);
            return false;
        }
    }
}
