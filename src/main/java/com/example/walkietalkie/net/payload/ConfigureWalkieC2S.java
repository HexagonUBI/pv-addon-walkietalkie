package com.example.walkietalkie.net.payload;

import com.example.walkietalkie.WalkieTalkieMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/**
 * Sent from the config screen to the server to persist the chosen settings onto the
 * walkie-talkie the player is holding in {@code hand}.
 */
public record ConfigureWalkieC2S(InteractionHand hand, int frequency, boolean enabled)
        implements CustomPacketPayload {

    public static final Type<ConfigureWalkieC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WalkieTalkieMod.MOD_ID, "configure"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigureWalkieC2S> STREAM_CODEC =
            StreamCodec.composite(
                    // InteractionHand has no built-in codec; send the ordinal.
                    ByteBufCodecs.VAR_INT.map(
                            i -> InteractionHand.values()[i],
                            InteractionHand::ordinal),
                    ConfigureWalkieC2S::hand,
                    ByteBufCodecs.VAR_INT, ConfigureWalkieC2S::frequency,
                    ByteBufCodecs.BOOL, ConfigureWalkieC2S::enabled,
                    ConfigureWalkieC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
