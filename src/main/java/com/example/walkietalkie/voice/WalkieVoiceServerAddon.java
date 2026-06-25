package com.example.walkietalkie.voice;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.PlayerActivationInfo;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.source.ServerBroadcastSource;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plasmo Voice side of the walkie-talkie.
 *
 * The callbacks that PV calls (onPlayerActivation*) receive a VoicePlayer.
 * We immediately cast to VoiceServerPlayer (its server subtype) to get access
 * to server-side methods.  UUID is obtained via createPlayerInfo().getPlayerId()
 * which is the only UUID accessor available on VoicePlayer in PV 2.1.x.
 */
@Addon(
        id = "wt-addon-server",
        name = "Walkie Talkie",
        version = "1.0.2",
        authors = {"SimpleFox"}
)
public final class WalkieVoiceServerAddon implements AddonInitializer {

    public static final String ACTIVATION_NAME = "walkie_talkie";
    public static final String SOURCE_LINE_NAME = "walkie_talkie";

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;

    private ServerActivation activation;
    private ServerSourceLine sourceLine;

    /** One broadcast source per active speaker UUID. */
    private final Map<UUID, ServerBroadcastSource> speakerSources = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------

    @Override
    public void onAddonInitialize() {
        // Source line: controls how clients group + volume-adjust our audio.
        this.sourceLine = voiceServer.getSourceLineManager().createBuilder(
                this,
                SOURCE_LINE_NAME,
                "source_line.walkietalkie.name",
                "plasmovoice:textures/icons/speaker.png",
                10
        ).build();

        // Activation: the "push-to-talk channel" the client streams mic into.
        // Empty distances list = no player-controlled range (it's a radio).
        this.activation = voiceServer.getActivationManager().createBuilder(
                this,
                ACTIVATION_NAME,
                "activation.walkietalkie.name",
                "plasmovoice:textures/icons/microphone.png",
                "walkietalkie.activation",
                10
        ).build();

        // PV callbacks use VoicePlayer (the base type); we cast to VoiceServerPlayer.
        activation.onPlayerActivationStart(this::onActivationStart);
        activation.onPlayerActivation(this::onActivation);
        activation.onPlayerActivationEnd(this::onActivationEnd);
    }

    @Override
    public void onAddonShutdown() {
        speakerSources.values().forEach(ServerBroadcastSource::remove);
        speakerSources.clear();
    }

    // -------------------------------------------------------------------------

    /** Called when mic capture begins on the client. */
    private void onActivationStart(VoicePlayer vp) {
        UUID id = playerId(vp);
        // Always create a fresh source per transmission — never reuse a previous one.
        // PV registers each source internally; reusing causes the "replaces broadcast" bug.
        ServerBroadcastSource stale = speakerSources.remove(id);
        if (stale != null) stale.remove();
        speakerSources.put(id, sourceLine.createBroadcastSource(false));
    }

    /** Called for every 20 ms audio frame while the player is transmitting. */
    private ServerActivation.Result onActivation(VoicePlayer vp, PlayerAudioPacket packet) {
        UUID speakerId = playerId(vp);

        Integer freq = currentState().getTransmitFrequency(speakerId);
        if (freq == null) {
            // Radio is off or player isn't flagged as transmitting -> pass through.
            return ServerActivation.Result.IGNORED;
        }

        ServerBroadcastSource source = speakerSources.get(speakerId);
        if (source == null) return ServerActivation.Result.IGNORED;

        // Point the broadcast source at the current listener set.
        source.setPlayers(resolveListeners(freq, speakerId));

        // Forward the encoded+encrypted frame.  Broadcast sources don't need a
        // distance argument (unlike proximity sources) — distance is irrelevant
        // for a radio.  PlayerActivationInfo is advisory metadata consumed by
        // recording add-ons; omit it if the overload doesn't compile (it's optional).
        VoiceServerPlayer vsp = (VoiceServerPlayer) vp;
        source.sendAudioFrame(
                packet.getData(),
                packet.getSequenceNumber(),
                new PlayerActivationInfo(vsp, packet)
        );

        return ServerActivation.Result.HANDLED;
    }

    /** Called when the player stops transmitting. */
    private ServerActivation.Result onActivationEnd(VoicePlayer vp, PlayerAudioEndPacket packet) {
        UUID id = playerId(vp);
        ServerBroadcastSource source = speakerSources.remove(id);
        if (source != null) source.remove();
        return ServerActivation.Result.HANDLED;
    }

    // -------------------------------------------------------------------------

    /**
     * Returns the UUID of a VoicePlayer.
     * VoicePlayer.createPlayerInfo().getPlayerId() is the stable public API
     * in PV 2.1.x.  We do NOT call getInstance().getUUID() because McServerPlayer
     * does not expose getUUID() in this version.
     */
    private static UUID playerId(VoicePlayer vp) {
        return vp.createPlayerInfo().getPlayerId();
    }

    /** Resolve the set of VoicePlayers that should hear {@code freq}, minus the speaker. */
    private Set<VoicePlayer> resolveListeners(int freq, UUID speakerId) {
        Set<VoicePlayer> listeners = new HashSet<>();
        for (UUID uuid : currentState().listenersFor(freq)) {
            if (uuid.equals(speakerId)) continue;
            voiceServer.getPlayerManager()
                    .getPlayerById(uuid)
                    .ifPresent(listeners::add);
        }
        return listeners;
    }

    private RadioState currentState() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return RadioState.get(server);
    }
}
