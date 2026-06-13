package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import com.iridium126.hextricks.HexTricks;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class HexTricksActions {
    private static final Map<ResourceLocation, ActionRegistryEntry> ACTIONS = new LinkedHashMap<>();

    public static final ActionRegistryEntry EXEC_TRICK = make(
            "trick/execute",
            new ActionRegistryEntry(
                    HexPattern.fromAngles("wdwewawqwqw", HexDir.SOUTH_EAST),
                    OpExecuteTrick.INSTANCE
            )
    );
    public static final ActionRegistryEntry READ_TRICK = make(
            "trick/read",
            new ActionRegistryEntry(
                    HexPattern.fromAngles("wawqwqwqwqwq", HexDir.EAST),
                    OpReadTrickFromItem.INSTANCE
            )
    );

    private HexTricksActions() {
    }

    private static ActionRegistryEntry make(String name, ActionRegistryEntry entry) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(HexTricks.MODID, name);
        ActionRegistryEntry old = ACTIONS.put(id, entry);
        if (old != null) {
            throw new IllegalArgumentException("Duplicate action id " + id);
        }
        return entry;
    }

    public static void register(BiConsumer<ActionRegistryEntry, ResourceLocation> registrar) {
        for (Map.Entry<ResourceLocation, ActionRegistryEntry> e : ACTIONS.entrySet()) {
            registrar.accept(e.getValue(), e.getKey());
        }
    }
}
