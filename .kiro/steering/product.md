---
inclusion: always
---

# Product: TapeBack – Retro Cassette Voice Diary

Publisher: **F7 Developer** · Hackathon category: **Kiroween – Resurrection**

TapeBack resurrects the 1980s–1990s cassette tape recorder as a modern, fully offline
voice recorder, audio diary and time capsule.

## Product pillars (in priority order)
1. **Recording you can trust** – recording never silently fails, survives the app going to
   the background, and every stop produces a saved, playable file.
2. **Playback that feels mechanical** – spinning reels whose speed follows real tape
   physics, piano-key transport, VU meters with real ballistics, mechanical sound effects.
3. **A library of cassettes** – recordings live on virtual cassettes with Side A/B,
   labels, shell skins, categories and cover artwork.
4. **Diary & time capsule** – browse by date, tags, favorites and search; seal messages
   until a future date and get a local notification when they unlock.
5. **Privacy by construction** – no INTERNET permission, no accounts, no analytics,
   no cloud backup. Everything stays in app-private storage.

## Language & audience
- The app ships as a single **global English** build. All user-facing copy is English and
  lives in `res/values/strings.xml`. Dates use English month names.
- Tone: warm, nostalgic, a little playful ("Press REC on your memories"), never cheesy.

## Non-goals
- No network features, sync, accounts, ads or in-app purchases.
- No speech-to-text (it would require cloud APIs or large models).
- No fake functionality: every visible control must do what it says.
