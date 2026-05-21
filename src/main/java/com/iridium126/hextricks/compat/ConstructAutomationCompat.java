package com.iridium126.hextricks.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ConstructAutomationCompat {
    private static final ResourceLocation SPELL_CONSTRUCT_ID = ResourceLocation.fromNamespaceAndPath("trickster", "spell_construct");
    private static final ResourceLocation MODULAR_SPELL_CONSTRUCT_ID = ResourceLocation.fromNamespaceAndPath("trickster", "modular_spell_construct");

    private ConstructAutomationCompat() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ConstructAutomationCompat::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        Block spellConstruct = BuiltInRegistries.BLOCK.get(SPELL_CONSTRUCT_ID);
        Block modularSpellConstruct = BuiltInRegistries.BLOCK.get(MODULAR_SPELL_CONSTRUCT_ID);

        if (spellConstruct != Blocks.AIR) {
            registerForBlock(event, spellConstruct);
            registerForBlockEntity(event, SPELL_CONSTRUCT_ID);
        }
        if (modularSpellConstruct != Blocks.AIR) {
            registerForBlock(event, modularSpellConstruct);
            registerForBlockEntity(event, MODULAR_SPELL_CONSTRUCT_ID);
        }
    }

    private static void registerForBlock(RegisterCapabilitiesEvent event, Block block) {
        event.registerBlock(
                Capabilities.ItemHandler.BLOCK,
                (level, pos, state, blockEntity, side) -> createHandler(blockEntity),
                block
        );
    }

    private static void registerForBlockEntity(RegisterCapabilitiesEvent event, ResourceLocation id) {
        ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id);
        BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(key);
        if (type == null) {
            return;
        }
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                type,
                (blockEntity, side) -> createHandler(blockEntity)
        );
    }

    private static ConstructMediaItemHandler createHandler(BlockEntity blockEntity) {
        return ConstructMediaStorage.isConstruct(blockEntity) ? new ConstructMediaItemHandler(blockEntity) : null;
    }
}
