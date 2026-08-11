package com.example.walkietalkie.client;

import com.example.walkietalkie.client.voice.WalkieVoiceClientAddon;
import com.example.walkietalkie.registry.WTSounds;
import com.example.walkietalkie.util.WTLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

public final class WTClientSounds {

    private static final WTLog LOGGER = WTLog.of("WalkieTalkie/Sounds");

    private static final float BUZZ_VOLUME = 0.7F;
    private static final float BUZZ_PITCH_MIN = 0.92F;
    private static final float BUZZ_PITCH_MAX = 1.08F;

    private static final Map<Integer, SoundInstance> activeSounds = new HashMap<>();
    private static final Map<BlockPos, SoundInstance> stationBuzz = new HashMap<>();

    public static void setStatic(int frequency, boolean active) {
        var sm = Minecraft.getInstance().getSoundManager();
        if (active) {
            if (activeSounds.containsKey(frequency)) return;

            float volume = WalkieVoiceClientAddon.getSfxVolume();

            var instance = new SimpleSoundInstance(
                    WTSounds.RADIO_STATIC.get().getLocation(),
                    SoundSource.PLAYERS,
                    volume, 1.0F,
                    RandomSource.create(),
                    true,
                    0,
                    SoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0,
                    true
            );
            sm.play(instance);
            activeSounds.put(frequency, instance);
        } else {
            SoundInstance instance = activeSounds.remove(frequency);
            if (instance != null) sm.stop(instance);
        }
    }

    public static void setStationBuzz(BlockPos pos, boolean active) {
        var sm = Minecraft.getInstance().getSoundManager();
        if (active) {
            if (stationBuzz.containsKey(pos)) return;

            var instance = new SimpleSoundInstance(
                    WTSounds.STATION_BUZZ.get().getLocation(),
                    SoundSource.BLOCKS,
                    BUZZ_VOLUME, buzzPitch(pos),
                    RandomSource.create(),
                    true,
                    0,
                    SoundInstance.Attenuation.LINEAR,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    false
            );
            stationBuzz.put(pos.immutable(), instance);
            sm.play(instance);
            LOGGER.info("Station buzz started at {}", pos);
        } else {
            SoundInstance instance = stationBuzz.remove(pos);
            if (instance != null) {
                sm.stop(instance);
                LOGGER.info("Station buzz stopped at {}", pos);
            }
        }
    }

    private static float buzzPitch(BlockPos pos) {
        int h = pos.hashCode();
        h ^= h >>> 16;
        float t = (h & 0xFF) / 255.0F;
        return BUZZ_PITCH_MIN + t * (BUZZ_PITCH_MAX - BUZZ_PITCH_MIN);
    }

    public static void stopAll() {
        var sm = Minecraft.getInstance().getSoundManager();
        activeSounds.values().forEach(sm::stop);
        activeSounds.clear();
        stationBuzz.values().forEach(sm::stop);
        stationBuzz.clear();
    }

    private WTClientSounds() {}
}
