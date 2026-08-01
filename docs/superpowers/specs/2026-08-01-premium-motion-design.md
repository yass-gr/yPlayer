# yPlayer — Premium Motion & Design Pass (Approach B)

## Overview

A coordinated pass across motion, visual design, and performance that makes yPlayer feel
premium and instant. The centerpiece is a single **expandable Now Playing sheet** that
doubles as the mini-player and the full-screen player, driven by a custom gesture-based
motion system. The design language is **modern immersive** (Apple Music-style fluidity):
translucent materials, a rotating artwork disc, blurred album-art backdrop, and spring
motion throughout.

Design direction locked with the user:

- **Scope:** everything — premium motion, premium design, lightning-fast performance.
- **Language:** modern immersive (translucent materials, big bold artwork, dark-friendly,
  fluid spring motion).
- **Performance:** proactive polish — no specific reported pain point; make the app feel
  instant.
- **Now Playing:** mini-player + expandable full-screen sheet.

## Goals

- A persistent mini-player that springs up when a song is playing.
- One fluid collapse/expand motion between mini-player and full-screen Now Playing,
  draggable in both directions with velocity handoff and rubber-banding.
- Immersive full-screen Now Playing: rotating disc artwork, blurred artwork backdrop,
  translucent surfaces.
- Instant library browsing: artwork caching (Coil), reduced recomposition, spring nav
  transitions, staggered list entrances, press feedback.
- Respect `prefers-reduced-motion` and `prefers-reduced-transparency`.

## Non-Goals (this pass)

- Predictive-back gestures (defer to a follow-up).
- Queue panel UI (stub button only this pass).
- Drag-to-dismiss in detail screens.
- Any streaming/network features.

## Architecture Decision

The mini-player and full-screen Now Playing are **one component** (`NowPlayingSheet`) at a
shared, continuous progress value (`0 = collapsed`, `1 = expanded`). This is what makes the
collapse/expand and drag-to-collapse seamless: the same artwork literally scales and
translates; there is no screen swap.

### Navigation changes

- Remove the `nowPlaying` route from the `NavHost`.
- `NowPlayingSheet` lives in `MainScreen`'s `Scaffold`, layered above the bottom nav.
- The sheet observes `playbackController.state` directly. When `currentSong != null` it
  springs up as a mini-player. No navigation, no callback wiring.
- Delete the `onPlaybackStarted` parameter from `SongsScreen`, `SearchScreen`,
  `AlbumDetailScreen`, `ArtistDetailScreen`, `PlaylistDetailScreen`, and remove the
  `navController.navigate("nowPlaying")` call sites in `MainScreen`.
- The `NavigationBar` gets bottom padding added when the sheet is shown so tabs stay
  tappable above the mini-player.

## Motion system (`ui/motion/`)

### `MotionSpecs.kt`

Spring presets mapped from Apple's damping/response model:

- `Settle` — critically damped (`dampingRatio = 1f`), no bounce. Used for expand/collapse
  settle and idle transitions.
- `Momentum` — under-damped (`dampingRatio ≈ 0.8f`), slight bounce. Used **only** when a
  drag/flick carried velocity (bounce belongs on momentum, not on idle fades).
- Rotation is handled separately (frame-driven, not a spring).

### `SheetState.kt`

Custom state holder for the sheet. Progress is an `Animatable` over `0..1` implementing:

- **1:1 tracking** — while dragging, progress is set directly from finger movement via
  `pointerInput`/`awaitEachGesture`, respecting the grab offset.
- **Rubber-banding** at both bounds — overshoot past 0/1 is damped with the
  `rubberband()` formula (progressive resistance, no hard stop).
- **Velocity tracking** — a short history of moves feeds a `VelocityTracker`; release
  velocity captured in px/s.
- **Momentum projection** — `projected = release + (velocity/1000) * d / (1-d)` with
  `d ≈ 0.998` decides which anchor (0 or 1) to snap to.
- **Velocity handoff** — the spring's `initialVelocity` = release velocity, so a fast
  upward drag continues smoothly to expanded with no seam.
- **Interruptible** — every settle animation starts from the live on-screen progress and
  can be grabbed mid-flight; a re-drag cancels the spring and re-targets.
- Exposes `expand()`, `collapse()`, `isDragging`, and the anchored progress.

### `RotatingArtwork.kt`

- Rotation accumulates via `withFrameNanos` **only while `isPlaying`**; on pause it freezes
  instantly, on resume it continues from the current angle.
