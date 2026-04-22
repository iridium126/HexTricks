package com.iridium126.hextricks.casting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

final class TricksterBridge {
    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    private static Object tricksRegistry;
    private static Method registryGetMethod;
    private static Method trickGetPatternMethod;
    private static Constructor<?> patternGlyphCtor;
    private static Constructor<?> spellPartCtor;
    private static Field casterKeyField;
    private static Method componentKeyGetMethod;
    private static Method queueSpellAndCastMethod;

    private TricksterBridge() {
    }

    static boolean tryQueueById(ServerPlayer player, String trickId) {
        try {
            if (!ensureInit()) {
                return false;
            }

            Object trick = registryGetMethod.invoke(tricksRegistry, ResourceLocation.parse(trickId));
            if (trick == null) {
                return false;
            }

            Object pattern = trickGetPatternMethod.invoke(trick);
            Object patternGlyph = patternGlyphCtor.newInstance(pattern);
            Object spellPart = spellPartCtor.newInstance(patternGlyph);

            Object casterKey = casterKeyField.get(null);
            Object casterComponent = componentKeyGetMethod.invoke(casterKey, player);
            if (casterComponent == null) {
                return false;
            }

            Object queueResult = queueSpellAndCastMethod.invoke(casterComponent, spellPart, List.of(), Optional.empty());
            return queueResult != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static synchronized boolean ensureInit() {
        if (initialized) {
            return available;
        }
        initialized = true;
        try {
            Class<?> tricksClass = Class.forName("dev.enjarai.trickster.spell.trick.Tricks");
            Field registryField = tricksClass.getField("REGISTRY");
            tricksRegistry = registryField.get(null);
            registryGetMethod = tricksRegistry.getClass().getMethod("get", Object.class);

            Class<?> trickClass = Class.forName("dev.enjarai.trickster.spell.trick.Trick");
            trickGetPatternMethod = trickClass.getMethod("getPattern");

            Class<?> patternGlyphClass = Class.forName("dev.enjarai.trickster.spell.PatternGlyph");
            Class<?> patternClass = Class.forName("dev.enjarai.trickster.spell.Pattern");
            patternGlyphCtor = patternGlyphClass.getConstructor(patternClass);

            Class<?> spellPartClass = Class.forName("dev.enjarai.trickster.spell.SpellPart");
            Class<?> fragmentClass = Class.forName("dev.enjarai.trickster.spell.Fragment");
            spellPartCtor = spellPartClass.getConstructor(fragmentClass);

            Class<?> modEntityComponentsClass = Class.forName("dev.enjarai.trickster.cca.ModEntityComponents");
            casterKeyField = modEntityComponentsClass.getField("CASTER");

            Class<?> componentKeyClass = Class.forName("org.ladysnake.cca.api.v3.component.ComponentKey");
            componentKeyGetMethod = componentKeyClass.getMethod("get", Object.class);

            Class<?> casterComponentClass = Class.forName("dev.enjarai.trickster.cca.CasterComponent");
            queueSpellAndCastMethod = casterComponentClass.getMethod(
                    "queueSpellAndCast",
                    spellPartClass,
                    List.class,
                    Optional.class
            );

            available = true;
        } catch (Throwable ignored) {
            available = false;
        }
        return available;
    }
}
