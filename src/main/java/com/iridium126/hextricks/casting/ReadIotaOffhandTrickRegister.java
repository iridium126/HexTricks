package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.addldata.ADIotaHolder;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.iridium126.hextricks.HexTricks;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class ReadIotaOffhandTrickRegister {
    private static volatile boolean readIotaTrickRegistered = false;

    private ReadIotaOffhandTrickRegister() {
    }

    public static void register() {
        if (readIotaTrickRegistered) {
            return;
        }
        try {
            if (!TricksterReflection.ensureRegisterInit()) {
                HexTricks.LOGGER.warn("Trickster offhand iota read trick registration is unavailable: register init failed");
                return;
            }

            Object pattern = TricksterReflection.patternOfMethod.invoke(null, (Object) new int[]{6, 7, 4, 1, 0, 3, 4, 5, 2, 1});
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
            TricksterReflection.tricksRegisterMethod.invoke(null, "read_iota_offhand", trick);
            readIotaTrickRegistered = true;
            HexTricks.LOGGER.info("Registered Trickster trick: read_iota_offhand");
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to register Trickster offhand iota read trick", t);
        }
    }

    private static Object createSignatureProxy() {
        java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("match".equals(name)) {
                return true;
            }
            if ("asText".equals(name)) {
                return FragmentConverter.makeTextLiteral("-> any");
            }
            if ("run".equals(name)) {
                try {
                    if (!TricksterReflection.ensureExecuteInit()) {
                        HexTricks.LOGGER.warn("Trickster offhand iota read trick execute bridge unavailable");
                        return TricksterReflection.voidFragmentInstance;
                    }
                    Object spellContext = args != null && args.length > 1 ? args[1] : null;
                    Iota iota = readIotaFromOffhand(spellContext);
                    if (iota == null) {
                        return TricksterReflection.voidFragmentInstance;
                    }

                    Object fragment = FragmentConverter.iotaToFragment(iota, true);
                    return fragment != null ? fragment : TricksterReflection.voidFragmentInstance;
                } catch (Throwable t) {
                    HexTricks.LOGGER.warn("Trickster offhand iota read trick failed", t);
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

    private static Iota readIotaFromOffhand(Object spellContext) {
        ItemStack stack = findStackFromOtherHand(spellContext,
                candidate -> {
                    ADIotaHolder holder = IXplatAbstractions.INSTANCE.findDataHolder(candidate);
                    return holder != null && (holder.readIota() != null || holder.emptyIota() != null);
                },
                candidate -> IXplatAbstractions.INSTANCE.findDataHolder(candidate) != null
        );
        if (stack == null) {
            return null;
        }

        ADIotaHolder holder = IXplatAbstractions.INSTANCE.findDataHolder(stack);
        if (holder == null) {
            return null;
        }

        Iota read = holder.readIota();
        return read != null ? read : holder.emptyIota();
    }

    private static ItemStack findStackFromOtherHand(Object spellContext, Predicate<ItemStack> wanted, Predicate<ItemStack> fallback) {
        try {
            if (spellContext == null) {
                return null;
            }

            Method sourceMethod = spellContext.getClass().getMethod("source");
            Object source = sourceMethod.invoke(spellContext);
            if (source == null) {
                return null;
            }

            Method getOtherHandStackMethod = source.getClass().getMethod("getOtherHandStack", Predicate.class);
            ItemStack matched = unwrapStack(getOtherHandStackMethod.invoke(source, wanted));
            if (matched != null) {
                return matched;
            }
            return unwrapStack(getOtherHandStackMethod.invoke(source, fallback));
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to resolve Trickster offhand stack for iota read", t);
            return null;
        }
    }

    private static ItemStack unwrapStack(Object maybeOptionalStack) {
        if (!(maybeOptionalStack instanceof Optional<?> optional) || optional.isEmpty()) {
            return null;
        }
        Object value = optional.get();
        return value instanceof ItemStack stack ? stack : null;
    }
}
