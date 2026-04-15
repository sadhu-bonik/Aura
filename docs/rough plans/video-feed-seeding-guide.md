# Video Feed — Seeding & Verification Guide

## Context

The video feed feature is implemented and the debug APK builds cleanly. But the `portfolioItems` and `users` collections in Firestore are empty, so launching the app right now will land on the empty state ("No videos yet"). Before you can actually see the feed working, you need to hand-seed a few test documents in the Firebase console. This guide walks through that — plus the exact checks to run once data is in place.

Project: `aura-creatorbrandcolab` (Firebase).
Entry screen: `VideoFeedFragment` — start destination of `nav_graph.xml`, so the app opens straight into the feed.

## Step 1 — Seed `users` documents

Go to Firebase Console → Firestore Database → Start/select the `users` collection. Create 2–3 documents. Document IDs should match the `creatorId` you'll reference from `portfolioItems` (any stable string works — `testCreator1`, etc., since we have no auth yet).

For each `users/{userId}` doc, add:

| Field | Type | Example |
|---|---|---|
| `userId` | string | `testCreator1` *(must match the doc ID)* |
| `email` | string | `creator1@test.local` |
| `role` | string | `creator` |
| `displayName` | string | `Sam the Creator` |
| `profileImageUrl` | string | any public image URL, or leave `""` |
| `createdAt` | timestamp | click the timestamp picker, pick now |
| `isProfileComplete` | boolean | `true` |

Only `displayName` and `profileImageUrl` are actually used by the feed overlay — the rest are required by `docs/FIRESTORE_SCHEMA.md` so keep them for consistency.

## Step 2 — Seed `portfolioItems` documents

Create 3–5 documents in the `portfolioItems` collection. Document IDs can be auto-generated.

For each doc, add:

| Field | Type | Example |
|---|---|---|
| `itemId` | string | (same as the auto-generated doc ID, or any stable string) |
| `creatorId` | string | one of the `userId`s from Step 1, e.g. `testCreator1` |
| `title` | string | `My first portfolio video` |
| `description` | string | `""` (optional) |
| `mediaUrl` | string | **a publicly playable mp4 URL** — see below |
| `mediaType` | string | `video` *(exact spelling — lowercase)* |
| `thumbnailUrl` | string | any image URL, or `""` |
| `isPublic` | boolean | `true` *(must be true — the query filters on this)* |
| `createdAt` | timestamp | now (vary these slightly between docs so ordering is visible) |

### Known-good public mp4 URLs

Any of these work for quick testing — no auth, no CORS issues:

- `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4`
- `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4`
- `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4`
- `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4`
- `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4`

Use different URLs across seeded items so swipes are visibly distinct.

## Step 3 — Confirm Firestore security rules allow reads

The debug app has no authenticated user yet. Firestore default **test mode** rules allow open reads/writes until a date in the console; **production mode** blocks everything. Firebase Console → Firestore → Rules. If rules look like:

```
allow read, write: if false;
```

temporarily switch to:

```
allow read: if true;
allow write: if false;
```

(Read-only open access. Don't leave this on past milestone 1 — the real rules come when auth lands.)

## Step 4 — Install and run the APK

Build artifact is already produced at `app/build/outputs/apk/debug/app-debug.apk` (from the earlier successful `assembleDebug`). To install on a connected device/emulator (API 24+):

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or just hit ▶ Run in Android Studio.

## Step 5 — Verification checks (golden path + edge cases)

Run through these in order:

1. **Launch** → app opens directly into the feed.
2. **First video auto-plays** within ~1–3s (spinner while loading, then the first frame renders and the thumbnail disappears).
3. **Overlay** shows creator's `displayName`, item `title`, avatar if `profileImageUrl` is set.
4. **Vertical swipe up** → advances to the next video. The previous video pauses; the new one starts from 0 and loops.
5. **Swipe back down** → previous video resumes playback from 0 (acceptable for milestone 1).
6. **Background/foreground** the app (press Home, then reopen) → playback pauses on background, resumes on the selected page.
7. **Rotate the device** → list and scroll position survive (ViewModel keeps state).

### Edge cases

8. **Empty state**: temporarily flip all seeded items' `isPublic` to `false`. Force-stop the app and reopen — you should see "No videos yet. Check back soon." and no crash. Flip them back when done.
9. **Broken URL**: seed one extra item with `mediaUrl = "https://example.com/does-not-exist.mp4"`. Swipe onto it → the in-page error label ("This video couldn't play.") appears. Other pages must remain unaffected.
10. **Memory**: seed ~15–20 items and swipe through them. In Android Studio → Profiler → Memory, confirm the process doesn't balloon — the ExoPlayer pool caps at 4 so live player count stays flat.

## Common gotchas

- **`mediaType` must be exactly `"video"`** (lowercase). Any other casing fails the `whereEqualTo` filter silently — you'll see empty state instead of your items.
- **`isPublic` must be a boolean `true`**, not the string `"true"`. The Firestore console distinguishes these.
- **`createdAt` must be a timestamp**, not a string. Items without it won't be included in the `orderBy` result.
- **Rules still blocking reads** → logs show `PERMISSION_DENIED` from Firestore and the ViewModel lands in `FeedUiState.Error`.
- **No video auto-plays on emulator** → some emulators have broken audio/codec stacks. Try a real device or a different AVD image (x86_64 with Google APIs works best).

## Files referenced

- `docs/FIRESTORE_SCHEMA.md` — authoritative field list for `users` and `portfolioItems`.
- `docs/features/video-feed.md` — feature spec written during implementation.
- `app/src/main/java/com/aura/app/data/repository/PortfolioRepository.kt` — the query you're feeding.
- `app/src/main/java/com/aura/app/ui/feed/VideoFeedFragment.kt` — the screen you're testing.
