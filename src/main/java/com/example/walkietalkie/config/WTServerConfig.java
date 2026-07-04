package com.example.walkietalkie.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side config (one copy per world, in config/walkietalkie-server.toml).
 * Controls the tunable frequency range -- the slider in the radio GUI always spans
 * exactly [min-frequency, max-frequency], with the leftmost handle position being
 * min-frequency and the rightmost being max-frequency.
 */
public final class WTServerConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue MIN_FREQUENCY;
    public static final ModConfigSpec.DoubleValue MAX_FREQUENCY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "Tunable frequency range for walkie-talkies and Radio Stations.",
                "The in-game slider always spans exactly this range: dragging all the way",
                "left lands on min-frequency, all the way right lands on max-frequency.",
                "Values are edited at 0.1 resolution (e.g. 100.2)."
        ).push("frequency");

        MIN_FREQUENCY = builder
                .comment("Minimum tunable frequency.")
                .defineInRange("min-frequency", 80.0, 0.0, 99999.0);

        MAX_FREQUENCY = builder
                .comment("Maximum tunable frequency. If this ends up lower than min-frequency,",
                        "it's treated as equal to min-frequency (single fixed channel).")
                .defineInRange("max-frequency", 999.9, 0.0, 99999.0);

        builder.pop();

        SPEC = builder.build();
    }

    private WTServerConfig() {}
}
