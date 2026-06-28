package com.example.walkietalkie.client.voice;

import com.example.walkietalkie.item.WalkieTalkieItem;
import com.example.walkietalkie.voice.WalkieVoiceServerAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import su.plo.config.entry.BooleanConfigEntry;
import su.plo.slib.api.chat.component.McTextComponent;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.audio.capture.ClientActivation;
import su.plo.voice.api.client.config.addon.AddonConfig;
import su.plo.voice.api.client.event.audio.capture.ClientActivationRegisteredEvent;
import su.plo.voice.api.event.EventSubscribe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Client bridge: drives the "walkie_talkie" ClientActivation while the player
 * holds right-click on an enabled walkie-talkie past the hold threshold, and
 * owns the addon's own settings block in PV's Addons menu (the same
 * {@code AddonConfig} mechanism pv-addon-soundphysics and other PV addons use).
 *
 * The PV client activation API is not publicly documented, so we call
 * ClientActivation#setActivated(boolean) via reflection to avoid a compile-time
 * dependency on a method that might not exist. If the call fails, the fallback
 * (bind a key in PV Settings → Activation) continues to work automatically.
 */
@Addon(
        id = "wt-addon-client",
        name = "Walkie Talkie (client)",
        version = "1.0.4",
        authors = {"SimpleFox"}
)
public final class WalkieVoiceClientAddon implements AddonInitializer {

    @InjectPlasmoVoice
    private PlasmoVoiceClient voiceClient;

    private boolean transmitting = false;

    private AddonConfig config;
    private BooleanConfigEntry voiceByDefaultEntry;

    @Override
    public void onAddonInitialize() {
        NeoForge.EVENT_BUS.addListener(this::onClientTick);

        // The addon's own block in PV's Addons settings menu. Backed by PV's normal
        // client config (config/plasmovoice/client.toml, "addons" section) -- same
        // mechanism other PV addons (e.g. pv-addon-soundphysics) use for their config.
        this.config = voiceClient.getAddonConfig(this);
        config.clear();
        this.voiceByDefaultEntry = config.addToggle(
                "voice-default",
                McTextComponent.translatable("config.walkietalkie.voice_default"),
                McTextComponent.translatable("config.walkietalkie.voice_default.tooltip"),
                true
        );
    }

    /**
     * PV creates a brand-new per-activation config entry (type = Push-to-Talk) the
     * first time a player ever sees this activation, requiring a separate PTT key on
     * top of holding the item -- which defeats the point of a press-to-talk radio.
     * Nudge it to Voice once, unless the player has since picked something else
     * themselves in PV's own Activation settings.
     */
    @EventSubscribe
    public void onActivationRegistered(ClientActivationRegisteredEvent event) {
        ClientActivation activation = event.getActivation();
        if (!WalkieVoiceServerAddon.ACTIVATION_NAME.equals(activation.getName())) return;
        if (voiceByDefaultEntry == null || !voiceByDefaultEntry.value()) return;
        if (activation.getType() == ClientActivation.Type.VOICE) return;

        setDefaultToVoice(activation);
    }

    /**
     * PV doesn't expose a public setter for an activation's Push-to-Talk/Voice/Inherit
     * type (ClientActivation only has a getter) -- it's meant to be the player's own
     * choice. We reach into the private per-activation config entry the same way PV's
     * own code does internally (see VoiceClientActivationManager#register, which does
     * the equivalent for the built-in proximity activation), but via reflection so a PV
     * update that renames things can't break the build.
     */
    private void setDefaultToVoice(ClientActivation activation) {
        try {
            Field field = activation.getClass().getDeclaredField("configType");
            field.setAccessible(true);
            Object configType = field.get(activation);

            Method isDefault = configType.getClass().getMethod("isDefault");
            if (!(boolean) isDefault.invoke(configType)) return; // player already chose something

            for (Method setter : configType.getClass().getMethods()) {
                if (setter.getName().equals("set") && setter.getParameterCount() == 1) {
                    setter.invoke(configType, ClientActivation.Type.VOICE);
                    break;
                }
            }
        } catch (Exception ignored) {
            // PV internals renamed/changed -- players can still flip it to Voice
            // themselves in PV's Activation settings.
        }
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
