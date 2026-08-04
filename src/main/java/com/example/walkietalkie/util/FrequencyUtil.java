package com.example.walkietalkie.util;

import com.example.walkietalkie.config.WTServerConfig;

import java.util.Locale;

public final class FrequencyUtil {

    public static int toDeci(float freq) {
        return Math.round(freq * 10F);
    }

    public static float fromDeci(int deci) {
        return deci / 10F;
    }

    public static float min() {
        return (float) (double) WTServerConfig.MIN_FREQUENCY.get();
    }

    public static float max() {
        float max = (float) (double) WTServerConfig.MAX_FREQUENCY.get();
        float min = min();
        return Math.max(min, max);
    }

    public static float clamp(float freq) {
        return Math.max(min(), Math.min(max(), freq));
    }

    public static String format(float freq) {
        return String.format(Locale.ROOT, "%.1f", freq);
    }

    private FrequencyUtil() {}
}
