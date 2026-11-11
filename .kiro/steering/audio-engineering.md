---
inclusion: fileMatch
fileMatchPattern: "**/audio/**"
---

# Audio engineering guidelines

## Recording
- Output: AAC-LC in MPEG-4 (`.m4a`), mono. Quality presets: Standard 44.1 kHz/96 kbps,
  High 44.1 kHz/128 kbps (default), Studio 48 kHz/192 kbps.
- The foreground service (`foregroundServiceType="microphone"`) must call `startForeground`
  **before** `MediaRecorder.start()`. Recording is only started while the UI is visible.
- `MediaRecorder.getMaxAmplitude()` is polled by exactly one ticker (it resets on read) at 20 Hz;
  the value feeds both the VU meter and the stored waveform.
- Every stop path (UI key, notification action, error listener, service teardown) goes through
  `RecordingController.stop()` which finalizes the file and inserts the Room row.
- Mechanical click sounds must never be captured: play the click *before* starting/resuming and
  *after* pausing/stopping.

## Playback
- Media3 ExoPlayer lives only in `PlaybackService`. UI talks to it through `MediaController`
  wrapped by `PlayerConnection`.
- Audio focus and becoming-noisy are handled by ExoPlayer (`handleAudioFocus = true`).
- Locked time capsules are never handed to the player.

## Effects DSP
- Processing is 16-bit PCM, interleaved, per-channel filter state.
- `TapeEffectChain` is pure Kotlin (no Android) and must be real-time safe: no allocation in
  `process`, no locks; parameters are swapped through a `@Volatile` reference.
- Output must stay within [-1, 1] before conversion (soft clip with `tanh`-style saturation).
- Levels (peak + RMS) are measured after the effect so meters show what the user hears.
