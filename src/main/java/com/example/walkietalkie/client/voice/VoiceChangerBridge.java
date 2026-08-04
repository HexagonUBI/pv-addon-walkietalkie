package com.example.walkietalkie.client.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class VoiceChangerBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("WalkieTalkie/VoiceChanger");

    private static final String ADDON_CLASS = "org.sawiq.client.VoiceChangerAddon";
    private static final String PRESET_CLASS = "org.sawiq.client.model.VoiceChangerPreset";

    private static boolean resolved;
    private static boolean available;

    private static Object addon;
    private static Method isInitialized;
    private static Method isEffectEnabled;
    private static Method getSelectedPreset;
    private static Method applyBuiltInPreset;
    private static Method setEnabled;
    private static Object radioPreset;

    private static boolean overrideActive;
    private static Object savedPreset;
    private static boolean savedEnabled;

    private VoiceChangerBridge() {}

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> addonClass = Class.forName(ADDON_CLASS);
            Field instanceField = addonClass.getField("INSTANCE");
            addon = instanceField.get(null);

            isInitialized = addonClass.getMethod("isInitialized");
            isEffectEnabled = addonClass.getMethod("isEffectEnabled");
            getSelectedPreset = addonClass.getMethod("getSelectedPreset");
            setEnabled = addonClass.getMethod("setEnabled", boolean.class);

            Class<?> presetClass = Class.forName(PRESET_CLASS);
            applyBuiltInPreset = addonClass.getMethod("applyBuiltInPreset", presetClass);
            radioPreset = Enum.valueOf(presetClass.asSubclass(Enum.class), "RADIO");

            available = true;
            LOGGER.info("pv-voice-changer detected - radio effect will be forced while transmitting");
        } catch (ClassNotFoundException e) {
            LOGGER.info("pv-voice-changer not installed - radio effect integration disabled");
        } catch (Exception e) {
            LOGGER.warn("pv-voice-changer found but its API did not match - integration disabled", e);
        }
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    private static boolean ready() {
        resolve();
        if (!available) return false;
        try {
            return (boolean) isInitialized.invoke(addon);
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized void beginRadio() {
        if (overrideActive || !ready()) return;
        try {
            savedPreset = getSelectedPreset.invoke(addon);
            savedEnabled = (boolean) isEffectEnabled.invoke(addon);

            if (savedPreset == radioPreset && savedEnabled) {
                overrideActive = true;
                return;
            }

            applyBuiltInPreset.invoke(addon, radioPreset);
            if (!savedEnabled) setEnabled.invoke(addon, true);
            overrideActive = true;
        } catch (Exception e) {
            LOGGER.warn("Failed to apply the radio voice effect", e);
            overrideActive = false;
        }
    }

    public static synchronized void endRadio() {
        if (!overrideActive) return;
        overrideActive = false;
        if (!ready()) return;
        try {
            if (savedPreset != null && savedPreset != radioPreset) {
                applyBuiltInPreset.invoke(addon, savedPreset);
            }
            boolean nowEnabled = (boolean) isEffectEnabled.invoke(addon);
            if (nowEnabled != savedEnabled) {
                setEnabled.invoke(addon, savedEnabled);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to restore the previous voice changer preset", e);
        } finally {
            savedPreset = null;
        }
    }
}
