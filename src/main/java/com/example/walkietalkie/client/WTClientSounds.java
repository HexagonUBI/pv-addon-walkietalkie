package com.example.walkietalkie.client;

import com.example.walkietalkie.client.voice.WalkieVoiceClientAddon;
import com.example.walkietalkie.registry.WTSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

public final class WTClientSounds {

    private static final Map<Integer, SoundInstance> activeSounds = new HashMap<>();

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

    public static void stopAll() {
        var sm = Minecraft.getInstance().getSoundManager();
        activeSounds.values().forEach(sm::stop);
        activeSounds.clear();
    }

    private WTClientSounds() {}
}
