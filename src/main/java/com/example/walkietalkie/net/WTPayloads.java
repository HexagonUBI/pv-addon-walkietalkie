package com.example.walkietalkie.net;

import com.example.walkietalkie.client.WTClientSounds;
import com.example.walkietalkie.item.WalkieTalkieItem;
import com.example.walkietalkie.menu.RadioMenu;
import com.example.walkietalkie.net.payload.RadioMenuUpdateC2S;
import com.example.walkietalkie.net.payload.SfxVolumeC2S;
import com.example.walkietalkie.net.payload.StaticStateS2C;
import com.example.walkietalkie.net.payload.ToggleWalkieC2S;
import com.example.walkietalkie.voice.RadioState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.example.walkietalkie.util.WTLog;

public final class WTPayloads {

    private static final WTLog LOGGER = WTLog.of("WalkieTalkie/Net");

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                RadioMenuUpdateC2S.TYPE,
                RadioMenuUpdateC2S.STREAM_CODEC,
                WTPayloads::handleRadioMenuUpdate);
        registrar.playToServer(
                ToggleWalkieC2S.TYPE,
                ToggleWalkieC2S.STREAM_CODEC,
                WTPayloads::handleToggle);
        registrar.playToServer(
                SfxVolumeC2S.TYPE,
                SfxVolumeC2S.STREAM_CODEC,
                WTPayloads::handleSfxVolume);
        registrar.playToClient(
                StaticStateS2C.TYPE,
                StaticStateS2C.STREAM_CODEC,
                WTPayloads::handleStaticState);
    }

    private static void handleRadioMenuUpdate(RadioMenuUpdateC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                LOGGER.warn("RadioMenuUpdateC2S from a non-ServerPlayer context - ignored");
                return;
            }
            if (sp.containerMenu.containerId != payload.containerId()) {
                LOGGER.warn("{} sent a RadioMenuUpdateC2S for containerId={} but has containerId={} open - ignored",
                        sp.getGameProfile().getName(), payload.containerId(), sp.containerMenu.containerId);
                return;
            }
            if (!(sp.containerMenu instanceof RadioMenu menu)) {
                LOGGER.warn("{} sent a RadioMenuUpdateC2S but their open menu isn't a RadioMenu - ignored",
                        sp.getGameProfile().getName());
                return;
            }
            menu.serverApply(payload.deciFrequency(), payload.enabled(), payload.micActive(), payload.outputActive());
        });
    }

    private static void handleToggle(ToggleWalkieC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!sp.isCreative()) return;

            int slot = payload.containerSlot();
            if (slot < 0 || slot >= sp.getInventory().getContainerSize()) return;

            ItemStack stack = sp.getInventory().getItem(slot);
            if (!(stack.getItem() instanceof WalkieTalkieItem)) return;

            WalkieTalkieItem.togglePower(stack, sp);
        });
    }

    private static void handleSfxVolume(SfxVolumeC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            RadioState.get(sp.server).setSfxVolume(sp.getUUID(), payload.volume());
        });
    }

    private static void handleStaticState(StaticStateS2C payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> WTClientSounds.setStatic(payload.frequency(), payload.active()));
    }

    private WTPayloads() {}
}
