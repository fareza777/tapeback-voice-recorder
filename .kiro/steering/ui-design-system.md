---
inclusion: fileMatch
fileMatchPattern: "**/ui/**"
---

# Retro UI design system

## Visual language
- Physical hardware first: brushed panels, plastic piano keys, cream VU faces, smoked tape
  windows, Phillips screws. Use gradients and soft shadows to suggest depth, never flat grey boxes.
- Typography: **Bungee** for display/brand, **Space Grotesk** for UI text, **VT323** for LCD
  readouts and counters, **Permanent Marker** for handwritten cassette labels.
- Themes (`RetroThemeId`): Amber Walkman (default), Neon Mixtape, Chrome Hi-Fi, Kiroween,
  Beige Classic. Each defines light and dark `RetroPalette` values; Material 3 color schemes are
  derived from the palette so stock components match the hardware.

## Motion
- Reels spin with physically correct angular velocity (ω = v / r) and motor inertia.
- Piano keys latch down for PLAY / REC / PAUSE / FF / REW like a real deck.
- VU needles use spring-damper ballistics (≈300 ms VU integration); peak LED holds 1 s.
- When **Reduce motion** is on (or system animations are disabled), decorative motion
  (grain jitter, reel spin, needle overshoot) is reduced or stopped; state is still shown.

## Layout
- Compact width: bottom `NavigationBar`. Width ≥ 600dp: `NavigationRail` and two-pane deck.
- Every icon-only button has a content description; meters expose a `stateDescription`.
- Contrast: text on palettes must meet WCAG AA (4.5:1) for body text.
