package com.example.walkietalkie.voice;

import com.example.walkietalkie.WalkieTalkieMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Keeps the listener cache fresh for cases that don't go through a payload
 * (dropping/picking up a walkie, dying, etc.). Once per second is plenty.
 */
@EventBusSubscriber(modid = WalkieTalkieMod.MOD_ID)
public final class RadioTicker {

    private static int counter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++counter >= 20) {
            counter = 0;
            RadioState.get(event.getServer()).refreshListeners(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RadioState.clear();
    }

    private RadioTicker() {}
}
