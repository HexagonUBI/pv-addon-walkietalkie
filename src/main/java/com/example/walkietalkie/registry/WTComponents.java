package com.example.walkietalkie.registry;

import com.example.walkietalkie.WalkieTalkieMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

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
