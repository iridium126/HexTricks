package com.iridium126.hextricks.casting;

import com.iridium126.hextricks.compat.ConstructMediaStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public final class TricksterReflection {
    static volatile boolean readInitialized = false;
    static volatile boolean readAvailable = false;
    static volatile boolean executeInitialized = false;
    static volatile boolean executeAvailable = false;
    static volatile boolean registerInitialized = false;
    static volatile boolean registerAvailable = false;

    static Method fragmentGetMethod;
    static Method fragmentToBase64Method;
    static Method fragmentFromBase64Method;
    static Class<?> fragmentClass;
    static Class<?> spellPartClass;
    static Constructor<?> spellPartCtor;
    static Class<?> numberFragmentClass;
    static Constructor<?> numberFragmentCtor;
    static Method numberValueMethod;
    static Class<?> booleanFragmentClass;
    static Method booleanFragmentOfMethod;
    static Method booleanFragmentAsBooleanMethod;
    static Class<?> vectorFragmentClass;
    static Constructor<?> vectorFragmentCtor;
    static Method vectorXMethod;
    static Method vectorYMethod;
    static Method vectorZMethod;
    static Class<?> listFragmentClass;
    static Constructor<?> listFragmentCtor;
    static Method listFragmentsMethod;
    static Method asTextMethod;
    static Method componentGetStringMethod;
    static Method spellContextSourceMethod;
    static Class<?> blockSpellSourceClass;
    static Field blockSpellSourceBlockEntityField;
    static Method spellSourceGetPlayerMethod;
    static Method spellContextStateMethod;
    static Method executionStateGetArgumentsMethod;
    static Class<?> entityFragmentClass;
    static Constructor<?> entityFragmentCtor;
    static Method entityUuidMethod;
    static Method componentLiteralMethod;
    static Method stringValueMethod;
    static Class<?> stringFragmentClass;
    static Constructor<?> stringFragmentCtor;
    static Class<?> voidFragmentClass;
    static Object voidFragmentInstance;
    static Class<?> playerSpellSourceClass;
    static volatile Constructor<?> cachedPlayerSpellSourceCtor;
    static Constructor<?> defaultSpellExecutorCtor;
    static Method spellExecutorRunMethod;
    static Method spellSourceGetExecutionManagerMethod;
    static Method spellExecutionManagerQueueMethod;
    static Method spellExecutorGetLastRunExecutionsMethod;
    static Method spellExecutorGetDeepestStateMethod;
    static Method executionStateIsDelayedMethod;
    static Field tricksterConfigField;
    static Method tricksterConfigMaxExecutionsMethod;

    static Method tricksRegisterMethod;
    static Method patternOfMethod;
    static Constructor<?> loadArgumentTrickCtor;
    static Class<?> signatureClass;

    private TricksterReflection() {}

    static synchronized boolean ensureReadInit() {
        if (readInitialized) return readAvailable;
        readInitialized = true;
        try {
            Class<?> fragmentComponentClass = Class.forName("dev.enjarai.trickster.item.component.FragmentComponent");
            fragmentGetMethod = fragmentComponentClass.getMethod("getFragment", net.minecraft.world.item.ItemStack.class);

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
        if (executeInitialized) return executeAvailable;
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
            booleanFragmentAsBooleanMethod = booleanFragmentClass.getMethod("asBoolean");

            vectorFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.VectorFragment");
            Class<?> vector3dcClass = Class.forName("org.joml.Vector3dc");
            vectorFragmentCtor = vectorFragmentClass.getConstructor(vector3dcClass);
            vectorXMethod = vectorFragmentClass.getMethod("x");
            vectorYMethod = vectorFragmentClass.getMethod("y");
            vectorZMethod = vectorFragmentClass.getMethod("z");

            listFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.ListFragment");
            listFragmentCtor = listFragmentClass.getConstructor(List.class);
            listFragmentsMethod = listFragmentClass.getMethod("fragments");

            Class<?> spellContextClass = Class.forName("dev.enjarai.trickster.spell.SpellContext");
            spellContextSourceMethod = spellContextClass.getMethod("source");
            spellContextStateMethod = spellContextClass.getMethod("state");

            Class<?> spellSourceClass = Class.forName("dev.enjarai.trickster.spell.execution.source.SpellSource");
            blockSpellSourceClass = Class.forName("dev.enjarai.trickster.spell.execution.source.BlockSpellSource");
            blockSpellSourceBlockEntityField = blockSpellSourceClass.getField("blockEntity");
            spellSourceGetPlayerMethod = spellSourceClass.getMethod("getPlayer");

            Class<?> executionStateClass = Class.forName("dev.enjarai.trickster.spell.execution.ExecutionState");
            executionStateGetArgumentsMethod = executionStateClass.getMethod("getArguments");

            entityFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.EntityFragment");
            Class<?> textClass = Class.forName("net.minecraft.network.chat.Component");
            entityFragmentCtor = entityFragmentClass.getConstructor(java.util.UUID.class, textClass);
            entityUuidMethod = entityFragmentClass.getMethod("uuid");
            componentLiteralMethod = textClass.getMethod("literal", String.class);

            asTextMethod = fragmentClass.getMethod("asText");
            componentGetStringMethod = textClass.getMethod("getString");

            stringFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.StringFragment");
            stringFragmentCtor = stringFragmentClass.getConstructor(String.class);
            stringValueMethod = stringFragmentClass.getMethod("value");

            voidFragmentClass = Class.forName("dev.enjarai.trickster.spell.fragment.VoidFragment");
            voidFragmentInstance = voidFragmentClass.getField("INSTANCE").get(null);

            playerSpellSourceClass = Class.forName("dev.enjarai.trickster.spell.execution.source.PlayerSpellSource");
            cachedPlayerSpellSourceCtor = resolvePlayerSpellSourceCtor(ServerPlayer.class);

            Class<?> defaultSpellExecutorClass = Class.forName("dev.enjarai.trickster.spell.execution.executor.DefaultSpellExecutor");
            defaultSpellExecutorCtor = defaultSpellExecutorClass.getConstructor(spellPartClass, List.class);

            Class<?> spellExecutorClass = Class.forName("dev.enjarai.trickster.spell.SpellExecutor");
            spellExecutorGetLastRunExecutionsMethod = spellExecutorClass.getMethod("getLastRunExecutions");
            spellExecutorGetDeepestStateMethod = spellExecutorClass.getMethod("getDeepestState");

            spellExecutorRunMethod = defaultSpellExecutorClass.getMethod("run", spellSourceClass);
            spellSourceGetExecutionManagerMethod = spellSourceClass.getMethod("getExecutionManager");

            Class<?> spellExecutionManagerClass = Class.forName("dev.enjarai.trickster.spell.execution.SpellExecutionManager");
            spellExecutionManagerQueueMethod = spellExecutionManagerClass.getMethod("queue", spellExecutorClass);

            executionStateIsDelayedMethod = executionStateClass.getMethod("isDelayed");

            Class<?> tricksterClass = Class.forName("dev.enjarai.trickster.Trickster");
            tricksterConfigField = tricksterClass.getField("CONFIG");
            Object config = tricksterConfigField.get(null);
            if (config != null) {
                tricksterConfigMaxExecutionsMethod = config.getClass().getMethod("maxExecutionsPerSpellPerTick");
            }

            executeAvailable = true;
        } catch (Throwable ignored) {
            executeAvailable = false;
        }
        return executeAvailable;
    }

    static synchronized boolean ensureRegisterInit() {
        if (registerInitialized) return registerAvailable;
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

    static Constructor<?> resolvePlayerSpellSourceCtor(Class<?> playerClass) {
        Constructor<?> cached = cachedPlayerSpellSourceCtor;
        if (cached != null) {
            Class<?>[] params = cached.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(playerClass)) return cached;
        }
        if (playerSpellSourceClass == null) return null;
        for (Constructor<?> ctor : playerSpellSourceClass.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(playerClass)) {
                cachedPlayerSpellSourceCtor = ctor;
                return ctor;
            }
        }
        return null;
    }

    static Object getFragmentStringValue(Object fragment) {
        if (fragment == null || stringValueMethod == null) return null;
        try {
            Object val = stringValueMethod.invoke(fragment);
            return val instanceof String ? val : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static String getFragmentAsText(Object fragment) {
        if (fragment == null || asTextMethod == null || componentGetStringMethod == null) return null;
        try {
            Object text = asTextMethod.invoke(fragment);
            if (text == null) return null;
            Object str = componentGetStringMethod.invoke(text);
            return str instanceof String s ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static List<?> getListFragments(Object fragment) {
        if (fragment == null || listFragmentsMethod == null) return null;
        try {
            Object raw = listFragmentsMethod.invoke(fragment);
            return raw instanceof List<?> list ? list : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static List<?> getSpellContextArguments(Object spellContext) {
        if (spellContext == null || spellContextStateMethod == null || executionStateGetArgumentsMethod == null) return null;
        try {
            Object state = spellContextStateMethod.invoke(spellContext);
            if (state == null) return null;
            Object args = executionStateGetArgumentsMethod.invoke(state);
            return args instanceof List<?> list ? list : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static ServerPlayer resolveCasterPlayer(Object spellContext) {
        if (spellContext == null || spellContextSourceMethod == null || spellSourceGetPlayerMethod == null) return null;
        try {
            Object source = spellContextSourceMethod.invoke(spellContext);
            if (source == null) return null;
            Object playerOpt = spellSourceGetPlayerMethod.invoke(source);
            if (playerOpt instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof ServerPlayer player) {
                return player;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    static Optional<BlockEntity> resolveConstructBlockEntity(Object spellContext) {
        if (spellContext == null || spellContextSourceMethod == null || blockSpellSourceClass == null
                || blockSpellSourceBlockEntityField == null) {
            return Optional.empty();
        }
        try {
            Object source = spellContextSourceMethod.invoke(spellContext);
            if (source == null || !blockSpellSourceClass.isInstance(source)) {
                return Optional.empty();
            }
            Object blockEntity = blockSpellSourceBlockEntityField.get(source);
            if (blockEntity instanceof BlockEntity entity && ConstructMediaStorage.isConstruct(entity)) {
                return Optional.of(entity);
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    static Optional<ServerLevel> resolveConstructLevel(Object spellContext) {
        return resolveConstructBlockEntity(spellContext)
                .map(BlockEntity::getLevel)
                .filter(ServerLevel.class::isInstance)
                .map(ServerLevel.class::cast);
    }
}
