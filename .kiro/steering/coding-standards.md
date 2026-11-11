---
inclusion: fileMatch
fileMatchPattern: "**/*.kt"
---

# Kotlin coding standards

- Kotlin official code style, 4-space indent, max line length ~120.
- Prefer `val`, immutable data classes and sealed interfaces for state.
- No `!!` in production code. Handle nulls explicitly.
- No `println`, `printStackTrace` or `Log.d` spam. Use `Log.w/e` with the `TAG` constant only for
  genuine failures.
- No `TODO`/`FIXME` left in committed code; finish it or remove it.
- Coroutines: use `viewModelScope` in ViewModels and the app `CoroutineScope` from `AppContainer`
  for work that must outlive a screen. Blocking IO goes to `Dispatchers.IO`.
- Composables: stateless where possible (state hoisting), `Modifier` as the first optional
  parameter, previews are not required.
- User-facing strings come from `strings.xml` via `stringResource`. Content descriptions are
  mandatory on every icon-only control.
- Touch targets ≥ 48dp. Respect `reduceMotion` from settings for decorative animation.
- Canvas drawing code must not allocate in the draw loop when avoidable (hoist `Path`, `Brush`
  and `TextLayoutResult` into `remember`).
- Pure logic (DSP, physics, filtering, capsule rules) lives in `domain`/`audio.fx` without Android
  imports so it is unit-testable on the JVM.
