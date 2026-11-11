# Design – TapeBack Retro Cassette Voice Diary

## 1. Architecture overview

```
┌──────────────────────────── ui (Compose, MVVM) ────────────────────────────┐
│ DeckScreen  LibraryScreen  CassetteScreen  DiaryScreen  CapsuleScreen  ... │
│     │ ViewModels (StateFlow<UiState>)                                      │
└─────┼──────────────────────────────────────────────────────────────────────┘
      │                       app-scoped singletons (AppContainer)
┌─────▼───────────────┐  ┌────────────────────┐  ┌─────────────────────────┐
│ RecordingController │  │ PlayerConnection   │  │ Repositories            │
│  ├ RecorderEngine   │  │  └ MediaController │  │  Cassette / Recording / │
│  └ RecordingService │  │      ↕ session     │  │  Settings (DataStore)   │
│    (FGS microphone) │  │ PlaybackService    │  └───────────┬─────────────┘
└─────────────────────┘  │  ExoPlayer + Tape- │              │
                         │  AudioProcessor    │        Room (tapeback.db)
                         └────────────────────┘
      domain/ : models + pure policies (ReelPhysics, VuBallistics, CapsulePolicy,
                DiaryLogic, WaveformCodec) – no Android imports, JVM-tested
```

Manual DI: `TapeBackApp` creates one `AppContainer` holding the database, repositories,
`RecordingController`, `PlayerConnection`, `CapsuleScheduler`, `MechanicalSounds`, and an
application `CoroutineScope(SupervisorJob() + Dispatchers.Default)`.

## 2. Data model (Room, `tapeback.db` v1)

| Table | Columns |
|---|---|
| `cassettes` | `id` PK, `title`, `category`, `labelColor` (ARGB Int), `skin` (enum name), `coverPath?`, `createdAt`, `updatedAt` |
| `recordings` | `id` PK, `cassetteId` FK→cassettes (CASCADE), `side` (A/B), `title`, `filePath`, `durationMs`, `sizeBytes`, `createdAt`, `favorite`, `waveform` BLOB (≤ 200 bytes), `capsuleUnlockAt?`, `capsuleNote?`, `capsuleNotified` |
| `recording_tags` | (`recordingId` FK CASCADE, `tag`) composite PK, index on `tag` |

`CassetteWithStats` is a projection with side A/B counts and total duration computed by
correlated sub-queries. `RecordingWithTags` uses `@Relation` with `projection = ["tag"]`.

Files: `filesDir/recordings/<uuid>.m4a`, `filesDir/covers/<uuid>.jpg` (downscaled to 1024 px).
Repository delete paths remove the files after the row is deleted.

## 3. Recording pipeline

State machine (`RecorderState`): `Idle → Preparing → Recording(paused=false|true) → Idle`,
plus a one-shot `RecorderEvent` stream (`Saved(id)`, `Discarded`, `Error(message)`).

1. `RecordingController.start(target)` checks permission and free space, plays the REC click,
   then starts `RecordingService` with `ACTION_START`.
2. The service calls `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)`
   and then `controller.beginCapture()`; this ordering satisfies Android 14 FGS rules.
3. `RecorderEngine` wraps `MediaRecorder` (MIC source, MPEG_4/AAC). A 20 Hz ticker reads
   `maxAmplitude` once per tick, feeding `level` (VU) and `WaveformAccumulator`.
4. Elapsed time is computed from `SystemClock.elapsedRealtime()` minus paused spans.
5. `stop()` → engine stop → `MediaMetadataRetriever` duration (fallback: elapsed) →
   repository insert with default title `"Entry · MMM d, h:mm a"` → `Saved(id)` → service stops.
6. `MediaRecorder.OnErrorListener` and service `onDestroy` route to the same `stop()` path.

Notification actions (`PAUSE`, `RESUME`, `STOP`) are explicit intents to the service.

## 4. Playback pipeline

`PlaybackService : MediaSessionService` builds ExoPlayer with a `DefaultRenderersFactory`
override of `buildAudioSink` that installs `TapeAudioProcessor` into `DefaultAudioSink`.
Audio attributes `USAGE_MEDIA / CONTENT_TYPE_SPEECH`, `handleAudioFocus = true`,
`setHandleAudioBecomingNoisy(true)`.

`PlayerConnection` (app scope) connects a `MediaController` lazily and exposes
`StateFlow<PlayerSnapshot>` (loaded queue, current id, isPlaying, position, duration, speed).
A 30 fps position ticker runs only while playing. Media items carry `mediaId = recordingId`,
a file URI, title, cassette name and optional cover artwork bytes.

### Deck winding (REW/FF)
Winding is simulated in `DeckViewModel`: the player pauses, a virtual position moves at 12×
real time per frame, the reels spin at winding speed, and the motor sound loops. On STOP, PLAY
or reaching an end, the player seeks once to the virtual position (avoids seek storms).

## 5. Effects DSP (`audio/fx`)

`TapeEffectChain` (pure Kotlin) processes interleaved 16-bit PCM per frame:

```
in → HPF → [preset section] → wow/flutter delay → saturation → + hiss → LPF → meter → out
```

