# Demo guide

## Seed demo content

The debug build can be filled with realistic cassettes, diary entries and capsules. The audio is
synthesized speech-like material, so waveforms and VU meters move. Nothing is downloaded.

```powershell
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w -e class com.f7developer.tapeback.DemoContentSeeder -e seed true `
    com.f7developer.tapeback.debug.test/androidx.test.runner.AndroidJUnitRunner
```

The seeder adds content each time it runs. Clear app data first
(`adb shell pm clear com.f7developer.tapeback.debug`) to avoid duplicate cassettes.

## Demo script (about 3 minutes)

1. **First run (20 s)** – launch, show the splash and swipe through onboarding; grant the
   microphone and notification permissions in context.
2. **Record (40 s)** – press REC. The key latches, reels spin and the VU needles follow your
   voice. Press PAUSE, then PAUSE again to resume into the same file. Turn the screen off for a
   few seconds to show recording continues with its notification. Press STOP and fill in the
   details sheet (title, tags).
3. **Play (40 s)** – press PLAY. Point out that the reels change speed as the tape moves from one
   reel to the other. Open **Sound & speed**, switch to Vintage Cassette, add hiss, try 1.5×.
   Press FF to hear the motor wind, then STOP twice to eject.
4. **Library (25 s)** – open a cassette, show Side A/B, edit the shell skin and label color,
   use "Record on this side".
5. **Diary (25 s)** – search, filter by a tag, show the streak and "On this day".
6. **Time capsule (25 s)** – seal a recording for one year with a note; show the countdown and
   that it cannot be played.
7. **Settings (15 s)** – switch to the Kiroween theme, toggle dark mode, grain and scanlines.
8. **Privacy (10 s)** – show the app info page: no network permission.

## Store listing draft

- **Title:** TapeBack – Cassette Voice Diary
- **Short description:** A retro cassette recorder for your voice diary. Offline and private.
- **Full description:**
  Press REC on your memories. TapeBack is a voice recorder and audio diary styled as a classic
  portable cassette deck, with spinning reels, needle VU meters and piano-key controls.
  Record onto virtual cassettes with Side A and B, add vintage tape effects on playback, browse
  your diary by date and tag, and seal messages that open on a future date.
  TapeBack works fully offline. It has no internet permission, no account, no ads and no
  analytics. Your recordings stay on your phone.
- **Category:** Music & Audio
- **Screenshots:** `docs/screenshots/`
