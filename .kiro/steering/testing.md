---
inclusion: fileMatch
fileMatchPattern: "**/{test,androidTest}/**"
---

# Testing conventions

- JVM unit tests live in `app/src/test` and cover all pure logic: DSP chain, reel physics,
  capsule policy, diary filtering/grouping, waveform encoding, formatters, WAV writer and
  tone synthesis.
- Instrumented tests live in `app/src/androidTest`: Room DAO behaviour (stats queries, cascade
  deletes, tags) and Compose accessibility semantics of the transport keys.
- Test names use backticks and describe behaviour: `` `locked capsule cannot be played before unlock` ``.
- No sleeping in tests. Inject `Clock`/time values instead of reading `System.currentTimeMillis()`.
- Every bug fixed during ADB device testing gets a regression test when the logic is testable.
- The Stop hook runs `./scripts/kiro/quality-gate.ps1 -Fast` (unit tests); run the full gate
  (`-Full`: unit tests + lint + assemble) before every push.
