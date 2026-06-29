package com.example.walkietalkie.net.payload;

import com.example.walkietalkie.WalkieTalkieMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from the inventory-click toggle ({@code WalkieTalkieItem#overrideOtherStackedOnMe})
 * when the player is in creative mode. Creative-menu slot clicks only invoke Item click
 * overrides client-side (there's no authoritative server call to piggyback on like there
 * is in survival), so we tell the server which of the player's own inventory slots to
 * flip ourselves.
 */
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
