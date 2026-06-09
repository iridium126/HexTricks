package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.circles.BlockEntityAbstractImpetus;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import com.iridium126.hextricks.HexTricks;
import com.iridium126.hextricks.compat.SlateKnotHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class CircleSlateSpellSource {
    private CircleSlateSpellSource() {
    }

    static Object wrap(Object delegate, CircleCastEnv env) {
        try {
            BlockEntityAbstractImpetus impetus = env.getImpetus();
            if (impetus == null || !(impetus.getLevel() instanceof ServerLevel level)) {
                return delegate;
            }

            SlateKnotInventory inventory = new SlateKnotInventory(level, collectSlots(level, env));
            Object pool = TricksterReflection.cachedInventoryManaPoolCtor.newInstance(inventory);
            Object syncingPool = Proxy.newProxyInstance(
                    TricksterReflection.mutableManaPoolClass.getClassLoader(),
                    new Class<?>[]{TricksterReflection.mutableManaPoolClass},
                    (proxy, method, args) -> invokePool(pool, inventory, method, args)
            );

            return Proxy.newProxyInstance(
                    TricksterReflection.spellSourceClass.getClassLoader(),
                    new Class<?>[]{TricksterReflection.spellSourceClass},
                    (proxy, method, args) -> invokeSource(delegate, syncingPool, method, args)
            );
        } catch (Throwable t) {
            HexTricks.LOGGER.warn("Failed to create circle slate Trickster spell source", t);
            return delegate;
        }
    }

    private static List<SlateKnotSlot> collectSlots(ServerLevel level, CircleCastEnv env) {
        List<BlockPos> positions = new ArrayList<>(env.circleState().knownPositions);
        positions.sort(Comparator.comparingLong(BlockPos::asLong));

        List<SlateKnotSlot> slots = new ArrayList<>();
        for (BlockPos pos : positions) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof SlateKnotHolder holder) {
                slots.add(new SlateKnotSlot(blockEntity, holder));
            }
        }
        return slots;
    }

    private static Object invokeSource(Object delegate, Object pool, Method method, Object[] args) throws Throwable {
        if ("getManaPool".equals(method.getName()) && method.getParameterCount() == 0) {
            return pool;
        }
        return invoke(method, delegate, args);
    }

    private static Object invokePool(Object pool, SlateKnotInventory inventory, Method method, Object[] args)
            throws Throwable {
        Object result = invoke(method, pool, args);
        if (isMutatingManaPoolMethod(method)) {
            inventory.setChanged();
        }
        return result;
    }

    private static boolean isMutatingManaPoolMethod(Method method) {
        String name = method.getName();
        return method.getParameterCount() > 0
                && ("use".equals(name) || "refill".equals(name) || "set".equals(name) || "setMax".equals(name));
    }

    private static Object invoke(Method method, Object target, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private record SlateKnotSlot(BlockEntity blockEntity, SlateKnotHolder holder) {
    }

    private static final class SlateKnotInventory implements Container {
        private final ServerLevel level;
        private final List<SlateKnotSlot> slots;

        private SlateKnotInventory(ServerLevel level, List<SlateKnotSlot> slots) {
            this.level = level;
            this.slots = slots;
        }

        @Override
        public int getContainerSize() {
            return slots.size();
        }

        @Override
        public boolean isEmpty() {
            for (SlateKnotSlot slot : slots) {
                if (!slot.holder().hextricks$getKnot().isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            if (!isValidSlot(slot)) {
                return ItemStack.EMPTY;
            }
            return slots.get(slot).holder().hextricks$getKnot();
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (!isValidSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = stack.split(amount);
            if (stack.isEmpty()) {
                slots.get(slot).holder().hextricks$setKnot(ItemStack.EMPTY);
            }
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (!isValidSlot(slot)) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = getItem(slot);
            slots.get(slot).holder().hextricks$setKnot(ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!isValidSlot(slot)) {
                return;
            }
            slots.get(slot).holder().hextricks$setKnot(stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            for (SlateKnotSlot slot : slots) {
                BlockEntity blockEntity = slot.blockEntity();
                blockEntity.setChanged();
                level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (SlateKnotSlot slot : slots) {
                slot.holder().hextricks$setKnot(ItemStack.EMPTY);
            }
            setChanged();
        }

        private boolean isValidSlot(int slot) {
            return slot >= 0 && slot < slots.size();
        }
    }
}
