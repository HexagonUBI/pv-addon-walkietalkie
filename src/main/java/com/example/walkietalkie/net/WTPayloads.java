package com.example.walkietalkie.net;

import com.example.walkietalkie.item.WalkieTalkieItem;
import com.example.walkietalkie.net.payload.ConfigureWalkieC2S;
import com.example.walkietalkie.registry.WTComponents;
import com.example.walkietalkie.voice.RadioState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class WTPayloads {

    public static void register(RegisterPayloadHandlersEvent event) {
        // Bump the version string if you change a payload's wire format.
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                ConfigureWalkieC2S.TYPE,
                ConfigureWalkieC2S.STREAM_CODEC,
                WTPayloads::handleConfigure);
    }

    private static void handleConfigure(ConfigureWalkieC2S payload, IPayloadContext ctx) {
        // Handlers run off-thread; hop back onto the server thread before touching state.
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;

            ItemStack stack = sp.getItemInHand(payload.hand());
            if (!(stack.getItem() instanceof WalkieTalkieItem)) return;

            // Clamp to whatever range you decide to expose in the GUI.
            int freq = Math.max(0, Math.min(9999, payload.frequency()));
            stack.set(WTComponents.FREQUENCY.get(), freq);
            stack.set(WTComponents.ENABLED.get(), payload.enabled());

            RadioState.get(sp.server).refreshListeners(sp.server);
        });
    }

    private WTPayloads() {}
}
