# yPlayer — Minimal, Fast Android Music Player

## Overview

A minimal, fast local music player for Android. It browses the music already on the
device (via MediaStore), plays it with background support, and lets the user organize
it into playlists. No streaming, no accounts, no downloads — just the user's own files,
rendered fast.

## Goals

- Minimal: a focused feature set, nothing extraneous.
- Fast: snappy library browsing and instant playback on mid-range hardware.
- Local-only: reads music from device storage; no network features.
- Background playback with a media notification.

## Non-Goals

- Sleep timer, equalizer, lyrics, gapless playback, folder browsing, tag editing.
- Network/streaming features, downloads, Chromecast, scrobbling.
- Tablet-specific layouts (works on tablets, not optimized for them).

## Decisions (locked with user)

| Topic | Decision |
|---|---|
| Stack | Kotlin + Jetpack Compose + Media3 (ExoPlayer) |
| Architecture | Single `app` module, MVVM + repositories |
| Music source | Whole device via MediaStore |
| Navigation | Bottom nav: Songs · Albums · Artists · Playlists; search icon in top bar |
| Now Playing | Full-screen classic (large art, progress, transport controls, queue/repeat) |
| Theme | Follows system (dark/light), Material You dynamic color on 12+, fixed fallback below |
| Playlists | Room database |
| SDKs | compileSdk/targetSdk 36, minSdk 26 (Android 8.0+) |

## Tech Stack

- **UI**: Jetpack Compose (Material 3), Navigation Compose
- **Playback**: androidx.media3 ExoPlayer + `MediaSessionService`
- **Persistence**: Room (playlists only)
- **Library**: MediaStore (`MediaStore.Audio.Media`)
- **DI**: none — manual construction via a simple `AppContainer` (keeps the app light; Hilt is overkill for this size)

## App Structure

Single `app` module. Package `com.yplayer`. Layers:

- `ui/` — Compose screens, ViewModels, navigation
- `data/` — repositories, Room database/DAOs
- `player/` — `PlaybackService`, player state holder
- `AppContainer` — wires dependencies at the Application level

### Screens & navigation

- **SongsScreen** — list of all tracks (title, artist, album, duration)
- **AlbumsScreen** — grid of album covers (artwork, album, artist)
- **ArtistsScreen** — list of artists
- **PlaylistsScreen** — list of playlists; create new; tap to open detail
- **AlbumDetailScreen** — tracks of one album
- **ArtistDetailScreen** — albums and tracks of one artist
- **PlaylistDetailScreen** — tracks in a playlist; add/remove/reorder
- **SearchScreen** — full-text search over title/artist/album, live filtering
- **NowPlayingScreen** — full-screen classic layout

Navigation via Navigation Compose. Bottom nav hosts the four top-level destinations;
detail/search/now-playing screens are pushed on top. Each top-level tab keeps its own
back stack.

### Now Playing (full-screen classic)

- Large album artwork
- Track title + artist
- Seek bar with elapsed/total time
- Transport controls: previous / play-pause / next
- Secondary row: queue, repeat, shuffle, more (menu)

## Data Layer

### MediaStore (library)

`LibraryRepository` queries `MediaStore.Audio.Media` for audio files and maps rows to
a `Song` domain model: `id`, `title`, `artist`, `album`, `albumId`, `durationMs`,
`dataUri`. Query results are cached in memory (a `StateFlow`) so re-renders are instant;
a manual refresh re-queries MediaStore. Album art is loaded lazily per item through
`ContentResolver.loadThumbnail` from embedded artwork (no network).

### Room (playlists)

- `playlists` table: `id` (PK), `name`, `createdAt`
- `playlist_songs` table: `playlistId` (FK), `mediaId` (MediaStore id), `position`
  (composite PK of playlistId + mediaId)
- `PlaylistRepository` wraps DAOs: create/rename/delete playlist, add/remove songs,
  reorder (update positions), read playlist with its ordered songs.

Playlists reference songs by MediaStore ID. If a file is removed from the device it
simply won't resolve at play time (handled in playback, see Error Handling).

## Playback (Media3)

- `PlaybackService : MediaSessionService` owns a `MediaSession` + `ExoPlayer`.
- The service runs as a **foreground service** while playing, showing Media3's built-in
  media notification (play/pause/seek/next) — background playback works when the app is
  closed.
- UI observes player state via `MediaController` (current track, position, play state).
- **Queue building**: tapping a song plays it in the context of the current list
  (Songs list, an album, an artist's tracks, or a playlist). Shuffle and repeat are
  supported.
- Media3 handles audio focus and headphone-unplug (becoming noisy) automatically.

## Permissions

Runtime permissions requested on first launch:

- `READ_MEDIA_AUDIO` — Android 13+
- `READ_EXTERNAL_STORAGE` — Android 12 and below
- `POST_NOTIFICATIONS` — Android 13+ (for the media notification)

Denying audio access shows a friendly empty state with an "Allow access" button that
re-triggers the permission flow.

## Theme

- Theme follows the system setting (dark/light) with Material 3.
- Dynamic color (Material You) accent on Android 12+; fixed fallback accent palette on
  older versions.
- Full-bleed album artwork on the Now Playing screen in both themes.

## Error Handling

- **Permission denied / revoked** → empty state + "Allow access" CTA.
- **Empty library** → per-tab empty states ("No songs yet").
- **File missing/removed at play time** → skip to the next playable track; no crash.
- **Playlist references a removed file** → that row is skipped during playback.

## Testing

- Unit tests:
  - `PlaylistRepository` against an in-memory Room database
  - `LibraryRepository` against a fake MediaStore source
  - ViewModels with fake repositories
- UI tests: a small Navigation Compose test suite covering the four bottom tabs and
  search navigation.

## Implementation Order (suggested)

1. Project scaffolding: Gradle wrapper, manifest, Compose theme, navigation shell with
   empty tabs, permissions flow.
2. Library: MediaStore repository + Songs/Albums/Artists screens.
3. Playback: Media3 service, queue building, Now Playing screen, media notification.
4. Playlists: Room schema, DAOs, Playlists + detail screens (add/remove/reorder).
5. Search screen.
6. Polish: empty states, artwork loading, tests.
