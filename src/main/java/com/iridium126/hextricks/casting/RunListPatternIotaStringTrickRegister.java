package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import com.iridium126.hextricks.HexTricks;
import com.iridium126.hextricks.util.ListPatternIotaParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RunListPatternIotaStringTrickRegister {
    private static volatile boolean runListPatternIotaStringTrickRegistered = false;

    private RunListPatternIotaStringTrickRegister() {
    }

    public static void register() {
        if (runListPatternIotaStringTrickRegistered) {
            return;
        }

        try {
            if (!TricksterBridge.ensureRegisterInit()) {
                HexTricks.LOGGER.warn("Trickster list/pattern string run trick registration is unavailable: register init failed");
                return;
            }

            Object pattern = TricksterBridge.patternOfMethod.invoke(null, (Object) new int[]{3, 6, 7, 8, 5, 4, 3, 0, 1, 4, 7, 3});
            Object trick = TricksterBridge.loadArgumentTrickCtor.newInstance(pattern, 0);

            Method getSignaturesMethod = trick.getClass().getMethod("getSignatures");
            Object rawSignatures = getSignaturesMethod.invoke(trick);
            if (!(rawSignatures instanceof List<?> signaturesRaw)) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<Object> signatures = (List<Object>) signaturesRaw;

            signatures.clear();
            signatures.add(createSignatureProxy());
            TricksterBridge.tricksRegisterMethod.invoke(null, "run_list_pattern_iota_string", trick);
            runListPatternIotaStringTrickRegistered = true;
            HexTricks.LOGGER.info("Registered Trickster trick: run_list_pattern_iota_string");
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to register Trickster list/pattern string run trick", t);
        }
    }

    private static Object createSignatureProxy() {
        java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("match".equals(name)) {
                List<?> fragments = args != null && args.length > 0 && args[0] instanceof List<?> list ? list : List.of();
                return hasRunnableInput(fragments);
            }
            if ("asText".equals(name)) {
                return TricksterBridge.makeTextLiteral("string/list, ... -> any");
            }
            if ("run".equals(name)) {
                try {
                    if (!TricksterBridge.ensureExecuteInit()) {
                        HexTricks.LOGGER.warn("Trickster list/pattern string run trick execute bridge unavailable");
                        return TricksterBridge.voidFragmentInstance;
                    }

                    Object spellContext = args != null && args.length > 1 ? args[1] : null;
                    List<?> fragments = args != null && args.length > 2 && args[2] instanceof List<?> list ? list : List.of();
                    Iota runnableIota = extractRunnableIota(fragments);
                    if (runnableIota == null) {
                        return TricksterBridge.voidFragmentInstance;
                    }
                    List<Iota> initialStack = extractInitialStack(fragments);
                    if (initialStack == null) {
                        return TricksterBridge.voidFragmentInstance;
                    }

                    Iota result = runRestoredIota(spellContext, runnableIota, initialStack);
                    if (result == null) {
                        return TricksterBridge.voidFragmentInstance;
                    }

                    Object fragment = TricksterBridge.iotaToFragment(result, true);
                    return fragment != null ? fragment : TricksterBridge.voidFragmentInstance;
                } catch (Throwable t) {
                    HexTricks.LOGGER.warn("Trickster list/pattern string run trick failed", t);
                    return TricksterBridge.voidFragmentInstance;
                }
            }
            return method.getDefaultValue();
        };
        return Proxy.newProxyInstance(
                TricksterBridge.signatureClass.getClassLoader(),
                new Class<?>[]{TricksterBridge.signatureClass},
                handler
        );
    }

    private static boolean hasRunnableInput(List<?> fragments) {
        if (fragments.isEmpty()) {
            return false;
        }

        return extractRunnableIota(fragments) != null && extractInitialStack(fragments) != null;
    }

    private static Iota extractRunnableIota(List<?> fragments) {
        if (fragments.isEmpty()) {
            return null;
        }

        Object fragment = fragments.getFirst();
        Iota stringIota = extractStringIota(fragment);
        if (stringIota != null) {
            return stringIota;
        }

        return extractListIota(fragment);
    }

    private static List<Iota> extractInitialStack(List<?> fragments) {
        if (fragments.size() <= 1) {
            return List.of();
        }

        List<Iota> initialStack = new ArrayList<>(fragments.size() - 1);
        for (int index = 1; index < fragments.size(); index++) {
            Iota iota = extractArgumentIota(fragments.get(index));
            if (iota == null) {
                return null;
            }
            initialStack.add(iota);
        }
        return initialStack;
    }

    private static Iota extractStringIota(Object fragment) {
        String displayString = extractStringInput(fragment);
        if (displayString == null) {
            return null;
        }

        Optional<Iota> restored = ListPatternIotaParser.restoreFromDisplay(displayString);
        return restored.orElse(null);
    }

    private static Iota extractListIota(Object fragment) {
        try {
            if (!TricksterBridge.ensureExecuteInit()) {
                return null;
            }
            Iota iota = TricksterBridge.fragmentToIota(fragment);
            return iota instanceof ListIota ? iota : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Iota extractArgumentIota(Object fragment) {
        try {
            if (!TricksterBridge.ensureExecuteInit()) {
                return null;
            }
            Iota iota = TricksterBridge.fragmentToIota(fragment);
            return iota != null ? iota : new NullIota();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String extractStringInput(Object fragment) {
        String value = tryReadFragmentValue(fragment);
        if (value != null) {
            return value;
        }

        value = tryReadFragmentText(fragment);
        if (value == null) {
            return null;
        }

        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private static String tryReadFragmentValue(Object fragment) {
        if (fragment == null) {
            return null;
        }

        try {
            Method valueMethod = fragment.getClass().getMethod("value");
            Object value = valueMethod.invoke(fragment);
            return value instanceof String string ? string : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String tryReadFragmentText(Object fragment) {
        if (fragment == null) {
            return null;
        }

        try {
            Method asTextMethod = fragment.getClass().getMethod("asText");
            Object text = asTextMethod.invoke(fragment);
            if (text == null) {
                return null;
            }

            Method getStringMethod = text.getClass().getMethod("getString");
            Object rawString = getStringMethod.invoke(text);
            return rawString instanceof String string ? string : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Iota runRestoredIota(Object spellContext, Iota restoredIota, List<Iota> initialStack) {
        ServerPlayer player = resolveCasterPlayer(spellContext);
        if (player == null) {
            return null;
        }

        try {
            CastingImage image = new CastingImage(
                    new ArrayList<>(initialStack),
                    0,
                    List.<CastingImage.ParenthesizedIota>of(),
                    false,
                    0,
                    new CompoundTag()
            );
            CastingVM vm = new CastingVM(image, new StaffCastEnv(player, InteractionHand.MAIN_HAND));
            ExecutionClientView result;

            if (restoredIota instanceof ListIota listIota) {
                List<Iota> instructions = new ArrayList<>();
                Iterable<Iota> iterable = listIota.subIotas();
                if (iterable != null) {
                    for (Iota iota : iterable) {
                        instructions.add(iota);
                    }
                }
                result = vm.queueExecuteAndWrapIotas(instructions, player.serverLevel());
            } else {
                result = vm.queueExecuteAndWrapIota(restoredIota, player.serverLevel());
            }

            if (!result.getResolutionType().getSuccess()) {
                return null;
            }

            List<Iota> stack = result.getStackDescs();
            return stack.isEmpty() ? new NullIota() : stack.getLast();
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to run restored Hex iota from Trickster string", t);
            return null;
        }
    }

    private static ServerPlayer resolveCasterPlayer(Object spellContext) {
        try {
            if (spellContext == null) {
                return null;
            }

            Method sourceMethod = spellContext.getClass().getMethod("source");
            Object source = sourceMethod.invoke(spellContext);
            if (source == null) {
                return null;
            }

            Method getPlayerMethod = source.getClass().getMethod("getPlayer");
            Object rawPlayerOptional = getPlayerMethod.invoke(source);
            if (!(rawPlayerOptional instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }

            Object rawPlayer = optional.get();
            return rawPlayer instanceof ServerPlayer player ? player : null;
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to resolve Trickster spell caster player", t);
            return null;
        }
    }
}
