---
inclusion: always
---

# Project structure & architecture

Single `:app` module, layered by package (Clean Architecture + MVVM):

```
com.f7developer.tapeback
├── TapeBackApp / MainActivity      – entry points, AppContainer creation
├── di/                             – AppContainer (manual DI graph)
├── core/                           – framework-light utilities (format, files, sound synth)
├── domain/                         – pure Kotlin models + policies (no Android imports)
│   ├── model/                      – Cassette, Recording, enums
│   └── policy/                     – CapsulePolicy, DiaryFilter, ReelPhysics, Waveform
├── data/                           – Room DB, DAOs, DataStore, repositories
├── audio/
│   ├── record/                     – RecorderEngine, RecordingController, RecordingService
│   ├── play/                       – PlaybackService, PlayerConnection
│   ├── fx/                         – DSP chain (pure Kotlin) + Media3 AudioProcessor adapter
│   └── export/                     – share & export to user-chosen location
├── capsule/                        – WorkManager scheduler + unlock worker
└── ui/
    ├── theme/                      – RetroPalette, Material 3 theme, typography
    ├── components/                 – CassetteArt, VuMeter, TransportKeys, Waveform, overlays
    └── <feature>/                  – Screen + ViewModel per feature (deck, library, diary,
                                      capsule, settings, onboarding, recording)
```

## Dependency direction
`ui → domain ← data`, `ui → audio → data`. `domain` never imports Android, Room or Compose.
ViewModels receive repositories/controllers through constructor parameters created by
`AppContainer` and a `viewModelFactory { }` initializer.

## State
- Screens expose one immutable `UiState` via `StateFlow` built with `stateIn(WhileSubscribed(5_000))`.
- One-off UI events (snackbars, navigation) use `Channel`/`SharedFlow`, never sticky state.
- Long-lived audio state (recorder, player) lives in app-scoped singletons, not in ViewModels,
  so it survives configuration changes and background services.
