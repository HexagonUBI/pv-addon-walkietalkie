package com.example.walkietalkie.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WTServerConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue MIN_FREQUENCY;
    public static final ModConfigSpec.DoubleValue MAX_FREQUENCY;
    public static final ModConfigSpec.DoubleValue STATION_MIC_RANGE;

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

        builder.push("station");

        STATION_MIC_RANGE = builder
                .comment("How close (in blocks) a player must be to a powered-on Radio Station",
                        "with an active microphone module for their voice to be picked up and",
                        "relayed onto the station's frequency.")
                .defineInRange("mic-range", 5.0, 0.0, 256.0);

        builder.pop();

        SPEC = builder.build();
    }

    private WTServerConfig() {}
}
