# Implementation Plan – TapeBack

Milestones each end with an installable APK verified on a device over ADB.

## M1 – Foundation
- [x] 1.1 Gradle project, version catalog, wrapper, manifest without INTERNET _(R9.1, R10.4)_
- [x] 1.2 Kiro steering, hooks and quality-gate scripts _(process)_
- [x] 1.3 Domain models and pure policies: ReelPhysics, VuBallistics, CapsulePolicy, DiaryLogic, WaveformCodec, formatters _(R2.2, R5, R6)_
- [x] 1.4 Room database, DAOs, repositories, DataStore settings, default cassette _(R3, R4.1, R7.5)_
- [x] 1.5 AppContainer manual DI and Application class

## M2 – Record & play (first functional APK)
- [x] 2.1 RecorderEngine + RecordingController + microphone foreground service with notification actions _(R1)_
- [x] 2.2 TapeEffectChain DSP + Media3 TapeAudioProcessor + AudioLevelBus _(R7)_
- [x] 2.3 PlaybackService (MediaSessionService) + PlayerConnection _(R2.1, R2.6)_
- [x] 2.4 Mechanical sound synthesizer (WAV generation + SoundPool) _(R2.7)_

## M3 – Retro deck UI
- [x] 3.1 RetroPalette themes, typography with bundled fonts _(R8.1)_
- [x] 3.2 CassetteArt with skins, reel physics and label _(R2.2, R8.2)_
- [x] 3.3 TransportKeys, VuMeter, TapeCounter, WaveformBar, AnalogOverlay _(R2.3–R2.5, R8.3)_
- [x] 3.4 DeckScreen + DeckViewModel incl. winding, eject, target cassette picker, effects sheet _(R2, R7)_
- [x] 3.5 Post-recording details sheet _(R3.4)_

## M4 – Library, diary, capsules
- [x] 4.1 Library grid + cassette editor (skin, label color, category, cover via Photo Picker) _(R4)_
- [x] 4.2 Cassette detail with Side A/B, play side, record on side _(R4.3, R4.5, R4.6)_
- [x] 4.3 Recording actions: rename, favorite, tags, move, share, save to device, delete _(R3)_
- [x] 4.4 Diary: grouping, search, filters, stats, streak, On this day _(R5)_
- [x] 4.5 Time capsule: seal flow, countdown, WorkManager unlock notification, break seal _(R6)_

## M5 – First-run, settings, polish
- [x] 5.1 Splash screen (animated icon on API 31+), adaptive + monochrome launcher icon _(R10.1, R10.2)_
- [x] 5.2 Onboarding pager with in-context permissions _(R10.1)_
- [x] 5.3 Settings: themes, mode, overlays, reduce motion, sounds, haptics, quality, storage, about _(R8)_
- [x] 5.4 Adaptive layout (NavigationRail ≥ 600dp, two-pane deck) and accessibility pass _(R8.5, R10.3)_

## M6 – Quality & release
- [x] 6.1 JVM unit tests for all policies and DSP _(R10.5)_
- [x] 6.2 Instrumented Room + Compose semantics tests _(R10.5)_
- [ ] 6.3 ADB device QA on real phone + emulator; fix findings
- [x] 6.4 Release signing, R8, APK + AAB _(R10.4)_
- [ ] 6.5 Screenshots, store assets, README with Kiroween Resurrection section, Kiro usage + demo docs
