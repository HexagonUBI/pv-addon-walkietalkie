package com.example.walkietalkie.net.payload;

import com.example.walkietalkie.WalkieTalkieMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent whenever the player drags the frequency slider or clicks the on/off button in a
 * RadioMenu (walkie-talkie or Radio Station screen). {@code containerId} is checked
 * server-side against the player's currently open menu before anything is applied --
 * see WTPayloads#handleRadioMenuUpdate.
 */
public record RadioMenuUpdateC2S(int containerId, int deciFrequency, boolean enabled, boolean micActive) implements CustomPacketPayload {

    public static final Type<RadioMenuUpdateC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WalkieTalkieMod.MOD_ID, "radio_menu_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RadioMenuUpdateC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RadioMenuUpdateC2S::containerId,
                    ByteBufCodecs.VAR_INT, RadioMenuUpdateC2S::deciFrequency,
                    ByteBufCodecs.BOOL, RadioMenuUpdateC2S::enabled,
                    ByteBufCodecs.BOOL, RadioMenuUpdateC2S::micActive,
                    RadioMenuUpdateC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
