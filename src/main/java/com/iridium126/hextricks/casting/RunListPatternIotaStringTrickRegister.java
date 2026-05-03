package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
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
            if (!TricksterReflection.ensureRegisterInit()) {
                HexTricks.LOGGER.warn("Trickster list/pattern string run trick registration is unavailable: register init failed");
                return;
            }

            Object pattern = TricksterReflection.patternOfMethod.invoke(null, (Object) new int[]{3, 6, 7, 8, 5, 4, 3, 0, 1, 4, 7, 3});
            Object trick = TricksterReflection.loadArgumentTrickCtor.newInstance(pattern, 0);

            Method getSignaturesMethod = trick.getClass().getMethod("getSignatures");
            Object rawSignatures = getSignaturesMethod.invoke(trick);
            if (!(rawSignatures instanceof List<?> signaturesRaw)) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<Object> signatures = (List<Object>) signaturesRaw;

            signatures.clear();
            signatures.add(createSignatureProxy());
            TricksterReflection.tricksRegisterMethod.invoke(null, "run_list_pattern_iota_string", trick);
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
                return extractRunnableInput(null, fragments) != null;
            }
            if ("asText".equals(name)) {
                return FragmentConverter.makeTextLiteral("string/list, ... -> any");
            }
            if ("run".equals(name)) {
                try {
                    if (!TricksterReflection.ensureExecuteInit()) {
                        HexTricks.LOGGER.warn("Trickster list/pattern string run trick execute bridge unavailable");
                        return TricksterReflection.voidFragmentInstance;
                    }

                    Object spellContext = args != null && args.length > 1 ? args[1] : null;
                    List<?> fragments = args != null && args.length > 2 && args[2] instanceof List<?> list ? list : List.of();
                    
                    RunnableInput input = extractRunnableInput(spellContext, fragments);
                    if (input == null) return TricksterReflection.voidFragmentInstance;

                    Iota result = runRestoredIota(spellContext, input.runnableIota(), input.initialStack());
                    return result != null ? Optional.ofNullable(FragmentConverter.iotaToFragment(result, false)).orElse(TricksterReflection.voidFragmentInstance) : TricksterReflection.voidFragmentInstance;
                } catch (Throwable t) {
                    HexTricks.LOGGER.warn("Trickster list/pattern string run trick failed", t);
                    return TricksterReflection.voidFragmentInstance;
                }
            }
            return method.getDefaultValue();
        };
        return Proxy.newProxyInstance(
                TricksterReflection.signatureClass.getClassLoader(),
                new Class<?>[]{TricksterReflection.signatureClass},
                handler
        );
    }

    private static RunnableInput extractRunnableInput(Object spellContext, List<?> fragments) {
        List<?> contextArgs = TricksterReflection.getSpellContextArguments(spellContext);
        if (contextArgs != null && !contextArgs.isEmpty()) {
            if (fragments.size() == 1 && contextArgs.size() > 1 && fragmentsFirstMatches(fragments.getFirst(), contextArgs.getFirst())) {
                Iota expanded = extractExpandedPatternListIota(contextArgs);
                if (expanded != null) return new RunnableInput(expanded, List.of());
            }

            Iota contextFirst = extractListIota(contextArgs.getFirst());
            if (contextFirst != null) {
                List<?> entries = TricksterReflection.getListFragments(contextArgs.getFirst());
                if (fragments.isEmpty() || fragmentsFirstMatches(fragments.getFirst(), entries != null && !entries.isEmpty() ? entries.getFirst() : null)) {
                    List<Iota> stack = extractInitialStack(contextArgs);
                    if (stack != null) return new RunnableInput(unwrapRunnableList(contextFirst), stack);
                }
            }
        }

        if (fragments.isEmpty()) return null;

        Object first = fragments.getFirst();
        Iota listIota = extractListIota(first);
        if (listIota != null) {
            List<Iota> stack = extractInitialStack(fragments);
            return stack != null ? new RunnableInput(unwrapRunnableList(listIota), stack) : null;
        }

        if (fragments.size() > 1) {
            Iota expanded = extractExpandedPatternListIota(fragments);
            if (expanded != null) return new RunnableInput(expanded, List.of());
        }

        Iota stringIota = extractStringIota(first);
        if (stringIota != null) {
            List<Iota> stack = extractInitialStack(fragments);
            return stack != null ? new RunnableInput(stringIota, stack) : null;
        }

        return null;
    }

    private static boolean fragmentsFirstMatches(Object f1, Object f2) {
        if (f1 == f2) return true;
        if (f1 == null || f2 == null) return false;
        String v1 = extractStringInput(f1);
        String v2 = extractStringInput(f2);
        return v1 != null && v1.equals(v2);
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
        String display = extractStringInput(fragment);
        return display != null ? ListPatternIotaParser.restoreFromDisplay(display).orElse(null) : null;
    }

    private static Iota extractListIota(Object fragment) {
        try {
            if (!TricksterReflection.ensureExecuteInit()) return null;
            return FragmentConverter.fragmentToIota(fragment) instanceof ListIota list ? list : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Iota unwrapRunnableList(Iota iota) {
        if (iota instanceof ListIota list) {
            List<Iota> entries = listEntries(list);
            if (entries.size() == 1 && entries.getFirst() instanceof ListIota nested) return nested;
        }
        return iota;
    }

    private static Iota extractExpandedPatternListIota(List<?> fragments) {
        List<Iota> iotas = new ArrayList<>();
        for (Object f : fragments) {
            Iota iota = extractStringIota(f);
            if (!(iota instanceof PatternIota)) return null;
            iotas.add(iota);
        }
        return new ListIota(iotas);
    }

    private static List<Iota> listEntries(ListIota list) {
        List<Iota> entries = new ArrayList<>();
        if (list.subIotas() != null) list.subIotas().forEach(entries::add);
        return entries;
    }

    private static Iota extractArgumentIota(Object fragment) {
        try {
            if (!TricksterReflection.ensureExecuteInit()) {
                return null;
            }
            Iota iota = FragmentConverter.fragmentToIota(fragment);
            return iota != null ? iota : new NullIota();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String extractStringInput(Object fragment) {
        String val = (String) TricksterReflection.getFragmentStringValue(fragment);
        if (val != null) return val;
        val = TricksterReflection.getFragmentAsText(fragment);
        if (val != null && val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    private static Iota runRestoredIota(Object spellContext, Iota restoredIota, List<Iota> initialStack) {
        ServerPlayer player = TricksterReflection.resolveCasterPlayer(spellContext);
        if (player == null) return null;

        try {
            CastingImage image = new CastingImage(new ArrayList<>(initialStack), 0, List.of(), false, 0, new CompoundTag());
            CastingVM vm = new CastingVM(image, new StaffCastEnv(player, InteractionHand.MAIN_HAND));
            ExecutionClientView result = (restoredIota instanceof ListIota list) ? 
                    vm.queueExecuteAndWrapIotas(listEntries(list), player.serverLevel()) : 
                    vm.queueExecuteAndWrapIota(restoredIota, player.serverLevel());

            if (!result.getResolutionType().getSuccess()) return null;
            List<Iota> stack = result.getStackDescs();
            return stack.isEmpty() ? new NullIota() : stack.getLast();
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to run restored Hex iota", t);
            return null;
        }
    }

    private record RunnableInput(Iota runnableIota, List<Iota> initialStack) {}
}
