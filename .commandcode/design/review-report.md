# Spoofer — Design Review

**Mode**: `/design review`
**Date**: 2026-06-04
**Register**: Product (Android app, operate surface)
**Inspected files**: 10 source files across ui/screen, ui/component, ui/theme

---

## Overall Score: 26 / 50

**Verdict**: Functional M3 implementation with no design authorship. The bones are solid but the surface lacks voice, the primary action is hidden, and interaction polish is missing.

## TL;DR

A competent Material 3 Android app for mock location spoofing. The map-first composition is correct for an operate surface, but the primary CTA (start/stop spoofing) is buried in a collapsed bottom sheet — the user must discover a gesture to find the most important action. The color palette is the default Google Maps blue, typography is completely unconfigured (`Typography()` with no overrides), and interaction states (loading, error, haptics) are missing. This is a working prototype, not a shipped product.

**Primary recommendation**: Surface the start/stop action and give this app a visual identity beyond the Maps clone palette.

---

## Heuristic Scores

| # | Heuristic | Score | Key Finding |
|---|-----------|-------|-------------|
| 1 | First impression | 6 / 10 | Competent M3 execution but no memorable point of view. Looks like any location app. |
| 2 | Hierarchy | 6 / 10 | Map-first is correct, but the primary action requires pulling up a bottom sheet to discover. |
| 3 | Color voice | 5 / 10 | Blue + green is the default location-app reflex. No brand distinction. |
| 4 | Type voice | 4 / 10 | Zero typography customization. ALL CAPS button text is tone-deaf. |
| 5 | Interaction feel | 5 / 10 | Core gestures work. Missing haptics, loading states, error recovery, dead buttons. |

---

## Cognitive Load / Risk

| Signal | Assessment |
|--------|------------|
| PASS | Map-first composition for operate surface |
| PASS | Dynamic color: adapts to device theme |
| PASS | AnimatedContent transitions between modes |
| PASS | Joystick drag gesture with magnitude/angle |
| PASS | Route preview polyline on map |
| PASS | Status chip with pulsing dot for live feedback |
| WATCH | Primary CTA hidden in collapsed bottom sheet (72dp peek) |
| WATCH | ALL CAPS button text reads as generated |
| WATCH | No loading indicators for async operations |
| WATCH | History list items have non-functional FilledTonalIconButtons |
| WATCH | Joystick invisible until spoofing starts — no preview mode |
| FAIL | Default blue primary is the smell-catalog generic tech hue |
| FAIL | `Typography()` with zero overrides — no type voice |
| FAIL | No haptic feedback anywhere in a tactile control app |
| FAIL | No error/recovery states for geocoding or route fetching |

---

## What's Working

- **Map as primary canvas** — Correct for an operate surface. The map is the object being manipulated, and overlays float above it.
- **Mode switching with animation** — The `AnimatedContent` slide transition between Static/Directions/Joystick panels is smooth and directionally aware.
- **Joystick implementation** — Drag gesture with clamped radius, angle calculation, crosshair lines, thumb glow, and specular highlight is well crafted.
- **Route progress tracking** — Linear progress indicator with remaining distance and recalculated ETA gives good temporal feedback during directions spoofing.
- **Favorites filtering** — Search-as-you-type filter on saved locations is responsive.
- **History screen layout** — LazyRow filter chips + card-based entry list with mode icons, duration, and distance is scannable.

---

## Priority Issues

### P0 — Primary action hidden in collapsed sheet

The start/stop spoofing button is inside the bottom sheet, which peeks at only 72dp. The user must discover they need to pull up the sheet to find the most important action in the app. For an operate surface (real-time location manipulation), the primary control must be visible without a gesture.

**Evidence**: `MapScreen.kt` line ~169: `sheetPeekHeight = 72.dp`. The `BottomSheetContent` composable contains the `Button` at its root level, pinned below the scrollable content, but still inside the sheet that starts collapsed.

**Fix**: Move the start/stop button to a `FloatingActionButton` on the map, or use `BottomAppBar` with a prominent FAB. The sheet should contain configuration only. The action should be always visible.

→ `/design relayout`

---

### P1 — No loading states

Route fetching (`fetchRoutePreview`), geocoding (`getFromLocationName`), and location loading are all silent operations. The user sees nothing happening and may tap multiple times, causing cascading requests.

**Evidence**: `MapScreen.kt` lines ~137-142: `LaunchedEffect` triggers `fetchRoutePreview` with no loading indicator. `LocationSearchBar.kt` lines ~76-88: geocoding runs on IO dispatcher with no visual feedback. No `CircularProgressIndicator` or shimmer anywhere in the app.

