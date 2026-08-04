package com.example.walkietalkie.voice;

public final class RadioAudioEffect {

    private static final double MIX = 0.96D;
    private static final double GAIN = 0.94D;
    private static final double DISTORTION = 0.06D;
    private static final double NOISE = 0.03D;
    private static final double TREMOLO_DEPTH = 0.03D;
    private static final double TREMOLO_RATE = 2.10D;
    private static final double LOW_EQ_DB = -9.5D;
    private static final double MID_EQ_DB = 5.4D;
    private static final double HIGH_EQ_DB = -8.4D;
    private static final double BIT_DEPTH = 9.0D;

    private static final int SAMPLE_RATE = 48000;
    private static final double LOW_CUTOFF = 300.0D;
    private static final double HIGH_CUTOFF = 3400.0D;

    private double hpState;
    private double hpPrevIn;
    private double lpState;
    private double tremoloPhase;
    private long noiseSeed = 0x5DEECE66DL;

    public void reset() {
        hpState = 0;
        hpPrevIn = 0;
        lpState = 0;
        tremoloPhase = 0;
    }

    public void process(short[] samples, int channels) {
        if (samples == null || samples.length == 0) return;

        int ch = Math.max(1, channels);
        double dt = 1.0D / SAMPLE_RATE;

        double hpRc = 1.0D / (2 * Math.PI * LOW_CUTOFF);
        double hpAlpha = hpRc / (hpRc + dt);
        double lpRc = 1.0D / (2 * Math.PI * HIGH_CUTOFF);
        double lpAlpha = dt / (lpRc + dt);

        double lowGain = dbToLinear(LOW_EQ_DB);
        double midGain = dbToLinear(MID_EQ_DB);
        double highGain = dbToLinear(HIGH_EQ_DB);
        double quantStep = Math.pow(2.0D, 16.0D - BIT_DEPTH);

        for (int i = 0; i < samples.length; i += ch) {
            double in = samples[i] / 32768.0D;
            double dry = in;

            double hp = hpAlpha * (hpState + in - hpPrevIn);
            hpPrevIn = in;
            hpState = hp;

            lpState += lpAlpha * (hp - lpState);
            double band = lpState;

            double low = in - hp;
            double high = hp - band;
            double wet = band * midGain + low * lowGain + high * highGain;

            if (DISTORTION > 0) {
                double drive = 1.0D + DISTORTION * 24.0D;
                wet = Math.tanh(wet * drive) / Math.tanh(drive);
            }

            if (quantStep > 1.0D) {
                double scaled = wet * 32768.0D;
                wet = (Math.round(scaled / quantStep) * quantStep) / 32768.0D;
            }

            if (TREMOLO_DEPTH > 0) {
                double mod = 1.0D - TREMOLO_DEPTH * (0.5D - 0.5D * Math.cos(tremoloPhase));
                wet *= mod;
                tremoloPhase += 2 * Math.PI * TREMOLO_RATE * dt;
                if (tremoloPhase > 2 * Math.PI) tremoloPhase -= 2 * Math.PI;
            }

            if (NOISE > 0) {
                wet += (nextNoise() * 2.0D - 1.0D) * NOISE * 0.35D;
            }

            double out = (dry * (1.0D - MIX) + wet * MIX) * GAIN;
            short value = clamp(out);

            for (int c = 0; c < ch && i + c < samples.length; c++) {
                samples[i + c] = value;
            }
        }
    }

    private double nextNoise() {
        noiseSeed = (noiseSeed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return ((noiseSeed >>> 24) & 0xFFFFFF) / (double) 0xFFFFFF;
    }

    private static double dbToLinear(double db) {
        return Math.pow(10.0D, db / 20.0D);
    }

    private static short clamp(double v) {
        double s = v * 32768.0D;
        if (s > 32767.0D) return 32767;
        if (s < -32768.0D) return -32768;
        return (short) Math.round(s);
    }
}
