package com.example.walkietalkie.client;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.client.voice.WalkieVoiceClientAddon;
import com.example.walkietalkie.item.WalkieTalkieItem;
import com.example.walkietalkie.registry.WTItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import su.plo.voice.api.client.PlasmoVoiceClient;

/**
 * Client-only companion entrypoint (runs alongside the common {@code WalkieTalkieMod}).
 */
@Mod(value = WalkieTalkieMod.MOD_ID, dist = Dist.CLIENT)
public final class WalkieTalkieClient {

    private final WalkieVoiceClientAddon clientAddon = new WalkieVoiceClientAddon();

    public WalkieTalkieClient(IEventBus modBus) {
        // PV client addon loader (symmetric to PlasmoVoiceServer.getAddonsLoader()).
        PlasmoVoiceClient.getAddonsLoader().load(clientAddon);

        modBus.addListener(this::clientSetup);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        // ItemProperties' internal map isn't thread-safe; FMLClientSetupEvent runs off-thread.
        event.enqueueWork(() -> ItemProperties.register(
                WTItems.WALKIE_TALKIE.get(),
                ResourceLocation.fromNamespaceAndPath(WalkieTalkieMod.MOD_ID, "enabled"),
                (stack, level, entity, seed) -> WalkieTalkieItem.isEnabled(stack) ? 1.0F : 0.0F
        ));
    }
}
