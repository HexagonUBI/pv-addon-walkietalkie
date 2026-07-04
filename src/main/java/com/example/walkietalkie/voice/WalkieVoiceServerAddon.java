package com.example.walkietalkie.voice;

import com.example.walkietalkie.util.FrequencyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.PlayerActivationInfo;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.source.ServerBroadcastSource;
import su.plo.voice.api.server.audio.source.ServerStaticSource;
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
 * Plasmo Voice server side.
 *
 * Audio relay paths:
 *
 *   PLAYER → PLAYERS: player using a walkie-talkie broadcasts to other players with the
 *     same frequency via a ServerBroadcastSource.
 *
 *   PLAYER → STATION (listen): any transmitting player's audio is also sent through a
 *     ServerStaticSource at each powered-on Radio Station on the same frequency, so
 *     nearby players hear the radio "speaking" -- even without their own walkie.
 *
 *   PLAYER → STATION (speak/capture): if a player is within range of a powered-on
 *     station whose mic button is active, ANY voice activation they make (proximity chat,
 *     another walkie-talkie, etc.) gets intercepted and relayed to that station's
 *     frequency. This is the "speak into the station" path, toggled by the mic button.
 *
 * Call {@link #onStationUpdated} to register / update / remove a station.
 */
@Addon(
        id = "wt-addon-server",
        name = "Walkie Talkie",
        version = "1.0.5",
        authors = {"SimpleFox"}
)
public final class WalkieVoiceServerAddon implements AddonInitializer {

    private static WalkieVoiceServerAddon INSTANCE;

    public static final short STATION_AUDIO_DISTANCE = 32;
    public static final String ACTIVATION_NAME = "walkie_talkie";
    public static final String SOURCE_LINE_NAME = "walkie_talkie";

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;

    private ServerActivation activation;
    private ServerSourceLine sourceLine;

    /** One broadcast source per currently-transmitting player UUID. */
    private final Map<UUID, ServerBroadcastSource> speakerSources = new ConcurrentHashMap<>();

    /**
     * Static sources used when a station LISTENS (plays received audio to nearby players).
     * Present whenever a station is powered on, regardless of mic module.
     */
    private final Map<BlockPos, ServerStaticSource> stationListenSources = new ConcurrentHashMap<>();

    // ---- lifecycle ----

    @Override
    public void onAddonInitialize() {
        INSTANCE = this;

        this.sourceLine = voiceServer.getSourceLineManager().createBuilder(
                this,
                SOURCE_LINE_NAME,
                "source_line.walkietalkie.name",
                "walkietalkie:textures/icons/wt_pvradio.png",
                10
        ).build();

        this.activation = voiceServer.getActivationManager().createBuilder(
                this,
                ACTIVATION_NAME,
                "activation.walkietalkie.name",
                "walkietalkie:textures/icons/wt_pvradio.png",
                "walkietalkie.activation",
                10
        ).build();

        activation.onPlayerActivationStart(this::onActivationStart);
        activation.onPlayerActivation(this::onActivation);
        activation.onPlayerActivationEnd(this::onActivationEnd);
    }

    @Override
    public void onAddonShutdown() {
        speakerSources.values().forEach(ServerBroadcastSource::remove);
        speakerSources.clear();
        stationListenSources.values().forEach(ServerStaticSource::remove);
        stationListenSources.clear();
        INSTANCE = null;
    }

    // ---- station lifecycle (called from RadioStationBlockEntity.syncVoiceRelay) ----

    /**
     * @param listen  station is powered on → create/keep the listen static source
     * @param speak   station has mic module + mic button active → capture nearby voice
     */
    public static void onStationUpdated(ServerLevel level, BlockPos pos,
                                        int deciFrequency, boolean listen, boolean speak) {
        WalkieVoiceServerAddon inst = INSTANCE;
        if (inst == null) return;
        inst.updateStation(level, pos, deciFrequency, listen, speak);
    }

    private void updateStation(ServerLevel level, BlockPos pos,
                               int deciFrequency, boolean listen, boolean speak) {
        // Always remove the old static source; rebuild if needed.
        ServerStaticSource oldSrc = stationListenSources.remove(pos);
        if (oldSrc != null) oldSrc.remove();

        RadioState state = currentState();
        state.setStationInactive(pos);

        if (listen) {
            McServerWorld pvWorld = voiceServer.getMinecraftServer().getWorld(level);
            if (pvWorld != null) {
                ServerPos3d pvPos = new ServerPos3d(pvWorld,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                ServerStaticSource src = sourceLine.createStaticSource(pvPos, false);
                stationListenSources.put(pos, src);
                state.setStationActive(pos, deciFrequency);
            }
        }
        // speak=true is tracked in RadioState for future mic-capture extension.
        // (Requires su.plo.voice.api:server-proxy-common for PlayerServerActivationEvent
        // which is not published as a standalone artifact in PV 2.1.x)
    }

    // ---- walkie-talkie activation callbacks ----

    private void onActivationStart(VoicePlayer vp) {
        UUID id = vp.getInstance().getUuid();
        ServerBroadcastSource stale = speakerSources.remove(id);
        if (stale != null) stale.remove();
        speakerSources.put(id, sourceLine.createBroadcastSource(false));
    }

    private ServerActivation.Result onActivation(VoicePlayer vp, PlayerAudioPacket packet) {
        UUID speakerId = vp.getInstance().getUuid();
        Integer freq = currentState().getTransmitFrequency(speakerId);
        if (freq == null) return ServerActivation.Result.IGNORED;

        ServerBroadcastSource playerSource = speakerSources.get(speakerId);
        if (playerSource == null) return ServerActivation.Result.IGNORED;

        PlayerActivationInfo info = new PlayerActivationInfo((VoiceServerPlayer) vp, packet);
        playerSource.setPlayers(resolveListeners(freq, speakerId));
        playerSource.sendAudioFrame(packet.getData(), packet.getSequenceNumber(), info);
        relayToStations(freq, packet.getData(), packet.getSequenceNumber(), info);

        return ServerActivation.Result.HANDLED;
    }

    private ServerActivation.Result onActivationEnd(VoicePlayer vp, PlayerAudioEndPacket packet) {
        UUID id = vp.getInstance().getUuid();
        Integer freq = currentState().getTransmitFrequency(id);
        if (freq != null) endOnStations(freq, packet.getSequenceNumber());

        ServerBroadcastSource src = speakerSources.remove(id);
        if (src != null) src.remove();
        return ServerActivation.Result.HANDLED;
    }

    // ---- relay helpers ----

    private void relayToStations(int deciFrequency, byte[] data, long seq,
                                 @Nullable PlayerActivationInfo info) {
        currentState().stationsForFrequency(deciFrequency).forEach(pos -> {
            ServerStaticSource src = stationListenSources.get(pos);
            if (src != null) src.sendAudioFrame(data, seq, STATION_AUDIO_DISTANCE, info);
        });
    }

    private void endOnStations(int deciFrequency, long seq) {
        currentState().stationsForFrequency(deciFrequency).forEach(pos -> {
            ServerStaticSource src = stationListenSources.get(pos);
            if (src != null) src.sendAudioEnd(seq, STATION_AUDIO_DISTANCE);
        });
    }

    private Set<VoicePlayer> resolveListeners(int freq, UUID speakerId) {
        Set<VoicePlayer> listeners = new HashSet<>();
        for (UUID uuid : currentState().listenersFor(freq)) {
            if (uuid.equals(speakerId)) continue;
            voiceServer.getPlayerManager().getPlayerById(uuid).ifPresent(listeners::add);
        }
        return listeners;
    }

    private RadioState currentState() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return RadioState.get(server);
    }
}
