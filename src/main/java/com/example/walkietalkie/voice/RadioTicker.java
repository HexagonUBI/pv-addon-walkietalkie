package com.example.walkietalkie.voice;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.item.WalkieTalkieItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

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
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        MinecraftServer server = sp.getServer();
        if (server == null) return;

        RadioState state = RadioState.get(server);
        Integer freq = state.getTransmitFrequency(sp.getUUID());
        if (freq != null) {
            WalkieTalkieItem.broadcastStaticState(server, freq, false);
            state.stopTransmitting(sp);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        RadioState.clear();
    }

    private RadioTicker() {}
}