**Fix**: Add a `CircularProgressIndicator` in the route info card area while fetching. Show a loading dot in the search bar during geocoding. Consider a subtle map overlay shimmer.

→ `/design interaction`

---

### P1 — Joystick discovery gap

The joystick overlay only renders when `isActive = isSpoofing && spoofMode == SpoofMode.JOYSTICK`. The joystick panel text says "Drag the joystick on the map to move" but there's no joystick visible until the user presses START SPOOFING. The user can't preview or practice.

**Evidence**: `JoystickOverlay.kt` line ~38: `if (!isActive) return`. `MapScreen.kt` line ~122: `isJoystickActive = isSpoofing && spoofMode == SpoofMode.JOYSTICK`.

**Fix**: Show the joystick in a dimmed/inactive state when joystick mode is selected but spoofing hasn't started. Let the user move the thumb without affecting location — use it as a preview.

→ `/design interaction`

---

### P1 — Dead icon buttons in history

History cards use `FilledTonalIconButton(onClick = {})` as leading content. These look interactive (filled, tonal, button-shaped) but do nothing on tap. Users will tap them and get no response.

**Evidence**: `HistoryScreen.kt` line ~240: `FilledTonalIconButton(onClick = {}) { Icon(modeIcon, ...) }`.

**Fix**: Either make the icon button navigate to the map with that entry's location (it already wraps in a clickable card — so either remove the button and use a plain `Icon`, or give the button its own action like "replay this route").

→ `/design interaction`

---

### P2 — ALL CAPS button text

The primary button uses "START SPOOFING" and "STOP SPOOFING" in all caps. M3 `labelLarge` is already medium-weight at 14sp — adding all caps makes it shout. This is a tell from the smell catalog (generated UI reflex).

**Evidence**: `BottomSheetContent.kt` lines ~172-173: `Text(if (isSpoofing) "STOP SPOOFING" else "START SPOOFING", style = MaterialTheme.typography.labelLarge)`.

**Fix**: Change to sentence case: "Start spoofing" / "Stop spoofing". M3 buttons don't need all-caps — the weight and color already provide emphasis.

→ `/design typeset`

---

### P2 — Zero type customization

`Typography()` is declared with no overrides — no custom font, no adjusted body size, no line-height tuning, no letter-spacing. Android renders system-default Roboto with M3 defaults. This is the most common AI tell.

**Evidence**: `Type.kt` line 5: `val SpooferTypography = Typography()`.

**Fix**: At minimum, set `bodyLarge` line-height to 24sp for readable prose and add a custom `labelLarge` with letter-spacing for button text. Consider a distinctive display font for coordinate readouts.

→ `/design typeset`

---

### P2 — No haptic feedback

A spoofing app is tactile by nature — joystick dragging, start/stop commands, mode switching. Zero haptic feedback anywhere. On a device with a vibration motor, this is a missed opportunity for physical confirmation.

**Evidence**: No `HapticFeedback` or `performHapticFeedback` calls anywhere in the codebase. Joystick drag, button press, and mode switch are all silent.

**Fix**: Add light haptic on joystick grab, medium haptic on mode switch, and a confirmation vibration on start/stop.

→ `/design interaction`

---

### P2 — Generic color palette

The app uses Google Maps blue (`#1A73E8`) as primary with a green secondary. This is the default "location app" palette — indistinguishable from Maps, Waze, or any other geo tool. The app has no owned color identity.

**Evidence**: `Color.kt` line 6: `val SeedPrimary = Color(0xFF1A73E8)` — exactly Google Maps blue. `SeedSecondary = Color(0xFF5BB974)` — generic green.

**Fix**: Choose a hue with a reason tied to this specific product. Stealth dark green (military/spoofing metaphor), tactical amber/copper (precision instrument), or deep violet (privacy/obfuscation). Tint the neutral surfaces toward the chosen hue.

→ `/design recolor`

---

## Next Modes

| Mode | Priority | Target |
|------|----------|--------|
| `/design relayout` | P0 | Surface primary CTA — move start/stop out of collapsed sheet |
| `/design interaction` | P1 | Loading states, haptics, joystick preview, fix dead buttons |
| `/design recolor` | P2 | Build a distinctive palette beyond Maps blue |
| `/design typeset` | P2 | Customize typography, fix button casing, set body measure |

---

Generated with CommandCode — 2026-06-04
