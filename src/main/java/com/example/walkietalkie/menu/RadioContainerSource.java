package com.example.walkietalkie.menu;

import net.minecraft.world.entity.player.Player;

public interface RadioContainerSource {

    float getFrequency();
    void setFrequency(float frequency);
    boolean isEnabled();
    void setEnabled(boolean enabled);

    boolean isMicActive();
    void setMicActive(boolean active);

    boolean isOutputActive();
    void setOutputActive(boolean active);

    boolean stillValid(Player player);

    RadioContainerSource DUMMY = new RadioContainerSource() {
        private float frequency = 100F;
        private boolean enabled = false, micActive = false, outputActive = false;

        @Override public float getFrequency() { return frequency; }
        @Override public void setFrequency(float f) { this.frequency = f; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public void setEnabled(boolean v) { this.enabled = v; }
        @Override public boolean isMicActive() { return micActive; }
        @Override public void setMicActive(boolean v) { this.micActive = v; }
        @Override public boolean isOutputActive() { return outputActive; }
        @Override public void setOutputActive(boolean v) { this.outputActive = v; }
        @Override public boolean stillValid(Player p) { return true; }
    };
}
