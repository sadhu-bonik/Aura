# Feature: Video Preloading

## Goal

Eliminate the buffering delay when swiping between videos. Videos should start near-instantly on both vertical (creator) and horizontal (within-creator) swipes.

## Approach

Use Media3's `DefaultPreloadManager` to buffer adjacent videos in the background while the user watches the current one. No extra ExoPlayer instances are created — the preload manager uses an internal preload player that coordinates with the single playback player, keeping us within codec limits.

## Architecture

The feed is two-axis:
- Vertical: ViewPager2 paging through creators
- Horizontal: RecyclerView + PagerSnapHelper paging through a creator's videos

We flatten all videos into a 1D index (creator 0's items first, then creator 1's, etc.) and preload a ±5-item sliding window around the current position.

### New class: `FeedPreloadManager`

`app/src/main/java/com/aura/app/ui/feed/FeedPreloadManager.kt`

Owns both the `DefaultPreloadManager` and the `ExoPlayer`. Both are co-created via `DefaultPreloadManager.Builder` so they share `BandwidthMeter`, `TrackSelector`, `LoadControl`, and `Looper`.

**Key methods**:
- `updateFeedData(entries)` — called when Firestore delivers the feed; rebuilds the flat URL list and seeds the preload window
- `updateCurrentPosition(creatorIdx, itemIdx)` — called on every swipe (vertical or horizontal); shifts the sliding window, adds/removes items from the preload manager, calls `invalidate()`
- `getPreloadedMediaSource(mediaUrl)` — called in `attachPlayer()`; returns the pre-buffered `MediaSource` or `null` if not yet preloaded
- `release()` — releases preload manager and player

**Preload depth by distance from current**:

| Distance | Preload stage | Data buffered |
|---|---|---|
| 1 | `STAGE_LOADED_FOR_DURATION_MS` | 5 seconds |
| 2–3 | `STAGE_LOADED_FOR_DURATION_MS` | 2 seconds |
| 4–6 | `STAGE_TRACKS_SELECTED` | metadata only |
| 7–10 | `STAGE_SOURCE_PREPARED` | manifest only |
| > 10 | none | — |

### Wiring

- `VideoFeedFragment` owns `FeedPreloadManager` (replaces direct `ExoPlayer` ownership)
- `attachPlayer()` tries `getPreloadedMediaSource()` first; falls back to `MediaItem.fromUri()` if not yet preloaded
- `ActiveVideoCallback.onItemPositionChanged()` (new default method) called from `CreatorPageViewHolder`'s scroll listener on settle — triggers preload window shift for horizontal swipes

## Known Issues / Follow-up (2026-04-18)

On-device testing shows no perceptible speedup. Three suspected causes:

### 1 — `invalidate()` clears the item we're about to play (probable bug)

When `updateCurrentPosition(newIdx)` is called, the item at `newIdx` transitions from distance 1 (fully buffered) to distance 0. Our `TargetPreloadStatusControl` returns `null` for distance 0. Inside `BasePreloadManager`, a `null` status triggers `clearSourceInternal()` — wiping the buffered data. By the time `getPreloadedMediaSource()` runs, the source is empty.

**Fix**: Return `STAGE_LOADED_FOR_DURATION_MS` (full duration) for distance 0 instead of `null`, so the manager preserves the source rather than clearing it.

### 2 — MediaItem key mismatch (possible)

`getPreloadedMediaSource()` creates a fresh `MediaItem.fromUri(url)` for the lookup. If this doesn't `equals()` the key stored in the preload manager's map, every lookup returns `null` silently.

**Fix**: Add a debug log to confirm hit vs. miss. If always missing, look up using the stored `MediaItem` reference from the `registered` map.

### 3 — Serial preload is too slow (fundamental API limitation)

`BasePreloadManager` processes one source at a time. On a cold feed, only the +1 item gets meaningful data before the user swipes.

**Mitigation**: Ensure `updateFeedData()` is called as early as possible (already done — called on first `Content` emission). Consider reducing preload depth for +1 to load faster but still enough for instant start.

### Debug steps

1. Add `Log.d("FeedPreload", ...)` in `FeedPreloadManager.add()`, `getPreloadedMediaSource()`, and register a `DefaultPreloadManager.Listener` for `onCompleted`
2. Add hit/miss log in `VideoFeedFragment.attachPlayer()`
3. Swipe on device, check logcat for which items complete preloading and whether playback gets hits
4. Apply fix for whichever suspect the logs confirm

## Files

| File | Role |
|---|---|
| `app/.../ui/feed/FeedPreloadManager.kt` | New — owns DefaultPreloadManager + ExoPlayer |
| `app/.../ui/feed/VideoFeedFragment.kt` | Modified — uses FeedPreloadManager, wires preload into attachPlayer |
| `app/.../ui/feed/ActiveVideoCallback.kt` | Modified — added `onItemPositionChanged` default method |
| `app/.../ui/feed/CreatorPageViewHolder.kt` | Modified — fires `onItemPositionChanged` on horizontal scroll settle |
| `gradle.properties` | Modified — pinned `org.gradle.java.home` to Android Studio JBR |
