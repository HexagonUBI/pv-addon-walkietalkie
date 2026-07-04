package com.example.walkietalkie.util;

import com.example.walkietalkie.config.WTServerConfig;

import java.util.Locale;

/**
 * All frequency math lives here so the item, the Radio Station block entity, the menu,
 * and the screen agree on exactly the same rules.
 *
 * Frequencies are edited at 0.1 resolution (matches real-world radio tuning, e.g.
 * "100.2 MHz") and stored as a plain float. For network sync and the slider's internal
 * math we use a fixed-point "deci-frequency" int (freq * 10, rounded) instead of the raw
 * float -- floats don't compare equal reliably after a network round-trip, but ints do,
 * and DataSlot/ContainerData (the menu sync mechanism) is int-based anyway.
 */
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
        // Guard against a host config with max < min -- collapse to a single valid point
        // rather than producing an inverted/unusable slider.
        return Math.max(min, max);
    }

    /** Clamps to the current server-configured [min, max] range. */
    public static float clamp(float freq) {
        return Math.max(min(), Math.min(max(), freq));
    }

    /** "100.2" -- always one decimal place, locale-independent (radios don't use commas). */
    public static String format(float freq) {
        return String.format(Locale.ROOT, "%.1f", freq);
    }

    private FrequencyUtil() {}
}
