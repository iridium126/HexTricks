package com.iridium126.hextricks.casting;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.common.lib.HexRegistries;
import com.iridium126.hextricks.HexTricks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class HexTricksIotaTypes {
    public static final DeferredRegister<IotaType<?>> IOTA_TYPES =
            DeferredRegister.create(HexRegistries.IOTA_TYPE, HexTricks.MODID);

    public static final DeferredHolder<IotaType<?>, IotaType<TrickIota>> TRICK =
            IOTA_TYPES.register("trick", () -> TrickIota.TYPE);

    private HexTricksIotaTypes() {
    }

    public static void register(IEventBus modEventBus) {
        IOTA_TYPES.register(modEventBus);
    }
}
