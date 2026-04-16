# Feature: Video Feed

**Status:** implemented (milestone 2 — vertical + horizontal axes, play/pause)
**Owner:** n/a yet
**Entry point:** `ui/feed/VideoFeedFragment` (start destination of `nav_graph.xml`)

## User story

Any user opening the app lands in a TikTok-style video feed. Swiping **vertically** pages through creators; swiping **horizontally** pages through a single creator's portfolio videos. The currently-focused video auto-plays and loops; all others are paused. Tapping a video toggles play/pause with a transient icon overlay. No auth gate yet — this will be added when the login screen lands.

## Screens

- `fragment_video_feed.xml` — vertical `ViewPager2` + centred loading / empty / error overlay.
- `item_creator_page.xml` — outer holder for each creator. Contains a horizontal `RecyclerView` (`items_recycler`) with `PagerSnapHelper` for that creator's items, a bottom bar with creator avatar + display name, and a dot indicator above the bar (hidden for single-item creators). **Note:** this was originally a nested `ViewPager2`, but nested ViewPager2 has a known measurement bug where inner pages don't fill the parent width. Replaced with `RecyclerView` + `LinearLayoutManager(HORIZONTAL)` + `PagerSnapHelper` which gives identical snap-to-page behavior without the measurement conflict. Item dimensions are set to exact pixels in `ItemPageAdapter.onBindViewHolder()`.
- `item_video_page.xml` — inner holder for each portfolio item. Full-bleed `PlayerView`, thumbnail shown until the first frame renders, centred play/pause indicator (animated fade), item title above the creator bar, error label on playback failure.

## Data reads

- `portfolioItems where mediaType == "video" and isPublic == true orderBy createdAt desc limit 50` — streamed via `PortfolioRepository.streamPublicVideos()` using Firestore `snapshots()`.
- ViewModel groups the flat list by `creatorId` into `List<CreatorFeedEntry>`, sorted by each creator's newest item `createdAt` descending.
- `users/{creatorId}` — fetched per creator via `UserRepository.getUserLite()` with an in-memory cache (loaded in `CreatorPageViewHolder`).

No writes.

## Playback lifecycle

- **Two-axis model:** the active cell is `(verticalIndex, horizontalIndex)`. Only one video plays at a time.
- `ExoPlayerPool` caps concurrent players at 8 with 4 pre-warmed at init. Outer `ViewPager2.offscreenPageLimit = 1`.
- Outer `ViewPager2.OnPageChangeCallback` activates the selected `CreatorPageViewHolder` and deactivates all others.
- Inner `RecyclerView.OnScrollListener` (inside `CreatorPageViewHolder`) detects page changes via `PagerSnapHelper.findSnapView()` when scroll settles to idle. Pauses the old item and plays the new one, but only if the outer page is active.
- `Fragment.onPause` deactivates all outer holders. `onResume` re-activates the selected one.
- `onViewRecycled` on the outer adapter releases all inner holders' players and unbinds the inner pager. `onViewRecycled` on the inner adapter releases that holder's player.
- `Fragment.onDestroyView` drains the pool.

## Play/pause on tap

- Tap anywhere on a video page toggles `playWhenReady`.
- A centred 72dp icon (play or pause) appears at 80% opacity and fades to 0 over 500ms (200ms start delay).
- The indicator animation is cancelled and reset on player release.

## Edge cases

- **Empty feed** — ViewModel emits `FeedUiState.Empty`; ViewPager hidden, empty message shown.
- **Firestore error** — `catch` in the ViewModel surfaces `FeedUiState.Error`.
- **Broken `mediaUrl`** — `Player.Listener.onPlayerError` shows an inline error label on that page; other pages unaffected; swiping back onto the page re-prepares playback.
- **Slow network** — thumbnail stays visible until `onRenderedFirstFrame`.
- **Missing creator doc** — overlay renders empty creator name/avatar; titles still show.
- **Creator with 1 video** — inner pager has 1 page, no horizontal swipe occurs, dot indicator hidden.
- **All videos from same creator** — 1 outer page with N inner pages; vertical swipe does nothing.
- **Nested scroll conflict** — perpendicular orientations (vertical outer, horizontal inner) handled natively by ViewPager2. `NestedScrollableHost` can be added if diagonal swipes cause issues.

## Invariants touched

None of the deal/chat/review invariants in `AGENTS.md` §4 apply — read-only feature. Schema field names match `docs/FIRESTORE_SCHEMA.md` exactly (camelCase).

## Out of scope / deferred

- Upload / record / pick-from-gallery.
- Auth gating (feature is open until the login screen lands).
- Likes / comments / follows.
- Analytics, view counts, prefetching beyond adjacent pages.
- Caching beyond ExoPlayer defaults.
- Send-deal button on creator overlay (depends on auth + deal screens).

## Verification

1. Seed `portfolioItems` in the Firebase console with `mediaType: "video"`, `isPublic: true`, playable `mediaUrl` values, and matching `users/{creatorId}` docs. Use multiple creators with multiple videos each.
2. `./gradlew assembleDebug` — passes.
3. Launch on device (API 24+):
   - Feed opens directly.
   - Vertical swipe pages through creators; horizontal swipe pages through items.
   - Only the active cell plays; all others paused.
   - Tap toggles play/pause with transient icon.
   - Dot indicator shows for multi-item creators, hidden for single-item.
   - Creator overlay (avatar + name) stays constant during horizontal swipe.
   - Item title updates on horizontal swipe.
   - Background/foreground pauses/resumes on the correct page.
   - Rotating preserves list + position.
4. Single-creator seed → vertical swipe does nothing, horizontal works.
5. Single-video-per-creator seed → horizontal swipe does nothing, vertical works.
6. Flip all seeds to `isPublic: false` → empty state renders.
7. Seed one item with an invalid URL → page shows error; swiping off/on recovers.
