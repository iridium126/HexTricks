package com.iridium126.hextricks;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.iridium126.hextricks.casting.HexTricksIotaTypes;
import com.iridium126.hextricks.casting.HexTricksActions;

import at.petrak.hexcasting.common.lib.HexRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(HexTricks.MODID)
public class HexTricks {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "hextricks";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public HexTricks(IEventBus modEventBus, ModContainer modContainer) {
        // Register custom HexCasting iota types for this addon
        HexTricksIotaTypes.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (HexTricks) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        //modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerHexActions);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        //modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerHexActions(RegisterEvent event) {
        if (!event.getRegistryKey().equals(HexRegistries.ACTION)) {
            return;
        }
        HexTricksActions.register((entry, id) ->
                event.register(HexRegistries.ACTION, id, () -> entry));
    }
}
