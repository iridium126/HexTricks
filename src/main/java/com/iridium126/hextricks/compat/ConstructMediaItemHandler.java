package com.iridium126.hextricks.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;

public final class ConstructMediaItemHandler implements IItemHandler {
    private static final int MEDIA_SLOT = 0;

    private final BlockEntity blockEntity;

    public ConstructMediaItemHandler(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != MEDIA_SLOT || stack.isEmpty()) {
            return stack;
        }
        ItemStack remainder = ConstructMediaStorage.getInsertionRemainder(this.blockEntity, stack, simulate);
        return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == MEDIA_SLOT ? 64 : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == MEDIA_SLOT && ConstructMediaStorage.canInsertMedia(this.blockEntity, stack);
    }
}
