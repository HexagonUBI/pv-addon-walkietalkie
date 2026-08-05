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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import su.plo.voice.api.server.PlasmoVoiceServer;

@Mod(WalkieTalkieMod.MOD_ID)
public final class WalkieTalkieMod {

    public static final String MOD_ID = "walkietalkie";

    private static final Logger LOGGER = LoggerFactory.getLogger("WalkieTalkie");

    private final WalkieVoiceServerAddon voiceAddon = new WalkieVoiceServerAddon();

    public WalkieTalkieMod(IEventBus modBus, ModContainer container) {
        LOGGER.info("Loading Walkie Talkie mod (version {})", container.getModInfo().getVersion());

        WTComponents.REGISTER.register(modBus);
        WTItems.REGISTER.register(modBus);
        WTSounds.REGISTER.register(modBus);
        WTBlocks.REGISTER.register(modBus);
        WTBlockEntities.REGISTER.register(modBus);
        WTMenus.REGISTER.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(WTPayloads::register);
        modBus.addListener(WTItems::addToCreativeTab);

        container.registerConfig(ModConfig.Type.COMMON, WTServerConfig.SPEC, "walkietalkie.toml");

        LOGGER.info("Registering the Walkie Talkie Plasmo Voice addon");
        PlasmoVoiceServer.getAddonsLoader().load(voiceAddon);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Walkie Talkie common setup complete");
    }
}