| Preset | Processing |
|---|---|
| Clean | bypass (meter only) |
| Vintage | HPF 45 Hz, low-shelf-like peak +2.5 dB @ 140 Hz, LPF 9.5 kHz, wow 0.55 Hz + flutter 6.3 Hz (≈ 2.2 ms depth), drive 1.6 |
| Lo-Fi | sample-and-hold to ≈ 11 kHz, 8-bit quantization, LPF 4.2 kHz, drive 1.3 |
| AM Radio | HPF 380 Hz, LPF 3.2 kHz, peak +5 dB @ 1.6 kHz, drive 2.4, light static |

- Biquads use the RBJ cookbook; per-channel state arrays sized at configure time.
- Wow/flutter uses a per-channel circular delay line with linear interpolation.
- Hiss: xorshift white noise through a one-pole high-pass, gain = hissLevel × 0.035.
- Saturation: `tanh(drive·x)/tanh(drive)`; final hard limit to [-1, 1].
- Parameters are an immutable `FxParams` swapped through a `@Volatile` field; the chain
  rebuilds coefficients when it sees a new instance (no locks, no allocation per sample).
- Levels: per-buffer peak and RMS per channel published to `AudioLevelBus`.

Speed/pitch: `PlaybackParameters(speed, if (linkPitch) speed else 1f)` – Sonic runs after
our processor inside `DefaultAudioSink`.

## 6. Physics & ballistics (domain/policy)

- **ReelPhysics:** hub radius h, full radius R. Tape area conserves:
  `r_supply(p) = √(h² + (1−p)(R²−h²))`, `r_takeup(p) = √(h² + p(R²−h²))`.
  Angular velocity `ω = v / r`. Tape counter = take-up turns `(r_takeup − h)/t`, which is
  non-linear exactly like a real deck counter.
- **Motor inertia:** speed approaches target with `s += (target − s)·(1 − e^(−dt/τ))`, τ = 0.18 s.
- **VuBallistics:** mass-spring-damper, ω₀ = 2π·2.1 Hz, ζ = 0.62 (≈300 ms rise, slight
  overshoot like an IEC VU meter). Peak LED holds for 1 s above −1 VU.
- **WaveformCodec:** amplitudes → dB-scaled 0..255 bytes, resampled to ≤ 200 bins.

## 7. Time capsule

- `CapsulePolicy.isLocked(unlockAt, now)`, `countdown(now)` → days/hours/minutes/seconds.
- Sealing writes `capsuleUnlockAt` (09:00 local on the chosen date), `capsuleNote`,
  `capsuleNotified = false` and enqueues unique WorkManager work `capsule-<id>` with
  `initialDelay = unlockAt − now`. WorkManager persists across reboots.
- `CapsuleUnlockWorker` posts a high-importance notification (channel `capsules`) with a
  deep link to the Capsules tab and marks `capsuleNotified = true`.
- On app start `CapsuleScheduler.resync()` re-enqueues any pending capsule (defensive).
- Player, share and export guards check `CapsulePolicy` before acting.

## 8. UI design

Navigation: `onboarding` → `main` (tabs Deck / Library / Diary / Capsules) → `cassette/{id}`,
`settings`. Bottom `NavigationBar` on compact width, `NavigationRail` ≥ 600 dp.

Key components (`ui/components`):

| Component | Notes |
|---|---|
| `CassetteArt` | Canvas cassette: shell gradient per skin, 5 screws, label with handwritten title, smoked window, tape packs sized by `ReelPhysics`, rotating 6-tooth hubs, head area, gloss |
| `TransportKeys` | Piano keys with 3D travel; latching for PLAY/REC/PAUSE/REW/FF; haptics + click |
| `VuMeter` | Cream face, red zone, needle on spring physics, peak LED, `stateDescription` |
| `TapeCounter` | Rolling mechanical digits + VT323 LCD time |
| `WaveformBar` | Stored waveform with progress fill; drag to seek; live mode during recording |
| `AnalogOverlay` | Tiled noise shader with jitter + scanlines, pointer-transparent |

Themes: `RetroThemeId` × {light, dark} → `RetroPalette` → Material 3 `ColorScheme`, exposed
through `LocalRetroPalette`. Fonts bundled in `res/font` (OFL / Apache 2.0).

## 9. Error handling

| Failure | Behaviour |
|---|---|
| Mic permission denied | Rationale dialog → system settings |
| `MediaRecorder` prepare/start throws | State back to Idle, file deleted, error snackbar |
| Engine error during capture | Auto-stop, keep what was captured, snackbar |
| Low storage | Refuse start, snackbar with free space |
| Audio file missing on play | Snackbar "Tape is damaged", offer delete |
| Locked capsule play attempt | Refuse, show countdown |

## 10. Testing strategy

- **JVM:** `TapeEffectChainTest`, `BiquadTest`, `ReelPhysicsTest`, `VuBallisticsTest`,
  `CapsulePolicyTest`, `DiaryLogicTest`, `WaveformCodecTest`, `TimeFormatTest`, `WavWriterTest`,
  `ToneSynthTest`.
- **Instrumented:** `TapeDatabaseTest` (stats projection, cascade delete, tags),
  `TransportKeysSemanticsTest` (content descriptions, selected state).
- **Device QA via ADB:** install on a Redmi Note 11 (Android 13) and an API 35 emulator,
  `uiautomator dump` to check semantics, `screencap` for screenshots, `dumpsys media_session`
  and `dumpsys activity services` to verify services.
