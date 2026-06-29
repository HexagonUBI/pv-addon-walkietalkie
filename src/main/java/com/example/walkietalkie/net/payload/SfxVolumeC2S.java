package com.example.walkietalkie.net.payload;

import com.example.walkietalkie.WalkieTalkieMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Reports the player's current "SFX volume" AddonConfig slider value (see
 * {@code WalkieVoiceClientAddon}) so the server can scale the toggle/talk click sounds
 * to what each individual player asked for. Sent once on login and again whenever the
 * slider changes.
 */
public record SfxVolumeC2S(float volume) implements CustomPacketPayload {

    public static final Type<SfxVolumeC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WalkieTalkieMod.MOD_ID, "sfx_volume"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SfxVolumeC2S> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, SfxVolumeC2S::volume,
                    SfxVolumeC2S::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
