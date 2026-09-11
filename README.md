# Towfik Music

An original, self-contained Android music player. It indexes and plays the audio files
**already stored on the user's own device** (via `MediaStore`) — it does not download,
stream, or redistribute any copyrighted content.

This project was rebuilt from a blank skeleton. It contains no code, assets, or binaries
from any third-party app.

| | |
|---|---|
| App name | **Towfik Music** |
| Package / applicationId | `com.towfik.music` |
| minSdk / target / compile | 24 / 34 / 34 |
| Language | Kotlin |

## Features

- **Library** — browse and search the device's local audio; play with one tap.
- **Playback** — foreground `PlaybackService` with `MediaPlayer`, `MediaSessionCompat`,
  a media-style notification, mini-player, and a full Now Playing bottom sheet
  (seek, shuffle, repeat, next/prev).
- **Playlists** — create / open / delete playlists. The **free plan is limited to 2
  playlists**; Premium removes the limit.
- **Premium** — a real Google Play **subscription / purchase** (see below). Free and
  Premium are genuinely gated: equalizer, playback speed, accent themes, sleep timer,
  unlimited playlists, gapless queue.
- **Material 3** day/night UI with a violet/gold brand theme and generated launcher icons.

## Premium is a real purchase (not a crack)

Premium entitlement is granted **only** from a purchase that Google Play Billing reports
as `PURCHASED` and acknowledges. `PremiumRepository` stores a purchase-derived expiry and
`hasFeature()` simply compares it against the clock — there is no offline unlock or patched
check, and `reconcile()` clears the entitlement if Play reports no active purchase.

Product IDs used (create these in **Play Console** before purchases can complete):

- `towfik_music_premium_monthly` (subscription)
- `towfik_music_premium_yearly` (subscription)
- `towfik_music_premium_lifetime` (one-time)

## Layout of the code

```
app/src/main/java/com/towfik/music/
  MainActivity.kt            bottom-nav host + service binding + mini player
  MusicApplication.kt        process-wide singletons
  data/                      Track, TrackRepository (MediaStore), PlaylistRepository
  playback/PlaybackService.kt MediaPlayer + MediaSession + notification
  premium/                   BillingManager (Play Billing), PremiumRepository, policies
  ui/                        Library / Playlists / Premium / Settings / NowPlaying + adapters
  util/Duration.kt           pure formatting helper (unit-tested)
app/src/test/                JVM unit tests for premium gating, playlist policy, duration
tools/verify.py              static resource / view-binding consistency lint
tools/make_icons.py          regenerates launcher icons from art/icon_source.png
```

## Build

A Gradle build needs the Android SDK, so it runs in CI (`.github/workflows/build-apk.yml`),
which executes:

1. `python3 tools/verify.py`  — static resource & view-binding checks
2. `./gradlew testDebugUnitTest`
3. `./gradlew assembleDebug`   — uploads `towfik-music-debug.apk`

Locally, with an Android SDK + JDK 17 installed: `./gradlew assembleDebug`.

## Regenerating icons

```
python3 tools/make_icons.py   # derives all mipmap densities from art/icon_source.png
```
