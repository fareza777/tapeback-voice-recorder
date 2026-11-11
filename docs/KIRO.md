# How Kiro built TapeBack

TapeBack was developed spec-first with Kiro. Everything Kiro used lives in `.kiro/` and
`scripts/kiro/`, so the process is reproducible from the repository.

## 1. Spec: requirements → design → tasks

`.kiro/specs/tapeback-voice-diary/`

| File | Contents |
|---|---|
| `requirements.md` | Ten requirement groups (R1 Recording … R10 First run & quality) written in EARS notation, e.g. "WHEN the user presses PAUSE while recording, THE SYSTEM SHALL pause capture without creating a new file". |
| `design.md` | Architecture, Room schema, recording and playback pipelines, DSP chain, reel physics and VU ballistics formulas, capsule scheduling, error handling and test strategy. |
| `tasks.md` | 28 tasks in six milestones (M1 Foundation … M6 Quality & release). Each task cites the requirements it satisfies, so every requirement traces to code. |

Each milestone ended with an installable APK checked on a phone over ADB.

## 2. Steering

`.kiro/steering/` keeps project rules in front of the agent. Files load only when relevant:

| File | Inclusion | What it enforces |
|---|---|---|
| `product.md` | always | Product pillars, English-only copy, tone, non-goals (no network, no speech-to-text, no fake controls) |
| `tech.md` | always | Stack and versions, version catalog, no INTERNET dependencies, R8 release, build commands |
| `structure.md` | always | Package layout, dependency direction (`domain` has no Android imports), `UiState` + `StateFlow` pattern |
| `coding-standards.md` | `**/*.kt` | No `!!`, no log spam, no TODOs, coroutine scopes, no allocation in draw loops |
| `audio-engineering.md` | `**/audio/**` | `startForeground` before `MediaRecorder.start()`, single amplitude ticker, real-time-safe DSP, clicks never recorded |
| `ui-design-system.md` | `**/ui/**` | Hardware visual language, fonts, themes, reel/needle motion rules, Reduce motion, accessibility |
| `testing.md` | test folders | What JVM vs instrumented tests cover, naming, no sleeps, regression test per device bug |

## 3. Hooks

`.kiro/hooks/*.json` call PowerShell scripts in `scripts/kiro/`:

| Hook | Trigger | Script | Effect |
|---|---|---|---|
| Spec progress at session start | `SessionStart` | `spec-status.ps1` | Injects task progress, the next open tasks and git state, so a new session knows where to resume |
| Kotlin standards guard | `PostFileSave` on `.kt` | `kotlin-guard.ps1` | Flags `!!`, `println`, `printStackTrace`, `Log.d`, TODO/FIXME and `GlobalScope` |
| Offline privacy guard | `PostFileSave` on manifest, Gradle, catalog, `.kt` | `privacy-guard.ps1` | Fails if INTERNET stops being removed or networking/analytics APIs appear |
| String resources guard | `PostFileSave` on `strings.xml`, `.kt` | `strings-guard.ps1` | Validates `strings.xml` and that every `R.string`/`R.plurals` reference resolves |
| Confirm destructive commands | `PreToolUse` on shell tools | `git-safety.ps1` | Asks the user before force pushes, hard resets, `git clean`, branch deletion or recursive deletes of source |
| Unit tests when the agent stops | `Stop` | `quality-gate.ps1 -Fast` | Static guards + JVM unit tests after every agent turn |
| Full gate after each spec task | `PostTaskExec` | `quality-gate.ps1 -Full` | Guards, unit tests, Android lint and a debug assemble |

## 4. Device QA loop

`scripts/qa/` lets the agent drive a phone over ADB: `tap.ps1` taps elements by text or
content description via `uiautomator`, `shot.ps1` and `capture.ps1` save screenshots with
half-size previews into the git-ignored `qa/` folder. Findings fed back into fixes, for example:

- An ANR on the emulator: the main thread waited on a render thread stuck in GPU shader
  compilation, while the app redrew the whole screen even when idle (the grain overlay jittered
  every 90 ms). The grain now animates only while tape is moving, and animated canvases draw in
  their own layers. An idle deck now renders zero frames (checked with `dumpsys gfxinfo` on a
  phone).
- Speed chips showed `1.3×` for 1.25× because of `%.2g` formatting; labels are now exact.
- The `-20` and `+3` VU scale labels overflowed the meter face; they are now clamped inside it.
