package com.example.walkietalkie.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WTServerConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue MIN_FREQUENCY;
    public static final ModConfigSpec.DoubleValue MAX_FREQUENCY;
    public static final ModConfigSpec.DoubleValue STATION_MIC_RANGE;
    public static final ModConfigSpec.BooleanValue STATION_RADIO_EFFECT;

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

        STATION_RADIO_EFFECT = builder
                .comment("Server-side radio effect on voice picked up by a Radio Station's microphone.",
                        "Off by default: the client applies the radio effect on playback instead,",
                        "which costs no server CPU and avoids a decode/re-encode quality loss.",
                        "Enable this only if you need the effect for players without this mod",
                        "installed - do not run both, or the effect is applied twice.")
                .define("radio-effect", false);

        builder.pop();

        SPEC = builder.build();
    }

    private WTServerConfig() {}
}
