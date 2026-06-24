package com.example.walkietalkie.client;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.client.voice.WalkieVoiceClientAddon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import su.plo.voice.api.client.PlasmoVoiceClient;

/**
 * Client-only companion entrypoint (runs alongside the common {@code WalkieTalkieMod}).
 * Its only job is to hand the Plasmo Voice *client* addon to PV's client addon loader,
 * mirroring how the common mod loads the server addon.
 */
@Mod(value = WalkieTalkieMod.MOD_ID, dist = Dist.CLIENT)
public final class WalkieTalkieClient {

    private final WalkieVoiceClientAddon clientAddon = new WalkieVoiceClientAddon();

    public WalkieTalkieClient(IEventBus modBus) {
        // PV client addon loader (symmetric to PlasmoVoiceServer.getAddonsLoader()).
        PlasmoVoiceClient.getAddonsLoader().load(clientAddon);
    }
}
