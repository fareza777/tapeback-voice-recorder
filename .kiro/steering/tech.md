---
inclusion: always
---

# Tech stack & build

| Area | Choice |
|---|---|
| Language | Kotlin 2.1.21, JVM target 17 |
| UI | Jetpack Compose (BOM 2025.06.01), Material 3, custom Canvas drawing |
| Audio capture | `MediaRecorder` (AAC/M4A) inside a `microphone` foreground service |
| Playback | Media3 ExoPlayer + `MediaSessionService` (background + media notification) |
| Effects | Custom Media3 `AudioProcessor` running a pure-Kotlin DSP chain |
| Persistence | Room 2.7 (KSP), DataStore Preferences |
| Scheduling | WorkManager (time-capsule unlock notifications) |
| DI | Manual constructor injection via `AppContainer` (no reflection, no Hilt) |
| Build | AGP 8.11.1, Gradle 8.14.3 wrapper, version catalog `gradle/libs.versions.toml` |
| SDK | minSdk 26, target/compileSdk 36 |

## Rules
- Never add a dependency that needs network access or declares `android.permission.INTERNET`.
  The manifest explicitly removes INTERNET with `tools:node="remove"`.
- No image-loading or HTTP libraries. Bitmaps are decoded locally with `BitmapFactory`.
- All dependency versions go through the version catalog.
- Release builds use R8 (`isMinifyEnabled = true`, `isShrinkResources = true`).
- Signing secrets live in `keystore.properties` (git-ignored). Never commit keys.

## Commands (PowerShell, from repo root)
- Debug APK: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Instrumented tests: `./gradlew connectedDebugAndroidTest`
- Release: `./gradlew assembleRelease bundleRelease`
- Quality gate used by hooks: `./scripts/kiro/quality-gate.ps1`
