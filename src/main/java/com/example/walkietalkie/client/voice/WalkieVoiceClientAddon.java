package com.example.walkietalkie.client.voice;

import com.example.walkietalkie.client.WTClientSounds;
import com.example.walkietalkie.client.WTClientHooks;
import com.example.walkietalkie.item.WalkieTalkieItem;
import com.example.walkietalkie.net.payload.SfxVolumeC2S;
import com.example.walkietalkie.voice.WalkieVoiceServerAddon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import su.plo.config.entry.BooleanConfigEntry;
import su.plo.config.entry.DoubleConfigEntry;
import su.plo.slib.api.chat.component.McTextComponent;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.client.PlasmoVoiceClient;
import su.plo.voice.api.client.audio.capture.ClientActivation;
import su.plo.voice.api.client.config.addon.AddonConfig;
import su.plo.voice.api.client.event.audio.capture.ClientActivationRegisteredEvent;
import su.plo.voice.api.client.event.audio.capture.ClientActivationUnregisteredEvent;
import su.plo.voice.api.event.EventSubscribe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Addon(
        id = "wt-addon-client",
        name = "Walkie Talkie (client)",
        version = "1.0.5",
        authors = {"SimpleFox"}
)
public final class WalkieVoiceClientAddon implements AddonInitializer {

    private static WalkieVoiceClientAddon INSTANCE;

    public static float getSfxVolume() {
        WalkieVoiceClientAddon inst = INSTANCE;
        if (inst == null || inst.sfxVolumeEntry == null) return 0.3F;
        return inst.sfxVolumeEntry.value().floatValue();
    }

    @InjectPlasmoVoice
    private PlasmoVoiceClient voiceClient;

    private boolean transmitting = false;
    private ClientActivation walkieActivation;

    private AddonConfig config;
    private BooleanConfigEntry voiceByDefaultEntry;
    private DoubleConfigEntry sfxVolumeEntry;

    @Override
    public void onAddonInitialize() {
        INSTANCE = this;
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);

        this.config = voiceClient.getAddonConfig(this);
        config.clear();
        this.voiceByDefaultEntry = config.addToggle(
                "voice-default",
                McTextComponent.translatable("config.walkietalkie.voice_default"),
                McTextComponent.translatable("config.walkietalkie.voice_default.tooltip"),
                true
        );
        this.sfxVolumeEntry = config.addVolumeSlider(
                "sfx-volume",
                McTextComponent.translatable("config.walkietalkie.sfx_volume"),
                McTextComponent.translatable("config.walkietalkie.sfx_volume.tooltip"),
                "%",
                0.3D, 0.0D, 1.0D
        );
        sfxVolumeEntry.addChangeListener(volume -> sendSfxVolume());
    }

    private void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        WTClientSounds.stopAll();
        sendSfxVolume();
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WTClientSounds.stopAll();
    }

    private void sendSfxVolume() {
        if (sfxVolumeEntry == null) return;
        PacketDistributor.sendToServer(new SfxVolumeC2S(sfxVolumeEntry.value().floatValue()));
    }

    @EventSubscribe
    public void onActivationRegistered(ClientActivationRegisteredEvent event) {
        ClientActivation activation = event.getActivation();
        if (!WalkieVoiceServerAddon.ACTIVATION_NAME.equals(activation.getName())) return;

        this.walkieActivation = activation;
        this.transmitting = false;

        activation.setDisabled(true);

        if (voiceByDefaultEntry != null
                && voiceByDefaultEntry.value()
                && activation.getType() != ClientActivation.Type.VOICE) {
            setDefaultToVoice(activation);
        }
    }

    @EventSubscribe
    public void onActivationUnregistered(ClientActivationUnregisteredEvent event) {
        if (event.getActivation() == walkieActivation) {
            this.walkieActivation = null;
        }
    }

    private void setDefaultToVoice(ClientActivation activation) {
        try {
            Field field = activation.getClass().getDeclaredField("configType");
            field.setAccessible(true);
            Object configType = field.get(activation);

            Method isDefault = configType.getClass().getMethod("isDefault");
            if (!(boolean) isDefault.invoke(configType)) return;

            for (Method setter : configType.getClass().getMethods()) {
                if (setter.getName().equals("set") && setter.getParameterCount() == 1) {
                    setter.invoke(configType, ClientActivation.Type.VOICE);
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void onClientTick(ClientTickEvent.Post event) {
        if (walkieActivation == null) return;

        LocalPlayer player = Minecraft.getInstance().player;
        boolean shouldTransmit = player != null && wantsToTransmit(player);

        if (shouldTransmit != transmitting) {
            transmitting = shouldTransmit;
            walkieActivation.setDisabled(!shouldTransmit);
        }
    }

    private boolean wantsToTransmit(LocalPlayer player) {
        if (!player.isUsingItem()) return false;
        ItemStack using = player.getUseItem();
        if (!(using.getItem() instanceof WalkieTalkieItem)) return false;
        if (!WalkieTalkieItem.isEnabled(using)) return false;
        return player.getTicksUsingItem() >= WalkieTalkieItem.HOLD_THRESHOLD;
    }
}
