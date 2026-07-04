package com.example.walkietalkie.registry;

import com.example.walkietalkie.WalkieTalkieMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Per-item state lives in Data Components (the 1.20.5+ replacement for raw NBT tags).
 *
 *   FREQUENCY — which channel this walkie is tuned to. One decimal place (e.g. 100.2),
 *               edited via the slider in the radio GUI; range is server-configured
 *               (see WTServerConfig).
 *   ENABLED   — whether the radio is powered on (you can hear the channel while on).
 *
 * Module/microphone slot contents use vanilla's own DataComponents.CONTAINER
 * (the same component Bundles and Shulker Boxes use) rather than a custom component --
 * no need to reinvent item-stack-backed inventory storage.
 *
 * Both FREQUENCY and ENABLED are persisted (saved with the stack) and network-
 * synchronized (so the client GUI and item tooltip can read them).
 */
public final class WTComponents {

    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(WalkieTalkieMod.MOD_ID);

    public static final Supplier<DataComponentType<Float>> FREQUENCY =
            REGISTER.registerComponentType("frequency", builder -> builder
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT));

    public static final Supplier<DataComponentType<Boolean>> ENABLED =
            REGISTER.registerComponentType("enabled", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    private WTComponents() {}
}
