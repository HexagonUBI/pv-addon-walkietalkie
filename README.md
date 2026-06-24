# Walkie Talkie — Plasmo Voice add-on (NeoForge 1.21.1)

A walkie-talkie item with selectable frequencies, built on Plasmo Voice 2.1.x.

* **Shift + right-click** → opens a vanilla-style config screen (frequency + power).
* **Quick tap right-click** → toggles the radio on/off.
* **Hold right-click** → push-to-talk on the current frequency.
* **Radio on (anywhere in inventory)** → you hear everyone transmitting on that frequency.

## How it fits together

| Layer | File(s) | Confidence |
|---|---|---|
| Item + gestures | `item/WalkieTalkieItem.java` | solid |
| Per-item state (frequency/on) | `registry/WTComponents.java` (data components) | solid |
| Config GUI | `client/WalkieTalkieScreen.java`, `client/WTClientHooks.java` | solid |
| Networking (GUI → server) | `net/…` | solid |
| **Frequency routing (who hears whom)** | `voice/WalkieVoiceServerAddon.java` + `voice/RadioState.java` | solid (a couple PV method names flagged `VERIFY`) |
| **Item-as-PTT mic capture** | `client/voice/WalkieVoiceClientAddon.java` | **needs you to wire one PV-client call** |

### The one thing to finish: client mic capture

Plasmo Voice captures the microphone **on the client**. The server only receives audio
when some client activation is actively capturing. So "hold the item to talk" needs a
client bridge that starts/stops capture into the `walkie_talkie` activation.

`WalkieVoiceClientAddon` already detects *when* to transmit (item held + enabled + past
threshold). The only TODO is the single call in `setTransmitting(boolean)` — see the two
documented strategies there, and the PV client Dokka under
`su.plo.voice.api.client.audio.capture`.

**Ship-now fallback (no PV internals):** leave `setTransmitting` as a no-op. The server
registers the `walkie_talkie` activation, so it shows up in PV's **Settings ▸ Activation**
tab; players bind a push-to-talk key there and hold it to talk on the frequency. The item
still handles config, on/off, and listening.

### `VERIFY` markers in the server addon

Two PV symbol names couldn't be confirmed from the public docs and are marked `VERIFY`:
* `VoiceServerPlayer#getInstance().getUUID()` — getting the player UUID.
* `PlayerManager#getPlayerById(UUID)` — and the broadcast `sendAudioFrame(...)` overload.

Open the PV Dokka (https://dokka.plasmovoice.com) for exact 2.1.x signatures.

## Your custom model

Drop your model JSON at:
```
src/main/resources/assets/walkietalkie/models/item/walkie_talkie.json
```
(replacing the placeholder). If it's a flat icon, also add
`textures/item/walkie_talkie.png`.

## Build

```
./gradlew build        # jar in build/libs
./gradlew runClient    # test in-game (needs Plasmo Voice in the run too)
```

Requires: Java 21, NeoForge 21.1.233, and **Plasmo Voice `neoforge-1.21-2.1.5`** installed
in the same instance (client and server).