- Slight speed ramp on expand (slow drift at mini scale, faster full rotation when
  expanded).
- Rotation applied via `graphicsLayer` only (compositor-friendly).
- Disabled under `prefers-reduced-motion`.

### Motion helpers (pure functions, unit-tested)

- `rubberband(overshoot: Float, dimension: Float, constant: Float): Float`
- `projectMomentum(velocityPxPerSec: Float, decelerationRate: Float = 0.998f): Float`
- `chooseAnchor(progress: Float, projected: Float): Float` — target selection from release
  velocity + position.

## Now Playing sheet design

### Expanded state (progress = 1)

- **Backdrop:** full-screen blurred, saturated version of the album art behind everything
  (Coil loads once; blur + scrim via `graphicsLayer`). Dark gradient scrim at the bottom
  for text/controls legibility.
- **Artwork:** large circular disc with a subtle center hub ring. `graphicsLayer` rotation
  only.
- **Header:** drag handle + collapsed-sheet peek (title slides up, artwork shrinks) as the
  user drags down — in-between frames telegraph where it's going.
- **Track info:** title (large, negative tracking, tight leading), then artist.
- **Seek bar:** slider with current/total time; draggable, updates 1:1 while scrubbing.
- **Transports:** previous / play-pause / next centered and oversized; shuffle and repeat
  flank them, active state tinted `primary`.
- **Queue icon** bottom-right — stub button this pass (no panel yet).

### Collapsed state (progress = 0)

- Translucent bar (Material surface at reduced opacity), 48dp artwork, one-line title +
  artist, play/pause button.
- Backdrop blur bleeds subtly through the bar's top edge.
- Tapping anywhere on the bar expands the sheet (`Settle` spec).

### Feedback rules

- Transport buttons scale down on press (`interactionSource` → press scale ~100ms) —
  feedback on pointer-down, not release.
- Playing/paused flips the play icon with a small crossfade + scale.

## Design tokens (`ui/theme/`)

- **`Type.kt`** — real type ramp. Display sizes get negative tracking (e.g. `-0.02em` at
  34–57sp) and tight leading; body stays near `0` tracking with comfortable leading.
  Headings lean on weight, not just size.
- **`Theme.kt`** — keep Material You dynamic color; add a custom shape system (more
  rounded art/buttons) and translucent surface tones so the mini-player and sheet read as
  material layers over content.

## Performance

- **Coil for artwork** — replaces the per-row `produceState`/`loadThumbnail` path in
  `Artwork.kt`. Scrolling lists stop re-decoding thumbnails every recomposition; sheet
  backdrop and mini-art share one cached bitmap; built-in crossfade.
- **Reduced recomposition** — seek slider/time text read position through
  `derivedStateOf` and re-render only on visible change; rows/cards stay `stable` with
  stable keys.
- **`prefers-reduced-motion`** → sheet auto-collapses with a static cross-fade, artwork
  rotation stops, nav/list transitions become fades.
- **`prefers-reduced-transparency`** → mini-player bar goes solid, no blur.
- Only new dependency: **Coil**.

## Navigation & list transitions

- **Nav transitions** on the `NavHost`: fade + 12dp vertical slide on enter/exit,
  spring-based (~250ms, `Settle`). Detail screens slide up, back pops down — symmetric
  paths.
- **List entrance** in `LazyColumn`s: staggered fade + 8dp slide on first composition;
  disabled under reduced-motion.
- **Press feedback** on list rows and album cards: scale-to-0.97 on press, spring back on
  release.

## Error handling

- Sheet handles `currentSong == null` (player idle) by collapsing to nothing — no sheet
  rendered.
- Artwork load failure falls back to the existing `MusicNote` placeholder.
- Existing playback error handling (skip-to-next on missing file) is unchanged.

## Testing

- **Unit:** `rubberband()`, `projectMomentum()`, `chooseAnchor()`. `SheetState` gets an
  interruptibility test (re-drag mid-settle cancels the spring) using the Compose test
  clock.
- **UI:** update `SongPlaybackTest` for the new flow — tap song → mini-player appears;
  tap mini-player → sheet expands. Navigation-to-`nowPlaying` assertions are removed.
- **Regression:** existing library/playlist tests pass; `./gradlew test` and `lint` green
  before finishing.

## Follow-up (deferred, noted only)

- Predictive-back gestures.
- Queue panel UI (the stub button hooks into this later).
- Drag-to-dismiss in detail screens.
