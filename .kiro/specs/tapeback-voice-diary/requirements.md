# Requirements – TapeBack Retro Cassette Voice Diary

## Introduction

TapeBack turns an Android phone into a 1980s–1990s portable cassette recorder. Users record
voice notes onto virtual cassettes, play them back on an animated deck with real tape physics,
keep an audio diary, and seal time-capsule messages that unlock on a future date. The app is
fully offline: no INTERNET permission, no account, no backend.

Requirements use EARS notation (WHEN / WHILE / IF / THE SYSTEM SHALL).

---

## R1 – Recording

**User story:** As a diarist, I want to record my voice reliably, even with the screen off,
so that no thought is ever lost.

1. WHEN the user presses REC and microphone permission is granted, THE SYSTEM SHALL start
   recording AAC audio to app-private storage within 500 ms and latch the REC key down.
2. IF microphone permission is not granted, THEN THE SYSTEM SHALL request it, and IF the user
   permanently denied it, THE SYSTEM SHALL explain why and offer to open system settings.
3. WHILE recording, THE SYSTEM SHALL run a foreground service of type `microphone` with an
   ongoing notification showing elapsed time and Pause/Resume and Stop actions.
4. WHEN the user presses PAUSE while recording, THE SYSTEM SHALL pause capture without
   creating a new file, and WHEN PAUSE is pressed again, THE SYSTEM SHALL resume into the same file.
5. WHEN recording stops via the STOP key, the notification, or an engine error, THE SYSTEM SHALL
   finalize the file, measure its real duration, and save a library entry with a default title.
6. IF free storage is below 50 MB, THEN THE SYSTEM SHALL refuse to start recording and tell
   the user why.
7. WHILE recording, THE SYSTEM SHALL drive the VU meters and a live waveform from the measured
   input level and SHALL store a compact waveform with the recording.
8. THE SYSTEM SHALL offer Standard (44.1 kHz / 96 kbps), High (44.1 kHz / 128 kbps, default)
   and Studio (48 kHz / 192 kbps) quality presets.
9. IF a recording is shorter than 1 second, THEN THE SYSTEM SHALL discard it and say so.

## R2 – Cassette deck playback

**User story:** As a user, I want playback to feel like a real tape deck, so that listening
is nostalgic and fun.

1. WHEN a recording is loaded and PLAY is pressed, THE SYSTEM SHALL play it through Media3 and
   latch the PLAY key.
2. WHILE audio is moving, THE SYSTEM SHALL spin both reels with angular velocity ω = v / r,
   where r is the current tape-pack radius derived from playback progress, with motor inertia.
3. WHEN REW or FF is pressed, THE SYSTEM SHALL wind the tape at high speed with a motor sound
   until STOP, PLAY, or the tape end is reached, then continue from the wound position.
4. WHEN STOP is pressed during playback, THE SYSTEM SHALL stop at the current position; WHEN STOP
   is pressed while already stopped, THE SYSTEM SHALL eject the tape.
5. THE SYSTEM SHALL provide a waveform scrub bar for precise seeking and a mechanical
   3-digit tape counter plus an elapsed/total time readout.
6. WHILE playing in the background, THE SYSTEM SHALL keep playing with a media notification
   and lock-screen controls, and SHALL respect audio focus and headphone unplugging.
7. WHEN any transport key is pressed and mechanical sounds are enabled, THE SYSTEM SHALL play a
   synthesized mechanical click, and SHALL never capture that click inside a recording.

## R3 – Recording management

**User story:** As a user, I want to manage my recordings, so that my library stays tidy.

1. THE SYSTEM SHALL let the user rename, favorite, tag, move (cassette and side) and delete a
   recording, deleting its audio file with the database row.
2. WHEN the user chooses Share, THE SYSTEM SHALL share the audio file through a content URI
   without granting broader storage access.
3. WHEN the user chooses Save to device, THE SYSTEM SHALL let the user pick a destination
   through the system document picker and copy the audio there.
4. WHEN a recording finishes, THE SYSTEM SHALL offer a details sheet to edit title, tags,
   side and time-capsule sealing.

## R4 – Virtual cassette library

