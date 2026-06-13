package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.*;
import com.iridium126.hextricks.HexTricks;
import com.iridium126.hextricks.util.ListPatternIotaParser;
import com.iridium126.hextricks.util.ListPatternIotaValidator;
import com.iridium126.hextricks.util.MetadataEscaper;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FragmentConverter {
    private static final String LIST_ENTITY_METADATA_PREFIX = "<hextricks:entity:";
    private static final String LIST_CONTINUATION_METADATA_PREFIX = "<hextricks:continuation:";
    private static final String LIST_METADATA_SUFFIX = ">";

    private FragmentConverter() {}

    public static Object makeTextLiteral(String text) {
        try {
            if (TricksterReflection.componentLiteralMethod != null) {
                return TricksterReflection.componentLiteralMethod.invoke(null, text);
            }
            Class<?> textClass = Class.forName("net.minecraft.network.chat.Component");
            TricksterReflection.componentLiteralMethod = textClass.getMethod("literal", String.class);
            return TricksterReflection.componentLiteralMethod.invoke(null, text);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Object stringToFragment(String value) {
        if (TricksterReflection.stringFragmentCtor == null) return null;
        try {
            return TricksterReflection.stringFragmentCtor.newInstance(value);
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to build Trickster StringFragment", t);
            return null;
        }
    }

    public static Object iotaToFragment(Iota iota, boolean reserveList) throws Throwable {
        if (iota instanceof DoubleIota number) {
            return TricksterReflection.numberFragmentCtor.newInstance(number.getDouble());
        }
        if (iota instanceof BooleanIota bool) {
            return TricksterReflection.booleanFragmentOfMethod.invoke(null, bool.getBool());
        }
        if (iota instanceof Vec3Iota vec) {
            Vec3 v = vec.getVec3();
            return TricksterReflection.vectorFragmentCtor.newInstance(new Vector3d(v.x, v.y, v.z));
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
            if (text == null) return null;
            return TricksterReflection.entityFragmentCtor.newInstance(entityIota.getEntityId(), text);
        }
        if (iota instanceof ListIota list) {
            if (reserveList) {
                String display = augmentReservedListDisplay(list, list.display().getString());
                Object stringFragment = stringToFragment(display);
                return stringFragment != null ? stringFragment : TricksterReflection.voidFragmentInstance;
            } else {
                List<Object> children = new ArrayList<>();
                Iterable<Iota> entries = list.subIotas();
                if (entries != null) {
                    for (Iota entry : entries) {
                        Object converted = iotaToFragment(entry, false);
                        if (converted == null) return null;
                        children.add(converted);
                    }
                }
                return TricksterReflection.listFragmentCtor.newInstance(children);
            }
        }
        if (iota instanceof NullIota || iota instanceof GarbageIota) {
            return TricksterReflection.voidFragmentInstance;
        }
        if (iota instanceof TrickIota trick) {
            return TricksterReflection.fragmentFromBase64Method.invoke(null, trick.getSpellData());
        }
        if (iota instanceof PatternIota pattern) {
            String display = pattern.display().getString();
            Object stringFragment = stringToFragment(display);
            return stringFragment != null ? stringFragment : TricksterReflection.voidFragmentInstance;
        }

        HexTricks.LOGGER.warn("Unsupported iota type for Trickster conversion: {}", iota.getClass().getName());
        return null;
    }

    public static Iota fragmentToIota(Object fragment) throws Throwable {
        if (fragment == null || TricksterReflection.voidFragmentClass.isInstance(fragment)) {
            return null;
        }
        if (TricksterReflection.numberFragmentClass.isInstance(fragment)) {
            Object value = TricksterReflection.numberValueMethod.invoke(fragment);
            return value instanceof Double d ? new DoubleIota(d) : null;
        }
        if (TricksterReflection.booleanFragmentClass.isInstance(fragment)) {
            if (TricksterReflection.booleanFragmentAsBooleanMethod == null) return null;
            Object value = TricksterReflection.booleanFragmentAsBooleanMethod.invoke(fragment);
            return value instanceof Boolean b ? new BooleanIota(b) : null;
        }
        if (TricksterReflection.vectorFragmentClass.isInstance(fragment)) {
            Object x = TricksterReflection.vectorXMethod.invoke(fragment);
            Object y = TricksterReflection.vectorYMethod.invoke(fragment);
            Object z = TricksterReflection.vectorZMethod.invoke(fragment);
            if (x instanceof Double dx && y instanceof Double dy && z instanceof Double dz) {
                return new Vec3Iota(new Vec3(dx, dy, dz));
            }
            return null;
        }
        if (TricksterReflection.listFragmentClass.isInstance(fragment)) {
            Object raw = TricksterReflection.listFragmentsMethod.invoke(fragment);
            if (!(raw instanceof List<?> entries)) return null;
            List<Iota> converted = new ArrayList<>(entries.size());
            for (Object entry : entries) {
                Iota iota = fragmentToIota(entry);
                converted.add(iota != null ? iota : new NullIota());
            }
            return new ListIota(converted);
        }
        if (TricksterReflection.entityFragmentClass.isInstance(fragment)) {
            Object rawUuid = TricksterReflection.entityUuidMethod.invoke(fragment);
            if (rawUuid instanceof UUID uuid) {
                net.minecraft.world.entity.Entity entity = resolveServerEntity(uuid);
                return entity != null ? new EntityIota(entity) : new NullIota();
            }
            return null;
        }
        if (TricksterReflection.stringFragmentClass.isInstance(fragment)) {
            Object value = TricksterReflection.stringValueMethod.invoke(fragment);
            if (value instanceof String s && ListPatternIotaValidator.isListOrPatternIotaDisplay(s)) {
                return ListPatternIotaParser.restoreFromDisplay(s).orElse(new NullIota());
            }
        }
        String spellData = null;
        if (TricksterReflection.ensureReadInit()) {
            Object base64 = TricksterReflection.fragmentToBase64Method.invoke(fragment);
            if (base64 instanceof String s && !s.isBlank()) {
                spellData = s;
            }
        }
        return new TrickIota(spellData);
    }

    private static String augmentReservedListDisplay(ListIota list, String display) {
        if (display == null || display.isEmpty()) return display;
        List<DisplayMetadataInsertion> insertions = new ArrayList<>();
        collectReservedListMetadata(list, insertions);
        if (insertions.isEmpty()) return display;

        StringBuilder builder = new StringBuilder(display);
        int searchFrom = 0;
        int offset = 0;
        for (DisplayMetadataInsertion insertion : insertions) {
            String token = insertion.displayToken();
            if (token == null || token.isEmpty()) continue;
            int tokenIndex = display.indexOf(token, searchFrom);
            if (tokenIndex < 0) continue;
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
                    LIST_ENTITY_METADATA_PREFIX + MetadataEscaper.sanitize(entityId) + LIST_METADATA_SUFFIX
            ));
        }
        if (iota instanceof ContinuationIota continuationIota) {
            String continuationPayload = serializeContinuationMetadata(continuationIota.getContinuation());
            insertions.add(new DisplayMetadataInsertion(
                    iota.display().getString(),
                    LIST_CONTINUATION_METADATA_PREFIX + MetadataEscaper.sanitize(continuationPayload) + LIST_METADATA_SUFFIX
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
            if (encoded != null) return encoded.toString();
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to encode continuation metadata", t);
        }
        return String.valueOf(continuation);
    }

    static net.minecraft.world.entity.Entity resolveServerEntity(UUID entityId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
            if (entity != null) return entity;
        }
        return null;
    }

    private record DisplayMetadataInsertion(String displayToken, String suffix) {}
}
