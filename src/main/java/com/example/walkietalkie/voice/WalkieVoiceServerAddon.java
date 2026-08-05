package com.example.walkietalkie.voice;

import com.example.walkietalkie.compat.SableBridge;
import com.example.walkietalkie.config.WTServerConfig;
import com.example.walkietalkie.util.FrequencyUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import su.plo.slib.api.permission.PermissionDefault;
import su.plo.slib.api.server.entity.player.McServerPlayer;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.audio.codec.AudioDecoder;
import su.plo.voice.api.audio.codec.AudioEncoder;
import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.event.EventPriority;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.capture.PlayerActivationInfo;
import su.plo.voice.api.server.audio.capture.ServerActivation;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.source.ServerBroadcastSource;
import su.plo.voice.api.server.audio.source.ServerStaticSource;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEndEvent;
import su.plo.voice.api.server.event.audio.source.PlayerSpeakEvent;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.player.VoiceServerPlayer;
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket;
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Addon(
        id = "wt-addon-server",
        name = "Walkie Talkie",
        version = "1.0.5",
        authors = {"SimpleFox"}
)
public final class WalkieVoiceServerAddon implements AddonInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("WalkieTalkie/Voice");

    private static WalkieVoiceServerAddon INSTANCE;

    public static final short STATION_AUDIO_DISTANCE = 32;
    public static final String ACTIVATION_NAME = "walkie_talkie";
    public static final String SOURCE_LINE_NAME = "walkie_talkie";

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;

    private ServerActivation activation;
    private ServerSourceLine sourceLine;

    private final Map<UUID, ServerBroadcastSource> speakerSources = new ConcurrentHashMap<>();
    private final Map<BlockPos, ServerStaticSource> stationListenSources = new ConcurrentHashMap<>();
    private final Map<BlockPos, SpeakStation> speakStations = new ConcurrentHashMap<>();
    private final Map<BlockPos, ServerLevel> stationLevels = new ConcurrentHashMap<>();
    private final Map<UUID, ServerBroadcastSource> proximityRelaySources = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> proximityActiveFreqs = new ConcurrentHashMap<>();

    private record SpeakStation(ServerLevel level, int deciFrequency) {}

    private volatile boolean speakEventSeen = false;
    private volatile boolean radioEffectBroken = false;

    private final Map<UUID, RadioProcessor> radioProcessors = new ConcurrentHashMap<>();

    private final class RadioProcessor {
        private final AudioDecoder decoder;
        private final AudioEncoder encoder;
        private final RadioAudioEffect effect = new RadioAudioEffect();

        private RadioProcessor() throws Exception {
            this.decoder = voiceServer.createOpusDecoder(false);
            this.encoder = voiceServer.createOpusEncoder(false);
            this.decoder.open();
            this.encoder.open();
        }

        private synchronized byte[] apply(byte[] data) throws Exception {
            short[] pcm = decoder.decode(data);
            effect.process(pcm, 1);
            return encoder.encode(pcm);
        }

        private void close() {
            try { decoder.close(); } catch (Exception ignored) {}
            try { encoder.close(); } catch (Exception ignored) {}
        }
    }

    private byte[] applyRadioEffect(UUID speakerId, byte[] data) {
        if (radioEffectBroken || !WTServerConfig.STATION_RADIO_EFFECT.get()) return data;
        try {
            RadioProcessor proc = radioProcessors.get(speakerId);
            if (proc == null) {
                proc = new RadioProcessor();
                RadioProcessor existing = radioProcessors.putIfAbsent(speakerId, proc);
                if (existing != null) { proc.close(); proc = existing; }
            }
            return proc.apply(data);
        } catch (Exception e) {
            radioEffectBroken = true;
            LOGGER.error("Radio effect processing failed - relaying station audio unprocessed from now on", e);
            return data;
        }
    }

    private void releaseRadioProcessor(UUID speakerId) {
        RadioProcessor proc = radioProcessors.remove(speakerId);
        if (proc != null) proc.close();
    }

    @Override
    public void onAddonInitialize() {
        INSTANCE = this;
        LOGGER.info("Initializing Walkie Talkie voice addon");

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
        ).setPermissionDefault(PermissionDefault.TRUE)
         .build();

        activation.onPlayerActivationStart(this::onActivationStart);
        activation.onPlayerActivation(this::onActivation);
        activation.onPlayerActivationEnd(this::onActivationEnd);

        voiceServer.getEventBus().register(this, this);
        LOGGER.info("Registered PlayerSpeakEvent/PlayerSpeakEndEvent listeners for station mic-capture");

        LOGGER.info("Walkie Talkie voice addon initialized (permission default: everyone allowed)");
    }

    @Override
    public void onAddonShutdown() {
        LOGGER.info("Shutting down Walkie Talkie voice addon " +
                "({} speakers, {} stations, {} proximity relays)",
                speakerSources.size(), stationListenSources.size(), proximityRelaySources.size());
        speakerSources.values().forEach(ServerBroadcastSource::remove);
        speakerSources.clear();
        stationListenSources.values().forEach(ServerStaticSource::remove);
        stationListenSources.clear();
        proximityRelaySources.values().forEach(ServerBroadcastSource::remove);
        proximityRelaySources.clear();
        speakStations.clear();
        proximityActiveFreqs.clear();
        radioProcessors.values().forEach(RadioProcessor::close);
        radioProcessors.clear();
        INSTANCE = null;
    }

    public static void onStationUpdated(ServerLevel level, BlockPos pos,
                                        int deciFrequency, boolean listen, boolean speak) {
        WalkieVoiceServerAddon inst = INSTANCE;
        if (inst == null) {
            LOGGER.warn("onStationUpdated({}) called but the addon isn't initialized yet - ignoring", pos);
            return;
        }
        try {
            inst.updateStation(level, pos, deciFrequency, listen, speak);
        } catch (Exception e) {
            LOGGER.error("Failed to update station at {} (freq={}, listen={}, speak={})",
                    pos, deciFrequency, listen, speak, e);
        }
    }

    public static void onStationRemoved(ServerLevel level, BlockPos pos) {
        WalkieVoiceServerAddon inst = INSTANCE;
        if (inst == null) return;
        try {
            inst.removeStation(pos);
        } catch (Exception e) {
            LOGGER.error("Failed to remove station at {}", pos, e);
        }
    }

    private void updateStation(ServerLevel level, BlockPos pos,
                               int deciFrequency, boolean listen, boolean speak) {
        ServerStaticSource oldSrc = stationListenSources.remove(pos);
        if (oldSrc != null) oldSrc.remove();

        RadioState state = currentState();
        state.setStationInactive(pos);

        if (listen) {
            McServerWorld pvWorld = voiceServer.getMinecraftServer().getWorld(level);
            if (pvWorld != null) {
                net.minecraft.world.phys.Vec3 at = SableBridge.resolvePosition(level, pos);
                ServerPos3d pvPos = new ServerPos3d(pvWorld, at.x, at.y, at.z);
                ServerStaticSource src = sourceLine.createStaticSource(pvPos, false);
                src.setIconVisible(WTServerConfig.STATION_SHOW_ICON.get());
                stationListenSources.put(pos, src);
                stationLevels.put(pos, level);
                state.setStationActive(pos, deciFrequency);
            } else {
                LOGGER.warn("Station at {} in {} has no matching Plasmo Voice world - listen relay disabled",
                        pos, level.dimension().location());
            }
        }

        if (speak) speakStations.put(pos, new SpeakStation(level, deciFrequency));
        else speakStations.remove(pos);

        LOGGER.info("Station updated: pos={} dim={} freq={} listen={} speak={}",
                pos, level.dimension().location(), FrequencyUtil.fromDeci(deciFrequency), listen, speak);
    }

    public static void refreshStationPositions() {
        WalkieVoiceServerAddon inst = INSTANCE;
        if (inst == null || !SableBridge.isAvailable()) return;
        try {
            inst.updateSourcePositions();
        } catch (Exception e) {
            LOGGER.error("Failed to refresh station source positions", e);
        }
    }

    private void updateSourcePositions() {
        if (stationListenSources.isEmpty()) return;
        boolean iconVisible = WTServerConfig.STATION_SHOW_ICON.get();
        stationListenSources.forEach((pos, src) -> {
            if (src.isIconVisible() != iconVisible) src.setIconVisible(iconVisible);

            ServerLevel level = stationLevels.get(pos);
            if (level == null) return;
            net.minecraft.world.phys.Vec3 at = SableBridge.toWorldPosition(level, pos);
            if (at == null) return;

            ServerPos3d current = src.getPosition();
            if (current != null
                    && current.getX() == at.x && current.getY() == at.y && current.getZ() == at.z) {
                return;
            }

            McServerWorld pvWorld = voiceServer.getMinecraftServer().getWorld(level);
            if (pvWorld == null) return;
            src.setPosition(new ServerPos3d(pvWorld, at.x, at.y, at.z));
            src.setDirty();
        });
    }

    private void removeStation(BlockPos pos) {
        ServerStaticSource oldSrc = stationListenSources.remove(pos);
        if (oldSrc != null) oldSrc.remove();
        speakStations.remove(pos);
        stationLevels.remove(pos);
        currentState().setStationInactive(pos);
        LOGGER.info("Station removed: pos={}", pos);
    }

    private void onActivationStart(VoicePlayer vp) {
        try {
            UUID id = vp.getInstance().getUuid();
            ServerBroadcastSource stale = speakerSources.remove(id);
            if (stale != null) stale.remove();
            speakerSources.put(id, sourceLine.createBroadcastSource(false));
            LOGGER.info("{} started transmitting on the walkie-talkie activation", vp.getInstance().getUuid());
        } catch (Exception e) {
            LOGGER.error("onActivationStart failed", e);
        }
    }

    private ServerActivation.Result onActivation(VoicePlayer vp, PlayerAudioPacket packet) {
        try {
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
        } catch (Exception e) {
            LOGGER.error("onActivation failed", e);
            return ServerActivation.Result.IGNORED;
        }
    }

    private ServerActivation.Result onActivationEnd(VoicePlayer vp, PlayerAudioEndPacket packet) {
        try {
            UUID id = vp.getInstance().getUuid();
            Integer freq = currentState().getTransmitFrequency(id);
            if (freq != null) endOnStations(freq, packet.getSequenceNumber(), null);

            ServerBroadcastSource src = speakerSources.remove(id);
            if (src != null) src.remove();
            LOGGER.info("{} stopped transmitting on the walkie-talkie activation", id);
            return ServerActivation.Result.HANDLED;
        } catch (Exception e) {
            LOGGER.error("onActivationEnd failed", e);
            return ServerActivation.Result.IGNORED;
        }
    }

    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerSpeak(PlayerSpeakEvent event) {
        try {
            if (!speakEventSeen) {
                speakEventSeen = true;
                LOGGER.info("First PlayerSpeakEvent received - mic-capture hook is live ({} speak-enabled stations)",
                        speakStations.size());
            }
            if (speakStations.isEmpty()) return;

            VoicePlayer vp = event.getPlayer();
            if (!(vp instanceof VoiceServerPlayer vsp)) return;

            UUID speakerId = vp.getInstance().getUuid();
            if (currentState().getTransmitFrequency(speakerId) != null) return;

            McServerPlayer mcPlayer = vsp.getInstance();
            Object rawInstance = mcPlayer.getInstance();
            if (!(rawInstance instanceof ServerPlayer sp)) return;

            PlayerAudioPacket packet = event.getPacket();
            PlayerActivationInfo info = new PlayerActivationInfo(vsp, packet);

            double micRange = WTServerConfig.STATION_MIC_RANGE.get();

            Map<BlockPos, SpeakStation> inRange = new java.util.HashMap<>();
            speakStations.forEach((pos, station) -> {
                if (station.level() != sp.level()) return;
                net.minecraft.world.phys.Vec3 at = SableBridge.resolvePosition(station.level(), pos);
                double dx = at.x - sp.getX();
                double dy = at.y - sp.getY();
                double dz = at.z - sp.getZ();
                if (dx * dx + dy * dy + dz * dz > micRange * micRange) return;
                inRange.put(pos, station);
            });
            if (inRange.isEmpty()) return;

            byte[] radioData = applyRadioEffect(speakerId, packet.getData());

            inRange.forEach((pos, station) -> {
                int freq = station.deciFrequency();
                Set<Integer> playerFreqs = proximityActiveFreqs.computeIfAbsent(speakerId, k -> ConcurrentHashMap.newKeySet());
                if (playerFreqs.add(freq)) {
                    LOGGER.info("{} started speaking into the station at {} (freq={})",
                            speakerId, pos, FrequencyUtil.fromDeci(freq));
                }

                ServerBroadcastSource src = proximityRelaySources.computeIfAbsent(
                        speakerId, k -> sourceLine.createBroadcastSource(false));
                src.setPlayers(resolveListeners(freq, speakerId));
                src.sendAudioFrame(radioData, packet.getSequenceNumber(), info);
                relayToStations(freq, radioData, packet.getSequenceNumber(), info, pos);
            });
        } catch (Exception e) {
            LOGGER.error("onPlayerSpeak failed", e);
        }
    }

    @EventSubscribe(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerSpeakEnd(PlayerSpeakEndEvent event) {
        try {
            UUID speakerId = event.getPlayer().getInstance().getUuid();

            Set<Integer> freqs = proximityActiveFreqs.remove(speakerId);
            if (freqs != null) freqs.forEach(freq -> endOnStations(freq, event.getPacket().getSequenceNumber(), null));

            ServerBroadcastSource src = proximityRelaySources.remove(speakerId);
            if (src != null) src.remove();

            releaseRadioProcessor(speakerId);
        } catch (Exception e) {
            LOGGER.error("onPlayerSpeakEnd failed", e);
        }
    }

    private void relayToStations(int deciFrequency, byte[] data, long seq,
                                 @Nullable PlayerActivationInfo info) {
        relayToStations(deciFrequency, data, seq, info, null);
    }

    private void relayToStations(int deciFrequency, byte[] data, long seq,
                                 @Nullable PlayerActivationInfo info, @Nullable BlockPos exclude) {
        currentState().stationsForFrequency(deciFrequency).forEach(pos -> {
            if (pos.equals(exclude)) return;
            ServerStaticSource src = stationListenSources.get(pos);
            if (src != null) src.sendAudioFrame(data, seq, STATION_AUDIO_DISTANCE, info);
        });
    }

    private void endOnStations(int deciFrequency, long seq, @Nullable BlockPos exclude) {
        currentState().stationsForFrequency(deciFrequency).forEach(pos -> {
            if (pos.equals(exclude)) return;
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