**User story:** As a collector, I want my recordings on custom cassettes, so that my memories
are organized like a shelf of mixtapes.

1. THE SYSTEM SHALL create a default cassette on first launch.
2. THE SYSTEM SHALL let the user create, edit and delete cassettes with title, category,
   label color, shell skin and optional cover artwork picked through the Photo Picker.
3. THE SYSTEM SHALL show each cassette with Side A and Side B track lists, counts and total length.
4. WHEN a cassette is deleted, THE SYSTEM SHALL ask for confirmation and delete its recordings and files.
5. WHEN the user chooses "Record on this side", THE SYSTEM SHALL make that cassette side the
   deck's recording target.
6. WHEN the user plays a side, THE SYSTEM SHALL queue that side's recordings in order.

## R5 – Audio diary

**User story:** As a diarist, I want to browse entries by date and find them quickly.

1. THE SYSTEM SHALL list recordings grouped by calendar day, newest first, labelled
   Today / Yesterday / full date.
2. THE SYSTEM SHALL filter by free-text search over titles, tags and cassette names,
   by favorites, and by tag.
3. THE SYSTEM SHALL show diary statistics: entry count, total recorded time and current
   daily streak.
4. WHEN recordings exist from the same day in a previous year, THE SYSTEM SHALL surface them
   as "On this day".
5. THE SYSTEM SHALL hide locked time capsules from the diary.

## R6 – Time capsule

**User story:** As a user, I want to record messages to my future self that cannot be played
until a chosen date.

1. THE SYSTEM SHALL let the user seal a recording with an unlock date in the future
   (quick picks: 1 month, 6 months, 1 year, 5 years, or a custom date) and an optional written note.
2. WHILE a capsule is locked, THE SYSTEM SHALL show it sealed with a live countdown and SHALL
   NOT play, share or export it.
3. WHEN the unlock time is reached, THE SYSTEM SHALL post a local notification, even after a
   device reboot, and SHALL make the capsule playable.
4. THE SYSTEM SHALL allow breaking a seal early only after an explicit confirmation.

## R7 – Audio effects

**User story:** As a listener, I want vintage sound colors and speed control.

1. THE SYSTEM SHALL apply real-time effect presets during playback: Clean, Vintage Cassette
   (wow & flutter, warmth, saturation), Lo-Fi (bit and sample-rate reduction), and AM Radio
   (band-limited, driven).
2. THE SYSTEM SHALL provide an adjustable tape-hiss level independent of the preset.
3. THE SYSTEM SHALL provide playback speed from 0.5× to 2.0×, with an option to link pitch
   to speed like a real tape motor.
4. THE SYSTEM SHALL drive the playback VU meters from the processed signal.
5. Effect settings SHALL persist across app restarts.

## R8 – Customization & visuals

1. THE SYSTEM SHALL offer at least five retro themes, each with light and dark variants, and a
   System / Light / Dark mode switch.
2. THE SYSTEM SHALL offer at least six cassette shell skins.
3. THE SYSTEM SHALL offer optional analog grain and scanline overlays.
4. THE SYSTEM SHALL provide a Reduce motion option that stops decorative motion.
5. THE SYSTEM SHALL adapt layout to compact and expanded widths (bottom bar vs navigation rail).

## R9 – Privacy & offline

1. THE SYSTEM SHALL NOT declare the INTERNET permission.
2. THE SYSTEM SHALL store recordings, covers, database and settings in app-private storage.
3. THE SYSTEM SHALL exclude user data from cloud backup while allowing device-to-device transfer.
4. THE SYSTEM SHALL require no account or login.

## R10 – First run, branding & quality

1. THE SYSTEM SHALL show a branded splash screen and a skippable onboarding that explains the
   app and requests microphone and notification permissions in context.
2. THE SYSTEM SHALL provide an adaptive launcher icon with a monochrome layer.
3. All icon-only controls SHALL have content descriptions; meters SHALL expose state descriptions.
4. Release builds SHALL be minified, resource-shrunk and signed, producing an APK and an AAB.
5. Pure logic SHALL be covered by JVM unit tests; persistence by instrumented tests.
