package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.*;
import com.iridium126.hextricks.HexTricks;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import com.iridium126.hextricks.util.ListPatternIotaParser;
import com.iridium126.hextricks.util.ListPatternIotaValidator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3d;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TricksterBridge {
    private static final int MAX_SYNC_EXECUTION_STEPS = 4096;
    private static final String LIST_ENTITY_METADATA_PREFIX = "<hextricks:entity:";
    private static final String LIST_CONTINUATION_METADATA_PREFIX = "<hextricks:continuation:";
    private static final String LIST_METADATA_SUFFIX = ">";
    private static volatile boolean readInitialized = false;
    private static volatile boolean readAvailable = false;
    private static volatile boolean executeInitialized = false;
    private static volatile boolean executeAvailable = false;
    private static volatile boolean registerInitialized = false;
    private static volatile boolean registerAvailable = false;

    private static Method fragmentGetMethod;
    private static Method fragmentToBase64Method;
    private static Method fragmentFromBase64Method;
    private static Class<?> fragmentClass;
    private static Class<?> spellPartClass;
    private static Constructor<?> spellPartCtor;
    private static Class<?> numberFragmentClass;
    private static Constructor<?> numberFragmentCtor;
    private static Method numberValueMethod;
    private static Class<?> booleanFragmentClass;
    private static Method booleanFragmentOfMethod;
    private static Class<?> vectorFragmentClass;
    private static Constructor<?> vectorFragmentCtor;
    private static Method vectorXMethod;
    private static Method vectorYMethod;
    private static Method vectorZMethod;
    private static Class<?> listFragmentClass;
    private static Constructor<?> listFragmentCtor;
    private static Method listFragmentsMethod;
    private static Class<?> entityFragmentClass;
    private static Constructor<?> entityFragmentCtor;
    private static Method entityUuidMethod;
    private static Method entityNameMethod;
    private static Method stringValueMethod;
    private static Class<?> stringFragmentClass;
    private static Constructor<?> stringFragmentCtor;
    static Class<?> voidFragmentClass;
    static Object voidFragmentInstance;
    private static Class<?> playerSpellSourceClass;
    private static Constructor<?> defaultSpellExecutorCtor;
    private static Method spellExecutorRunMethod;

    static Method tricksRegisterMethod;
    static Method patternOfMethod;
    static Constructor<?> loadArgumentTrickCtor;
    static Class<?> signatureClass;

    private TricksterBridge() {
    }

    static String readSpellDataFromStack(ItemStack stack) {
        try {
            if (!ensureReadInit()) {
                return null;
            }

            Object fragmentOptional = fragmentGetMethod.invoke(null, stack);
            if (!(fragmentOptional instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }

            Object fragment = optional.get();
            Object base64 = fragmentToBase64Method.invoke(fragment);
            if (!(base64 instanceof String s) || s.isBlank()) {
                return null;
            }
            return s;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Iota tryExecuteBySpellData(ServerPlayer player, String spellData, List<Iota> arguments) {
        try {
            if (!ensureExecuteInit() || spellData == null || spellData.isBlank()) {
                return null;
            }

            Object decoded = fragmentFromBase64Method.invoke(null, spellData);
            if (decoded == null) {
                return null;
            }

            Object spellPart = spellPartClass.isInstance(decoded) ? decoded : spellPartCtor.newInstance(decoded);
            Object source = newPlayerSpellSource(player);
            if (source == null) {
                return null;
            }
            List<Object> tricksterArgs = new ArrayList<>(arguments.size());
            for (Iota arg : arguments) {
                Object fragmentArg = iotaToFragment(arg, false);
                if (fragmentArg == null) {
                    HexTricks.LOGGER.warn("Unsupported iota argument type for Trickster execution: {}", arg.getClass().getName());
                    return null;
                }
                tricksterArgs.add(fragmentArg);
            }

            Object executor = defaultSpellExecutorCtor.newInstance(spellPart, tricksterArgs);
            for (int step = 0; step < MAX_SYNC_EXECUTION_STEPS; step++) {
                Object runResult = spellExecutorRunMethod.invoke(executor, source);
                if (!(runResult instanceof Optional<?> optional)) {
                    return null;
                }
                if (optional.isPresent()) {
                    Iota converted = fragmentToIota(optional.get());
                    return converted != null ? converted : new NullIota();
                }
            }

            return null;
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to execute Trickster spell fragment bridge", t);
            return null;
        }
    }

    private static synchronized boolean ensureReadInit() {
        if (readInitialized) {
            return readAvailable;
        }
        readInitialized = true;
        try {
            Class<?> fragmentComponentClass = Class.forName("dev.enjarai.trickster.item.component.FragmentComponent");
            Class<?> itemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            fragmentGetMethod = fragmentComponentClass.getMethod("getFragment", itemStackClass);

            fragmentClass = Class.forName("dev.enjarai.trickster.spell.Fragment");
            fragmentToBase64Method = fragmentClass.getMethod("toBase64");
            fragmentFromBase64Method = fragmentClass.getMethod("fromBase64", String.class);

            readAvailable = true;
        } catch (Throwable ignored) {
            readAvailable = false;
        }
        return readAvailable;
    }

    static synchronized boolean ensureExecuteInit() {
        if (executeInitialized) {
            return executeAvailable;
        }
        executeInitialized = true;
        try {
            if (!ensureReadInit()) {
                executeAvailable = false;
                return false;
            }

            spellPartClass = Class.forName("dev.enjarai.trickster.spell.SpellPart");
            spellPartCtor = spellPartClass.getConstructor(fragmentClass);

            numberFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.NumberFragment");
            numberFragmentCtor = numberFragmentClass.getConstructor(double.class);
            numberValueMethod = numberFragmentClass.getMethod("number");

            booleanFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.BooleanFragment");
            booleanFragmentOfMethod = booleanFragmentClass.getMethod("of", boolean.class);

            vectorFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.VectorFragment");
            Class<?> vector3dcClass = Class.forName("org.joml.Vector3dc");
            vectorFragmentCtor = vectorFragmentClass.getConstructor(vector3dcClass);
            vectorXMethod = vectorFragmentClass.getMethod("x");
            vectorYMethod = vectorFragmentClass.getMethod("y");
            vectorZMethod = vectorFragmentClass.getMethod("z");

            listFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.ListFragment");
            listFragmentCtor = listFragmentClass.getConstructor(List.class);
            listFragmentsMethod = listFragmentClass.getMethod("fragments");

            entityFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.EntityFragment");
            Class<?> textClass = Class.forName("net.minecraft.network.chat.Component");
            entityFragmentCtor = entityFragmentClass.getConstructor(java.util.UUID.class, textClass);
            entityUuidMethod = entityFragmentClass.getMethod("uuid");
            entityNameMethod = entityFragmentClass.getMethod("name");

            stringFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.StringFragment");
            stringFragmentCtor = stringFragmentClass.getConstructor(String.class);
            stringValueMethod = stringFragmentClass.getMethod("value");

            voidFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.VoidFragment");
            voidFragmentInstance = voidFragmentClass.getField("INSTANCE").get(null);

            playerSpellSourceClass = Class.forName("dev.enjarai.trickster.spell.execution.source.PlayerSpellSource");

            Class<?> defaultSpellExecutorClass = Class.forName("dev.enjarai.trickster.spell.execution.executor.DefaultSpellExecutor");
            defaultSpellExecutorCtor = defaultSpellExecutorClass.getConstructor(spellPartClass, List.class);

            Class<?> spellSourceClass = Class.forName("dev.enjarai.trickster.spell.execution.source.SpellSource");
            spellExecutorRunMethod = defaultSpellExecutorClass.getMethod("run", spellSourceClass);

            executeAvailable = true;
        } catch (Throwable ignored) {
            executeAvailable = false;
        }
        return executeAvailable;
    }

    static synchronized boolean ensureRegisterInit() {
        if (registerInitialized) {
            return registerAvailable;
        }
        registerInitialized = true;
        try {
            Class<?> tricksClass = Class.forName("dev.enjarai.trickster.spell.trick.Tricks");
            Class<?> patternClass = Class.forName("dev.enjarai.trickster.spell.Pattern");
            Class<?> trickClass = Class.forName("dev.enjarai.trickster.spell.trick.Trick");
            Class<?> loadArgumentClass = Class.forName("dev.enjarai.trickster.spell.trick.func.LoadArgumentTrick");
            signatureClass = Class.forName("dev.enjarai.trickster.spell.type.Signature");

            tricksRegisterMethod = tricksClass.getMethod("register", String.class, trickClass);
            patternOfMethod = patternClass.getMethod("of", int[].class);
            loadArgumentTrickCtor = loadArgumentClass.getConstructor(patternClass, int.class);

            registerAvailable = true;
        } catch (Throwable ignored) {
            registerAvailable = false;
        }
        return registerAvailable;
    }

    private static Object newPlayerSpellSource(ServerPlayer player) {
        if (playerSpellSourceClass == null) {
            return null;
        }

        try {
            for (Constructor<?> ctor : playerSpellSourceClass.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(player.getClass())) {
                    return ctor.newInstance(player);
                }
            }
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to construct Trickster PlayerSpellSource", t);
        }

        HexTricks.LOGGER.warn(
                "No compatible PlayerSpellSource constructor found for player class {}",
                player.getClass().getName()
        );
        return null;
    }

    static Object makeTextLiteral(String text) {
        try {
            Class<?> textClass = Class.forName("net.minecraft.network.chat.Component");
            Method literalMethod = textClass.getMethod("literal", String.class);
            return literalMethod.invoke(null, text);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Object stringToFragment(String value) {
        if (stringFragmentCtor == null) {
            return null;
        }

        try {
            return stringFragmentCtor.newInstance(value);
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to build Trickster StringFragment", t);
            return null;
        }
    }

    static Object iotaToFragment(Iota iota, boolean reserveList) throws Throwable {
        if (iota instanceof DoubleIota number) {
            return numberFragmentCtor.newInstance(number.getDouble());
        }
        if (iota instanceof BooleanIota bool) {
            return booleanFragmentOfMethod.invoke(null, bool.getBool());
        }
        if (iota instanceof Vec3Iota vec) {
            Vec3 v = vec.getVec3();
            return vectorFragmentCtor.newInstance(new Vector3d(v.x, v.y, v.z));
        }
        if (iota instanceof EntityIota entityIota) {
            String displayName;
            if (entityIota.getEntityName() != null) {
                String name = entityIota.getEntityName().getString();
                int colonIndex = name.lastIndexOf(":");
                displayName = colonIndex > 0 ? name.substring(0, colonIndex) : name;
            } else {
                displayName = Component.translatable("hexcasting.spelldata.entity.whoknows").getString();
            }
            Object text = makeTextLiteral(displayName);
            if (text == null) {
                return null;
            }
            return entityFragmentCtor.newInstance(entityIota.getEntityId(), text);
        }
        if (iota instanceof ListIota list) {
            if (reserveList) {
                String display = augmentReservedListDisplay(list, list.display().getString());
                Object stringFragment = TricksterBridge.stringToFragment(display);
                return stringFragment != null ? stringFragment : TricksterBridge.voidFragmentInstance;
            } else {
                List<Object> children = new ArrayList<>();
                Iterable<Iota> entries = list.subIotas();
                if (entries != null) {
                    for (Iota entry : entries) {
                        Object converted = iotaToFragment(entry, false);
                        if (converted == null) {
                            return null;
                        }
                        children.add(converted);
                    }
                }
                return listFragmentCtor.newInstance(children);
            }
        }
        if (iota instanceof NullIota || iota instanceof GarbageIota) {
            return voidFragmentInstance;
        }
        if (iota instanceof TrickIota trick) {
            return fragmentFromBase64Method.invoke(null, trick.getSpellData());
        }
        if (iota instanceof PatternIota pattern) {
            String display = pattern.display().getString();
            Object stringFragment = TricksterBridge.stringToFragment(display);
            return stringFragment != null ? stringFragment : TricksterBridge.voidFragmentInstance;
        }

        HexTricks.LOGGER.warn("Unsupported iota type for Trickster conversion: {}", iota.getClass().getName());
        return null;
    }

    private static String augmentReservedListDisplay(ListIota list, String display) {
        if (display == null || display.isEmpty()) {
            return display;
        }

        List<DisplayMetadataInsertion> insertions = new ArrayList<>();
        collectReservedListMetadata(list, insertions);
        if (insertions.isEmpty()) {
            return display;
        }

        StringBuilder builder = new StringBuilder(display);
        int searchFrom = 0;
        int offset = 0;
        for (DisplayMetadataInsertion insertion : insertions) {
            String token = insertion.displayToken();
            if (token == null || token.isEmpty()) {
                continue;
            }

            int tokenIndex = display.indexOf(token, searchFrom);
            if (tokenIndex < 0) {
                continue;
            }

            int insertIndex = tokenIndex + token.length() + offset;
            builder.insert(insertIndex, insertion.suffix());
            searchFrom = tokenIndex + token.length();
            offset += insertion.suffix().length();
        }

        return builder.toString();
    }

    private static void collectReservedListMetadata(Iota iota, List<DisplayMetadataInsertion> insertions) {
        if (iota instanceof EntityIota entityIota) {
            String entityId = String.valueOf(entityIota.getEntityId());
            insertions.add(new DisplayMetadataInsertion(
                    entityIota.display().getString(),
                    LIST_ENTITY_METADATA_PREFIX + sanitizeMetadataValue(entityId) + LIST_METADATA_SUFFIX
            ));
        }

        if (iota instanceof ContinuationIota continuationIota) {
            String continuationPayload = serializeContinuationMetadata(continuationIota.getContinuation());
            insertions.add(new DisplayMetadataInsertion(
                    iota.display().getString(),
                    LIST_CONTINUATION_METADATA_PREFIX
                            + sanitizeMetadataValue(continuationPayload)
                            + LIST_METADATA_SUFFIX
            ));
        }

        if (iota instanceof ListIota listIota) {
            Iterable<Iota> entries = listIota.subIotas();
            if (entries != null) {
                for (Iota entry : entries) {
                    collectReservedListMetadata(entry, insertions);
                }
            }
        }
    }

    private static String serializeContinuationMetadata(SpellContinuation continuation) {
        try {
            Object encoded = SpellContinuation.getCODEC().encodeStart(JsonOps.INSTANCE, continuation).result().orElse(null);
            if (encoded != null) {
                return encoded.toString();
            }
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to encode continuation metadata", t);
        }
        return String.valueOf(continuation);
    }

    private static String sanitizeMetadataValue(String value) {
        if (value == null) {
            return "null";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace(",", "\\u002C")
                .replace("[", "\\u005B")
                .replace("]", "\\u005D")
                .replace("<", "\\u003C")
                .replace(">", "\\u003E");
    }

    private record DisplayMetadataInsertion(String displayToken, String suffix) {
    }

    static Iota fragmentToIota(Object fragment) throws Throwable {
        if (fragment == null || voidFragmentClass.isInstance(fragment)) {
            return new NullIota();
        }
        if (numberFragmentClass.isInstance(fragment)) {
            Object value = numberValueMethod.invoke(fragment);
            if (value instanceof Double d) {
                return new DoubleIota(d);
            }
            return null;
        }
        if (booleanFragmentClass.isInstance(fragment)) {
            Method asBooleanMethod = booleanFragmentClass.getMethod("asBoolean");
            Object value = asBooleanMethod.invoke(fragment);
            if (value instanceof Boolean b) {
                return new BooleanIota(b);
            }
            return null;
        }
        if (vectorFragmentClass.isInstance(fragment)) {
            Object x = vectorXMethod.invoke(fragment);
            Object y = vectorYMethod.invoke(fragment);
            Object z = vectorZMethod.invoke(fragment);
            if (x instanceof Double dx && y instanceof Double dy && z instanceof Double dz) {
                return new Vec3Iota(new Vec3(dx, dy, dz));
            }
            return null;
        }
        if (listFragmentClass.isInstance(fragment)) {
            Object raw = listFragmentsMethod.invoke(fragment);
            if (!(raw instanceof List<?> entries)) {
                return null;
            }
            List<Iota> converted = new ArrayList<>(entries.size());
            for (Object entry : entries) {
                Iota iota = fragmentToIota(entry);
                converted.add(iota != null ? iota : new NullIota());
            }
            return new ListIota(converted);
        }
        if (entityFragmentClass.isInstance(fragment)) {
            Object rawUuid = entityUuidMethod.invoke(fragment);
            if (rawUuid instanceof java.util.UUID uuid) {
                Component entityName = null;
                Object rawName = entityNameMethod.invoke(fragment);
                if (rawName != null) {
                    Object rawString = rawName.getClass().getMethod("getString").invoke(rawName);
                    if (rawString instanceof String s) {
                        entityName = Component.literal(s);
                    }
                }
                return new EntityIota(uuid, entityName);
            }
            return null;
        }
        if (stringFragmentClass.isInstance(fragment)) {
            Object value = stringValueMethod.invoke(fragment);
            if (value instanceof String s && ListPatternIotaValidator.isListOrPatternIotaDisplay(s)) {
                return ListPatternIotaParser.restoreFromDisplay(s).orElse(new NullIota());
            }
            return new NullIota();
        }
        return new NullIota();
    }
}
