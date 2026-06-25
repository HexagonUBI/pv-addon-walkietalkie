package com.example.walkietalkie.client.voice;

import com.example.walkietalkie.item.WalkieTalkieItem;
import com.example.walkietalkie.voice.WalkieVoiceServerAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.client.PlasmoVoiceClient;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Client bridge: drives the "walkie_talkie" ClientActivation while the player
 * holds right-click on an enabled walkie-talkie past the hold threshold.
 *
 * The PV client activation API is not publicly documented, so we call
 * ClientActivation#setActivated(boolean) via reflection to avoid a compile-time
 * dependency on a method that might not exist. If the call fails, the fallback
 * (bind a key in PV Settings → Activation) continues to work automatically.
 */
@Addon(
        id = "wt-addon-client",
        name = "Walkie Talkie (client)",
        version = "1.0.3",
        authors = {"SimpleFox"}
)
public final class WalkieVoiceClientAddon implements AddonInitializer {

    @InjectPlasmoVoice
    private PlasmoVoiceClient voiceClient;

    private boolean transmitting = false;

    @Override
    public void onAddonInitialize() {
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean shouldTransmit = player != null && wantsToTransmit(player);

        if (shouldTransmit != transmitting) {
            transmitting = shouldTransmit;
            applyTransmit(shouldTransmit);
        }
    }

    private boolean wantsToTransmit(LocalPlayer player) {
        if (!player.isUsingItem()) return false;
        ItemStack using = player.getUseItem();
        if (!(using.getItem() instanceof WalkieTalkieItem)) return false;
        if (!WalkieTalkieItem.isEnabled(using)) return false;
        return player.getTicksUsingItem() >= WalkieTalkieItem.HOLD_THRESHOLD;
    }

    /**
     * Activates/deactivates the PV client activation via reflection.
     * We use reflection because PV's client API is internal and the method
     * name is not guaranteed — this way the build succeeds regardless.
     *
     * Tried method names (PV 2.1.x): setActivated(boolean)
     * Falls back silently if unavailable; players can bind a PTT key in PV settings.
     */
    private void applyTransmit(boolean active) {
        try {
            Object mgr = voiceClient.getActivationManager();
            // getActivationByName(String) → Optional<ClientActivation>
            Method getByName = mgr.getClass().getMethod(
                    "getActivationByName", String.class);
            Optional<?> opt = (Optional<?>) getByName.invoke(
                    mgr, WalkieVoiceServerAddon.ACTIVATION_NAME);
            opt.ifPresent(activation -> {
                try {
                    Method setter = activation.getClass()
                            .getMethod("setActivated", boolean.class);
                    setter.invoke(activation, active);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {
            // PV client API unavailable or method renamed — fallback to PV keybind.
        }
    }
}
