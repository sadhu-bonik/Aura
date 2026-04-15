# Feature: Video Feed

**Status:** implemented (milestone 1 — vertical axis only)
**Owner:** n/a yet
**Entry point:** `ui/feed/VideoFeedFragment` (start destination of `nav_graph.xml`)

## User story

Any user opening the app lands in a TikTok-style vertical video feed of every creator's public portfolio videos. Swiping up advances to the next video; the currently-focused video auto-plays and loops, all others are paused. No auth gate yet — this will be added when the login screen lands.

## Screens

- `fragment_video_feed.xml` — vertical `ViewPager2` + centred loading / empty / error overlay.
- `item_video_page.xml` — full-bleed `androidx.media3.ui.PlayerView`, thumbnail shown until the first frame renders, bottom gradient with creator avatar + display name + item title, error label shown on playback failure.

## Data reads

- `portfolioItems where mediaType == "video" and isPublic == true orderBy createdAt desc limit 50` — streamed via `PortfolioRepository.streamPublicVideos()` using Firestore `snapshots()`.
- `users/{creatorId}` — fetched per item via `UserRepository.getUserLite()` with an in-memory cache.

No writes.

## Playback lifecycle

- `ExoPlayerPool` caps concurrent players at 4; `ViewPager2.offscreenPageLimit = 1`, so at most ~3 holders are alive at any time.
- `ViewPager2.OnPageChangeCallback.onPageSelected` is the single source of truth for "which page plays". Everything else is paused.
- `Fragment.onPause` pauses all holders. `onResume` re-plays the selected page.
- `ViewHolder.onViewRecycled` returns the `ExoPlayer` to the pool (stopped, media cleared). `Fragment.onDestroyView` drains the pool.

## Edge cases

- **Empty feed** — ViewModel emits `FeedUiState.Empty`; ViewPager hidden, empty message shown.
- **Firestore error** — `catch` in the ViewModel surfaces `FeedUiState.Error`.
- **Broken `mediaUrl`** — `Player.Listener.onPlayerError` shows an inline error label on that page; other pages unaffected; swiping back onto the page re-prepares playback.
- **Slow network** — thumbnail stays visible until `onRenderedFirstFrame`.
- **Missing creator doc** — overlay simply renders empty creator name/avatar; title still shows.

## Invariants touched

None of the deal/chat/review invariants in `AGENTS.md` §4 apply — read-only feature. Schema field names match `docs/FIRESTORE_SCHEMA.md` exactly (camelCase).

## Out of scope / deferred

- **Horizontal axis** — per SRA §4.4 Seq 001 and the Sequence-Brand's Activities UML, horizontal swipe is meant to page through a single creator's portfolio items without leaving the feed. Deferred: it re-shapes a "page" (creator → list of their items) and requires nested pagers with per-creator player reuse. Worth landing once the vertical axis is stable.
- Upload / record / pick-from-gallery.
- Auth gating (feature is open until the login screen lands).
- Likes / comments / follows.
- Analytics, view counts, prefetching beyond adjacent pages.
- Caching beyond ExoPlayer defaults.

## Verification

1. Seed `portfolioItems` in the Firebase console with `mediaType: "video"`, `isPublic: true`, a playable `mediaUrl`, and matching `users/{creatorId}` docs.
2. `./gradlew assembleDebug` — passes.
3. Launch on device (API 24+):
   - Feed opens directly.
   - First video auto-plays; vertical swipe advances; previous video pauses.
   - Overlay shows creator `displayName` and item `title`.
   - Background/foreground pauses/resumes on the correct page.
   - Rotating preserves list + position.
4. Flip all seeds to `isPublic: false` → empty state renders.
5. Seed one item with an invalid URL → page shows error; swiping off/on recovers.
