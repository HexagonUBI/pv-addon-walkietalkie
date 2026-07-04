package com.example.walkietalkie;

import com.example.walkietalkie.config.WTServerConfig;
import com.example.walkietalkie.net.WTPayloads;
import com.example.walkietalkie.registry.WTBlockEntities;
import com.example.walkietalkie.registry.WTBlocks;
import com.example.walkietalkie.registry.WTComponents;
import com.example.walkietalkie.registry.WTItems;
import com.example.walkietalkie.registry.WTMenus;
import com.example.walkietalkie.registry.WTSounds;
import com.example.walkietalkie.voice.WalkieVoiceServerAddon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import su.plo.voice.api.server.PlasmoVoiceServer;

/**
 * Walkie-Talkie — a Plasmo Voice add-on for NeoForge 1.21.1.
 *
 * Two "mods" live in here:
 *   1. A normal NeoForge mod (item, block, GUI, networking, data components, config).
 *   2. A Plasmo Voice *addon* ({@link WalkieVoiceServerAddon}) loaded into PV's own
 *      addon loader. PV addons are NOT NeoForge mods — they are plain objects handed
 *      to {@code PlasmoVoiceServer.getAddonsLoader().load(addon)}.
 */
@Mod(WalkieTalkieMod.MOD_ID)
public final class WalkieTalkieMod {

    public static final String MOD_ID = "walkietalkie";

    // The PV server addon instance. Loaded in the constructor (PV requirement:
    // it must be handed to the loader early; PV injects the API just before init).
    private final WalkieVoiceServerAddon voiceAddon = new WalkieVoiceServerAddon();

    public WalkieTalkieMod(IEventBus modBus, ModContainer container) {
        // ---- NeoForge registrations (mod event bus) ----
        WTComponents.REGISTER.register(modBus);
        WTItems.REGISTER.register(modBus);
        WTSounds.REGISTER.register(modBus);
        WTBlocks.REGISTER.register(modBus);
        WTBlockEntities.REGISTER.register(modBus);
        WTMenus.REGISTER.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(WTPayloads::register);     // RegisterPayloadHandlersEvent
        modBus.addListener(WTItems::addToCreativeTab); // BuildCreativeModeTabContentsEvent

        // Server config: min/max tunable frequency (config/walkietalkie-server.toml).
        container.registerConfig(ModConfig.Type.SERVER, WTServerConfig.SPEC);

        // ---- Hand our addon to Plasmo Voice's loader ----
        // Safe to call here: the mods.toml ordering = AFTER plasmovoice guarantees PV
        // is constructed first, so its static addon loader exists.
        PlasmoVoiceServer.getAddonsLoader().load(voiceAddon);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // nothing yet
    }
}
