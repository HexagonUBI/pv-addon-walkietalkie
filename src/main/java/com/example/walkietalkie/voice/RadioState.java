package com.example.walkietalkie.voice;

import com.example.walkietalkie.item.WalkieTalkieItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single source of truth, server-side, for:
 *   • which players are currently listening on which frequency, and
 *   • which players are currently transmitting (and on what frequency).
 *
 * A player "listens" on a frequency if they hold an ENABLED walkie tuned to it
 * anywhere in their inventory. Listener sets are cached and refreshed on config
 * changes, on toggle, and once per second by {@link RadioTicker}, rather than being
 * recomputed for every 20 ms audio frame.
 */
public final class RadioState {

    private static volatile RadioState instance;

    public static RadioState get(MinecraftServer server) {
        RadioState local = instance;
        if (local == null) {
            synchronized (RadioState.class) {
                local = instance;
                if (local == null) {
                    instance = local = new RadioState();
                }
            }
        }
        return local;
    }

    static void clear() {
        instance = null;
    }

    // freq -> set of listening player UUIDs
    private final Map<Integer, Set<UUID>> listenersByFreq = new ConcurrentHashMap<>();
    // transmitting player UUID -> freq
    private final Map<UUID, Integer> transmitting = new ConcurrentHashMap<>();

    private RadioState() {}

    // ---- transmit state (set from the item's use-tick / release) ----

    public void startTransmitting(ServerPlayer sp, int frequency) {
        transmitting.put(sp.getUUID(), frequency);
    }

    public void stopTransmitting(ServerPlayer sp) {
        transmitting.remove(sp.getUUID());
    }

    public Integer getTransmitFrequency(UUID uuid) {
        return transmitting.get(uuid);
    }

    // ---- listener cache ----

    public Set<UUID> listenersFor(int frequency) {
        return listenersByFreq.getOrDefault(frequency, Set.of());
    }

    /** Recompute the freq -> listeners map from all online players' inventories. */
    public void refreshListeners(MinecraftServer server) {
        Map<Integer, Set<UUID>> next = new HashMap<>();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            UUID uuid = sp.getUUID();
            // Scan the whole inventory; a player may carry several walkies.
            for (ItemStack stack : sp.getInventory().items) {
                if (stack.getItem() instanceof WalkieTalkieItem && WalkieTalkieItem.isEnabled(stack)) {
                    int freq = WalkieTalkieItem.frequencyOf(stack);
                    next.computeIfAbsent(freq, k -> new HashSet<>()).add(uuid);
                }
            }
        }
        listenersByFreq.clear();
        listenersByFreq.putAll(next);
    }
}
