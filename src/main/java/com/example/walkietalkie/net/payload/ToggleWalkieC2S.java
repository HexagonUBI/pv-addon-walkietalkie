package com.example.walkietalkie.net.payload;

import com.example.walkietalkie.WalkieTalkieMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToggleWalkieC2S(int containerSlot) implements CustomPacketPayload {

    public static final Type<ToggleWalkieC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WalkieTalkieMod.MOD_ID, "toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleWalkieC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ToggleWalkieC2S::containerSlot,
                    ToggleWalkieC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
