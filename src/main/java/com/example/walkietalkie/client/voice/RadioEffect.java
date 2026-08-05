package com.example.walkietalkie.client.voice;

import com.example.walkietalkie.voice.RadioAudioEffect;
import com.example.walkietalkie.util.WTLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class RadioEffect {

    private static final WTLog LOGGER = WTLog.of("WalkieTalkie/RadioEffect");

    private static final String ENGINE_CLASS = "org.sawiq.client.audio.VoiceChangerAudioEngine";
    private static final String STATE_CLASS = "org.sawiq.client.audio.VoiceChangerAudioEngine$VoiceChangerState";
    private static final String PROFILE_CLASS = "org.sawiq.client.model.VoiceChangerProfile";
    private static final String PRESET_CLASS = "org.sawiq.client.model.VoiceChangerPreset";

    private static final int STRENGTH = 100;

    private static boolean resolved;
    private static boolean engineAvailable;
    private static Method processMethod;
    private static Constructor<?> stateConstructor;
    private static Object radioProfile;

    private final Object engineState;
    private final RadioAudioEffect fallback;

    public RadioEffect() {
        resolve();
        Object state = null;
        if (engineAvailable) {
            try {
                state = stateConstructor.newInstance();
            } catch (Exception e) {
                state = null;
            }
        }
        this.engineState = state;
        this.fallback = state == null ? new RadioAudioEffect() : null;
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> engineClass = Class.forName(ENGINE_CLASS);
            Class<?> stateClass = Class.forName(STATE_CLASS);
            Class<?> profileClass = Class.forName(PROFILE_CLASS);
            Class<?> presetClass = Class.forName(PRESET_CLASS);

            processMethod = engineClass.getDeclaredMethod(
                    "process", short[].class, int.class, profileClass, int.class, stateClass);
            processMethod.setAccessible(true);

            stateConstructor = stateClass.getDeclaredConstructor();
            stateConstructor.setAccessible(true);

            Method defaultsFor = profileClass.getMethod("defaultsFor", presetClass);
            Object radioPreset = Enum.valueOf(presetClass.asSubclass(Enum.class), "RADIO");
            radioProfile = defaultsFor.invoke(null, radioPreset);

            engineAvailable = true;
            LOGGER.info("Using pv-voice-changer's RADIO preset engine for the radio effect");
        } catch (ClassNotFoundException e) {
            LOGGER.info("pv-voice-changer not installed - using the built-in radio effect");
        } catch (Exception e) {
            LOGGER.warn("pv-voice-changer engine could not be used - falling back to the built-in radio effect", e);
        }
    }

    public void process(short[] samples, int channels) {
        if (engineState != null) {
            try {
                processMethod.invoke(null, samples, channels, radioProfile, STRENGTH, engineState);
                return;
            } catch (Exception e) {
                LOGGER.warn("pv-voice-changer engine failed mid-stream - using the built-in effect from now on", e);
                engineAvailable = false;
            }
        }
        if (fallback != null) fallback.process(samples, channels);
    }
}
