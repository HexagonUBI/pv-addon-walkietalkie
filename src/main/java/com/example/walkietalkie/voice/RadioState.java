package com.example.walkietalkie.voice;

import com.example.walkietalkie.item.WalkieTalkieItem;
import com.example.walkietalkie.util.FrequencyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RadioState {

    public static final float DEFAULT_SFX_VOLUME = 0.3F;

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

    private final Map<Integer, Set<UUID>> listenersByFreq = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> transmitting = new ConcurrentHashMap<>();
    private final Map<UUID, Float> sfxVolume = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> activeStations = new ConcurrentHashMap<>();

    private RadioState() {}

    public void startTransmitting(ServerPlayer sp, int frequency) {
        transmitting.put(sp.getUUID(), frequency);
    }

    public void stopTransmitting(ServerPlayer sp) {
        transmitting.remove(sp.getUUID());
    }

    public Integer getTransmitFrequency(UUID uuid) {
        return transmitting.get(uuid);
    }

    public void setSfxVolume(UUID uuid, float volume) {
        sfxVolume.put(uuid, Math.max(0F, Math.min(1F, volume)));
    }

    public float sfxVolumeOf(UUID uuid) {
        return sfxVolume.getOrDefault(uuid, DEFAULT_SFX_VOLUME);
    }

    public void setStationActive(BlockPos pos, int deciFrequency) {
        activeStations.put(pos, deciFrequency);
    }

    public void setStationInactive(BlockPos pos) {
        activeStations.remove(pos);
    }

    public Map<BlockPos, Integer> activeStations() {
        return java.util.Collections.unmodifiableMap(activeStations);
    }

    public Set<BlockPos> stationsForFrequency(int deciFrequency) {
        Set<BlockPos> result = new HashSet<>();
        activeStations.forEach((pos, freq) -> {
            if (freq.equals(deciFrequency)) result.add(pos);
        });
        return result;
    }

    public Set<UUID> listenersFor(int frequency) {
        return listenersByFreq.getOrDefault(frequency, Set.of());
    }

    public void refreshListeners(MinecraftServer server) {
        Map<Integer, Set<UUID>> next = new HashMap<>();
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            UUID uuid = sp.getUUID();
            for (ItemStack stack : sp.getInventory().items) {
                if (stack.getItem() instanceof WalkieTalkieItem && WalkieTalkieItem.isEnabled(stack)) {
                    int freq = FrequencyUtil.toDeci(WalkieTalkieItem.frequencyOf(stack));
                    next.computeIfAbsent(freq, k -> new HashSet<>()).add(uuid);
                }
            }
        }
        listenersByFreq.clear();
        listenersByFreq.putAll(next);
    }
}
