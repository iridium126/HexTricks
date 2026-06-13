package com.iridium126.hextricks.mixin;

import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import com.iridium126.hextricks.compat.SlateKnotHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockEntitySlate.class, remap = false)
public abstract class BlockEntitySlateKnotMixin extends BlockEntity implements SlateKnotHolder {
    @Unique
    private static final String HEXTRICKS_KNOT_TAG = "hextricks_knot";

    @Unique
    private ItemStack hextricks$knot = ItemStack.EMPTY;

    protected BlockEntitySlateKnotMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public ItemStack hextricks$getKnot() {
        return hextricks$knot;
    }

    @Override
    public void hextricks$setKnot(ItemStack stack) {
        hextricks$knot = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    @Override
    public ItemStack hextricks$removeKnot() {
        ItemStack knot = hextricks$knot;
        hextricks$knot = ItemStack.EMPTY;
        return knot;
    }

    @Inject(method = "saveModData", at = @At("TAIL"))
    private void hextricks$saveKnot(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (!hextricks$knot.isEmpty()) {
            tag.put(HEXTRICKS_KNOT_TAG, hextricks$knot.save(registries, new CompoundTag()));
        } else {
            tag.remove(HEXTRICKS_KNOT_TAG);
        }
    }

    @Inject(method = "loadModData", at = @At("TAIL"))
    private void hextricks$loadKnot(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        hextricks$knot = tag.contains(HEXTRICKS_KNOT_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(HEXTRICKS_KNOT_TAG))
                : ItemStack.EMPTY;
    }
}
