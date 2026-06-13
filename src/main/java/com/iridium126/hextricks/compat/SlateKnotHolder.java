package com.iridium126.hextricks.compat;

import net.minecraft.world.item.ItemStack;

public interface SlateKnotHolder {
    ItemStack hextricks$getKnot();

    void hextricks$setKnot(ItemStack stack);

    ItemStack hextricks$removeKnot();
}
