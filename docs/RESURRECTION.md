# TapeBack and the Kiroween Resurrection brief

Official category definition (Kiroween 2026 Official Rules, Project Requirements):

> Resurrection: Bring your favorite dead technology back to life. Reimagine
> obsolete tech with today's innovations or solving tomorrow's problems.

TapeBack resurrects the portable cassette recorder — a 1980s–1990s consumer
object that is no longer manufactured for everyday voice notes — as a fully
offline Android voice diary. The hardware is gone; the behaviours people
remember are not.

## What is dead, and what comes back

| Dead technology | How TapeBack reimagines it today |
|---|---|
| Spinning reels that slow down and speed up as tape packs from one hub to the other | Angular velocity ω = v / r from the current tape-pack radius, with motor inertia (`domain/policy/ReelPhysics.kt`) |
| Needle VU meters with real ballistics | Spring-damper needle motion, ~300 ms integration and a 1 s peak LED (`VuBallistics.kt`), driven by the live input or output level — not a decoration |
| Chunky piano-key transport that latches | REC, PLAY, REW, FF, STOP/EJECT and PAUSE latch like a mechanical deck; STOP while stopped ejects |
| The sound of tape | Real-time DSP in a Media3 `AudioProcessor`: Vintage Cassette (wow and flutter, warmth, saturation), Lo-Fi, AM Radio, adjustable hiss, 0.5×–2× speed with optional tape-style pitch |
| Mechanical clicks and motor whirr | Synthesized in code (no audio assets), rendered once to WAV and played through `SoundPool`; never captured in a recording |
| A shelf of mixtapes | Virtual cassettes with Side A/B, hand-written style labels, seven shell skins, categories and cover art |

## The modern half (today's innovations, tomorrow's problems)

The cassette metaphor is not a skin over a generic recorder. The modern half
is what makes the idea trustworthy on a phone:

- AAC recording in a `microphone` foreground service that survives the screen
  turning off, with a notification (Pause / Stop).
- Background playback with a Media3 media notification, audio focus and
  headphone-unplug handling.
- A searchable diary with streaks and "On this day".
- Time capsules sealed until a future date, unlocked by WorkManager
  notifications that survive reboots.
- Privacy by construction: no `INTERNET` permission (removed in the manifest
  with `tools:node="remove"`, and a Kiro hook fails the check if it comes
  back), no account, no analytics, no cloud backup.

## Why this is Resurrection and not Costume Contest

The UI is deliberately retro, but the category fit is the revived mechanism:
reel physics, latching transport, VU ballistics, tape DSP and mixtape
library. Costume Contest would be the right call only if the work was
primarily a haunted visual theme on an otherwise ordinary app. TapeBack's
claim is the dead machine, brought back as a private voice diary.
