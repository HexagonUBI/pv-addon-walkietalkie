package com.example.walkietalkie.menu;

import net.minecraft.world.entity.player.Player;

/**
 * Whatever is "being configured" by a RadioMenu -- a handheld walkie-talkie item, or a
 * placed Radio Station block. The menu only ever talks to this interface, so it doesn't
 * need to know or care which one it's looking at.
 */
public interface RadioContainerSource {

    float getFrequency();

    void setFrequency(float frequency);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /** Whether the mic-transmit mode is active (only meaningful on Radio Stations with a mic module). */
    boolean isMicActive();

    void setMicActive(boolean active);

    /** Mirrors AbstractContainerMenu#stillValid -- should the menu stay open right now? */
    boolean stillValid(Player player);

    /**
     * Inert placeholder used only for the CLIENT-side menu instance (the one built from
     * the registered MenuType factory, with no real player/item/block behind it yet --
     * real values arrive moments later via the menu's synced ContainerData). Never used
     * server-side.
     */
    RadioContainerSource DUMMY = new RadioContainerSource() {
        private float frequency = 0F;
        private boolean enabled = false;
        private boolean micActive = false;

        @Override public float getFrequency() { return frequency; }
        @Override public void setFrequency(float frequency) { this.frequency = frequency; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public void setEnabled(boolean enabled) { this.enabled = enabled; }
        @Override public boolean isMicActive() { return micActive; }
        @Override public void setMicActive(boolean active) { this.micActive = active; }
        @Override public boolean stillValid(Player player) { return true; }
    };
}
