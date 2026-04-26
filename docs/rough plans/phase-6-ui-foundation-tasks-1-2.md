# Aura — Foundation-Only UI Integration (Tasks 1 + 2)

## Context

Backend logic is fully merged on `feature/deal_workflow` (commits `dedded5` + `de47068`): `ProfileViewModel` (state + uploadEvent), `EditProfileViewModel` (state + event), `VideoFeedViewModel` (FeedUiState with niche-ranked entries via `CreatorRankingRepository` + `NicheMatcher`), `YouTubeRepository.fetchAndScore`, `PortfolioAdapter`, multi-step Auth flows. Existing UI is outdated XML placeholder.

The Figma source lives in `Aura_UI_UX/`, with the master spec at `Aura_UI_UX/aura_lumina/DESIGN.md` ("Ethereal Atelier — Nocturnal Edition"). 23 screen folders each contain `screen.png` + Stitch-generated `code.html` with the canonical token map.

**Scope decision (locked 2026-04-25):** the team expects further design changes. To avoid throwaway work on feature screens, **active scope is Tasks 1 + 2 only** — the foundation (palette, font, gradient, "no-line" tokens) and the reusable component includes. Both layers are inherently design-change-tolerant: a future palette tweak is a one-file edit to `colors.xml`; a chip-style change is a single edit to `styles.xml`; the structural includes (`layout_empty_state`, `include_section_header`, `include_stat_pill`, glass bottom nav) survive 90% of redesigns because their *contract* (where you put text/icons) doesn't change even when the visual treatment does.

Tasks 3–5 (Profile, Edit Profile, Video Feed vertical slices) are documented below in **"Deferred — pending design lock"** so the analysis isn't lost, but they are NOT to be implemented until the designer confirms the screen structures won't materially shift.

**Critical reality check vs. current `res/values/`:** the Figma palette is meaningfully different from what's in `colors.xml` today. Current uses `#B1A1FF` and `#6FFBBE`; Figma uses `primary #cac1ed`, `surface #0f0e12`, full Material 3 token set including `tertiary-container #fdcbf2` (the "Aura Chip") and a `#5620e0 → #7a729a` 135° linear gradient. Current `Widget.Aura.Card` has `strokeWidth: 1dp` which **violates the design system's "No-Line Rule"** (separation must be tonal, not stroked). Font is **Manrope** — not currently bundled. So Task 1 is a real foundation rewrite, not a patch.

**Note: no Figma screen surfaces YouTube analytics anywhere on the creator profile** — `grep -r "engagement|analytic|score"` only hits deal-history / campaign-setup contexts. Coordinate with the designer separately if SRA §4.6 needs a render surface.

---

## Public state surface to wire against (do NOT redefine)

| ViewModel | State | Events / commands |
|---|---|---|
| `ProfileViewModel` (`ui/main/ProfileViewModel.kt`) | `state: StateFlow<ProfileUiState>` = `Loading \| Success(user, creatorProfile, brandProfile, portfolio, isOwner) \| Error` | `uploadEvent: SharedFlow<UploadEvent>` = `Started \| Progress(msg) \| Success \| Failure(msg)`. Methods: `loadProfile(creatorId?)`, `uploadProfilePicture(uri)`, `uploadPortfolioVideo(...)`, `deletePortfolioItem(item)`. Factory takes `Context` for `SessionManager`. |
| `EditProfileViewModel` (`ui/main/EditProfileViewModel.kt`) | `state: StateFlow<EditProfileUiState>` = `Loading \| Success(user, creatorProfile, brandProfile) \| Error` | `event: StateFlow<EditProfileEvent?>` = `Saving \| SaveSuccess \| SaveError(msg)`. Methods: `saveProfile(name, bio, selectedTags, uri)`, `resetEvent()`. Auto-loads on `init`. |
| `VideoFeedViewModel` (`ui/feed/VideoFeedViewModel.kt`) | `state: StateFlow<FeedUiState>` = `Loading \| Empty \| Content(entries) \| Error(msg)` | `loadCreatorFeed()`, `loadMoreFeed()`. Pre-resolves `creatorName` + `creatorProfileImageUrl` on each entry. Auto-loads on `init`. |

`PortfolioAdapter` lives at `app/src/main/java/com/aura/app/adapters/PortfolioAdapter.kt` (DiffCallback + ViewHolder, ready for `submitList`).

---

## Canonical Figma tokens (source of truth — copy into `colors.xml` verbatim)

From `Aura_UI_UX/aura_creator_profile_unified_system/code.html` (cross-checked against 5 other screens, identical):

| Token | Hex | Usage |
|---|---|---|
| `background` / `surface` / `surface-dim` | `#0f0e12` | Primary dark base. Never `#000000` |
| `surface-container-lowest` | `#000000` | Edge cases only |
| `surface-container-low` | `#141318` | Recessed sections, pill input bg |
| `surface-container` | `#1a191f` | Standard cards, post thumbs |
| `surface-container-high` | `#201f26` | Elevated |
| `surface-container-highest` / `surface-variant` | `#27252d` | Most prominent, role chips |
| `surface-bright` | `#2d2b34` | |
| `primary` / `surface-tint` | `#cac1ed` | Light lavender — primary text/icon |
| `on-primary` | `#423b60` | Text on primary fill |
| `primary-container` | `#554d73` | Filled containers |
| `on-primary-container` | `#e7dfff` | |
| `primary-fixed` | `#d6ccf9` | |
| `primary-dim` | `#bcb3df` | |
| `secondary` | `#cac3dc` | |
| `tertiary` | `#ffdff6` | |
| `tertiary-container` | `#fdcbf2` | "Aura Chip" pink fill |
| `on-tertiary-container` | `#654060` | "Aura Chip" text |
| `error` | `#f97386` | |
| `error-dim` | `#c44b5f` | Form error message text |
| `on-background` / `on-surface` | `#e9e3ef` | Primary text |
| `on-surface-variant` | `#aea9b5` | Secondary text, label headers |
| `outline` | `#77747e` | Input placeholder |
| `outline-variant` | `#494650` | "Ghost border" at 15% opacity per DESIGN.md §4 |

**Gradient (CTAs, progress fill):** `linear-gradient(135deg, #5620e0 0%, #7a729a 100%)`. Ship as `drawable/bg_aura_gradient_primary.xml`. The `#5620e0` purple is also used as the **active bottom-nav icon color** (verified in 4 screen files).

**Border radii:** `DEFAULT 1rem (16dp)`, `lg 2rem (32dp)`, `xl 3rem (48dp)`, `full 9999px (pill — height/2 in Android)`. Inputs and primary buttons use `full`. Cards use `2rem` (the "stat pill" cards on profile use `rounded-2xl`).

**Font:** Manrope, weights `200/300/400/500/600/700/800`. Add as a downloadable font (`res/font/manrope.xml` referencing Google Fonts provider) so it doesn't bloat APK.

---

## Active scope: Tasks 1 & 2

### Task 1 — Foundation rewrite (palette, font, gradient, "No-Line" tokens)

**Goal.** Replace the partial / drifted token set in `res/values/` with the canonical "Ethereal Atelier" palette. After this task lands, every existing styled widget (`Widget.Aura.Button.Primary`, `Widget.Aura.Card`, etc.) automatically picks up the new look without changing call-sites in layouts.

**Files to edit.**
- `app/src/main/res/values/colors.xml` — rewrite. Replace the current SRA / Stitch hybrid (which has both `#B1A1FF` AND `#6FFBBE` as primaries) with the 23-token Material 3 set in the table above. Keep legacy aliases (`colorPrimary`, `colorSurface`, `colorOnPrimary`, etc.) pointing at the new hexes so existing layouts don't break, and ADD the kebab-case M3 names as additional aliases (`color_primary`, `color_surface_container_high`, …) for layouts written from Figma.
- `app/src/main/res/values/styles.xml`:
  - **`Widget.Aura.Card`** — drop `strokeWidth: 1dp` to obey the No-Line Rule (DESIGN.md §2). Use only `cardBackgroundColor` tonal shifts. Add `Widget.Aura.Card.GhostBorder` variant with `outline-variant @ 15% alpha` for the rare accessibility case. Verify `cardCornerRadius` matches the per-screen radius (`rounded-2xl` ≈ 16dp, `rounded-3xl` ≈ 24dp).
  - **`Widget.Aura.Button.Primary`** — change `backgroundTint` from flat `colorPrimary` to a drawable: `@drawable/bg_aura_gradient_primary` (created below). `cornerRadius` → 50% of height (use `?attr/buttonCornerRadius` or hardcode `28dp` for `52dp` height — pill).
  - **`Widget.Aura.TextInputLayout`** — switch from `OutlinedBox` parent to `FilledBox`. `boxBackgroundColor → @color/colorSurfaceContainerLow`, `boxStrokeWidth: 0dp`, `boxStrokeWidthFocused: 2dp`, `boxStrokeColor → @color/colorPrimary @ 50%` (matches `focus:ring-2 ring-primary/50`), `boxCornerRadius` → 28dp (pill at 56dp height). The Figma input height is `h-14` = 56dp.
  - **Text appearances** — change `android:fontFamily` on every `TextAppearance.Aura.*` from `sans-serif-medium` to `@font/manrope_medium` / `manrope_bold` etc.
- `app/src/main/res/values/themes.xml` — currently empty. Move the M3 theme attribute mapping here from `styles.xml` (or leave in styles, but at minimum register `<item name="fontFamily">@font/manrope</item>` on the base theme so every TextView inherits Manrope by default).

**Files to create.**
- `app/src/main/res/font/manrope.xml` — downloadable-font definition (Google Fonts provider, family `Manrope`, default weight 400). Plus per-weight aliases `manrope_medium.xml` (500), `manrope_semibold.xml` (600), `manrope_bold.xml` (700), `manrope_extrabold.xml` (800).
- `app/src/main/res/drawable/bg_aura_gradient_primary.xml` — `<shape android:shape="rectangle">` with `<gradient android:angle="135" android:startColor="#5620e0" android:endColor="#7a729a"/>` and `<corners android:radius="9999dp"/>` so it pills automatically inside any height. (Android draws gradients at layer-list level — for buttons it's fine on `MaterialButton` via `app:backgroundTint=null` + `android:background`.)
- `app/src/main/res/values/dimens.xml` — verify it has `space_xs..xxl`, `corner_radius_sm/md/lg/full`, `icon_size_*`. Add any missing per UI_TOKENS.md §3.

**Definition of Done.**
1. `./gradlew assembleDebug` clean.
2. Open existing `fragment_home_container.xml` in the layout previewer — bottom nav background and any text reading the legacy color aliases now show the new `#0f0e12` and `#cac1ed`. No regressions.
3. Open Figma `aura_creator_profile_unified_system/screen.png` and any screen we haven't touched yet (e.g. `fragment_video_feed.xml` previewing on dark) — verify the existing widgets that reference `Widget.Aura.Button.Primary` now render with the purple gradient pill.
4. Manrope is the actual font on a real device (sideload + `adb shell dumpsys` or eyeball — Manrope's lowercase `g` is distinctive).
5. Lint `./gradlew lintDebug` shows zero new `HardcodedColor` / `HardcodedText` warnings introduced by this task.

---

### Task 2 — Reusable component includes (per UI_TOKENS §10 + Figma)

**Goal.** Build the shared layout includes EVERY feature screen will compose, before touching feature screens. This prevents Tasks 3–5 from duplicating loading / empty / stat / section-header markup.

**Files to create.**
- `layout_loading.xml` — centered indeterminate circular progress (M3 `CircularProgressIndicator`, indicatorColor `colorPrimary`). Standard ID `@+id/pb_loading`.
- `layout_empty_state.xml` — vertical container with `Material Symbols Outlined` icon (use `@drawable/ic_*` placeholder for now), `tv_empty_title` (TitleLarge), `tv_empty_subtitle` (BodyMedium, `colorOnSurfaceVariant`).
- `layout_error.xml` — icon + `tv_error_message` + `btn_retry` MaterialButton with `Widget.Aura.Button.Primary` style.
- `include_section_header.xml` — the Figma "About" / "Posts" pattern: `<TextView styled UPPERCASE BOLD TRACKING-WIDEST color=onSurfaceVariant>` next to a `<View height=1dp background=@color/colorOutlineVariant alpha=0.1>` filling the rest. Used on profile, brand profile, and feed-detail bottom sheets. Inflate as `<include layout="@layout/include_section_header"/>` and override the title via `findViewById(R.id.tv_section_title).text = "..."` from the host fragment.
- `include_stat_pill.xml` — the rounded-2xl `surface-container-low` card with a `text-2xl primary` value and a `text-[10px] uppercase tracking-widest on-surface-variant` label. Used in 3-up rows on Creator and Brand profiles (creator: Views / Rank, brand: Campaigns / Creators / Success).
- `item_post_thumb.xml` — `aspect-[3/4] surface-container rounded-lg` thumb with optional bottom-left play-count overlay (`absolute bottom-2 left-2 bg-black/40 backdrop-blur rounded text-xs`). Inflated by `PortfolioAdapter`.
- `item_creator_card_horizontal.xml` — circular avatar with `bg-gradient-to-tr from-primary to-tertiary p-1` ring, name + 1-line niche label below. Used in "Suggested Creators" rail on Brand profile + (later) Brand discovery feed.
- **Glass bottom-nav refresh** in `fragment_home_container.xml`:
  - Add `android:background="@drawable/bg_glass_bottom_nav"` (new drawable: `surface @ 80% alpha` + `corners android:topLeftRadius="40dp" android:topRightRadius="40dp"` + a soft elevation glow — for the actual blur, leave a TODO since true backdrop-blur in stock Android requires API 31+ `RenderEffect`; for now the 80%-alpha tonal shift is a faithful enough fallback).
  - Update active indicator color to `#5620e0` (the Figma accent), update `color_nav_item.xml` selector accordingly.

**Definition of Done.**
1. Each include compiles and renders in the Layout Editor preview against the new Task 1 theme.
2. A throwaway `DebugFragment` (or just the existing `DiscoverFragment`'s empty layout, temporarily) displays one of each include, side-by-side, matching the Figma look at glance.
3. The `fragment_home_container.xml` bottom nav now has rounded top corners + glass background + purple-accent active item, matching `aura_creator_profile_unified_system/screen.png`.

---

## Verification after Tasks 1 + 2 (active scope)

1. `./gradlew assembleDebug` clean.
2. `./gradlew lintDebug` — zero new `HardcodedColor` / `MissingFont` warnings.
3. Existing screens (untouched in this scope) still build and load — no broken style refs.
4. Bottom nav on `fragment_home_container.xml` now matches the glass + rounded-top-corners + purple-accent active item from any Figma screen.
5. A throwaway `DebugFragment` (or temporarily inflated in `DiscoverFragment`'s empty layout) renders one of each include from Task 2, side-by-side, against the new theme — visual sanity check.
6. Manrope is the actual font on a real device.

After this verification passes, **stop and wait** for the designer to lock screen designs before starting Tasks 3–5.

---

## Deferred — pending design lock (Tasks 3 / 4 / 5)

The analysis below is preserved for resumption once the designer confirms the screen structures (banner / identity / stats row / about / posts/suggested for profile, pill-input form for edit, vertical pager + overlays for feed) are final. **Do not implement until then.**

When resuming, re-verify against the latest Figma — if the screen `code.html` files in `Aura_UI_UX/` have been updated, re-extract the canonical token references before coding.

### Task 3 (deferred) — Profile screen vertical slice (creator + brand, single Fragment)

**Goal.** Replace `fragment_profile.xml` with a single role-aware layout that matches `aura_creator_profile_unified_system` (creator) and `aura_brand_profile_polished` (brand), wired to `ProfileViewModel`. The two designs share 90% structure (banner / avatar / handle / chips / stats / about / role-specific tail), so one layout with conditional visibility is correct.

**Files to edit.**
- `app/src/main/res/layout/fragment_profile.xml` — `NestedScrollView` root. From top:
  1. **Hero banner**: `ShapeableImageView height=280dp scaleType=centerCrop`, with a `View` overlay for the gradient `bg-gradient-to-t from-surface via-surface/20 to-transparent` (drawable `bg_profile_hero_gradient.xml` — vertical gradient ending at `colorBackground`).
  2. **Identity block** at `marginTop=-56dp` (the Figma `-mt-14`): 80dp circular avatar with 2dp `colorPrimary @ 20% alpha` border, optional verified badge (filled material-symbol `verified` on `colorPrimary` background pill), 3xl extrabold tracking-tighter handle, then a chip row.
  3. **Stats row** (3-up `LinearLayout`) of `<include layout="@layout/include_stat_pill"/>`. Bind by ID:
     - Creator role: pill 1 = total Views (sum of `portfolio.items.viewCount` if available; else placeholder), pill 2 = "Top X%" rank (placeholder until ranking endpoint exists).
     - Brand role: pill 1 = Campaigns count, pill 2 = Creators worked-with count, pill 3 = Success%.
  4. **About section**: `<include layout="@layout/include_section_header"/>` with title "About" → BodyLarge `bio` text from the role-specific profile.
  5. **Tail (role-conditional)**:
     - Creator: another section-header "Posts" → 3-column `RecyclerView` with `GridLayoutManager(spanCount=3)`, `PortfolioAdapter`, `item_post_thumb.xml` (Task 2). Visibility GONE when `role==brand`.
     - Brand: section-header "Suggested Creators" → horizontal `RecyclerView` of `item_creator_card_horizontal.xml`. Visibility GONE when `role==creator`. (Adapter feed is TBD — for now bind from a placeholder list; the actual brand-side recommendations is a separate work-stream.)
  6. **`<include layout="@layout/layout_loading"/>` + `layout_empty_state` + `layout_error`**, all initially `visibility="gone"`, toggled by state observer.
- `app/src/main/java/com/aura/app/ui/main/ProfileFragment.kt` — replace any current rendering with two collectors:
  ```kotlin
  viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
          launch { viewModel.state.collect(::renderState) }
          launch { viewModel.uploadEvent.collect(::renderUploadEvent) }
      }
  }
  ```
  - `renderState(ProfileUiState.Loading)` → show `pb_loading`, hide content + error.
  - `renderState(ProfileUiState.Success)` → branch on `user.role` to populate creator vs brand sections; submit `portfolio` to `PortfolioAdapter` via `submitList`. Show empty-state if creator + portfolio empty (per memory `feedback_android_lifecycle_pitfalls.md`: "every list needs an empty state").
  - `renderState(ProfileUiState.Error)` → show error layout with retry that calls `viewModel.loadProfile(args.creatorId)`.
  - `renderUploadEvent` → Snackbar lifecycle: Started ⇒ persistent indeterminate Snackbar, Progress ⇒ updates Snackbar text, Success/Failure ⇒ replace with terminal Snackbar.
  - Avatar tap (when `isOwner`) → `ActivityResultContracts.PickVisualMedia()` → `viewModel.uploadProfilePicture(uri)`.
  - "Edit" CTA visible when `isOwner` → `findNavController().navigate(...)` to EditProfileFragment (Task 4).

**Reused.**
- `PortfolioAdapter` already exists; verify its item layout — if the existing one is the outdated placeholder, swap the inflate target to `item_post_thumb.xml` from Task 2.
- `Glide` for avatar + post thumbs (UI_TOKENS §7).

**Definition of Done.**
1. Cold-launch on a creator account → Profile tab renders banner / avatar / handle / niche chips / Stats pills / About / Posts grid in <500ms after data loads. Spinner shows briefly on cold start.
2. Brand account → renders same banner shell + brand stats pills (3 of them) + About + "Suggested Creators" rail. Posts grid is GONE. No crash.
3. Tap own avatar → image picker → upload → progress Snackbar → success → avatar refreshes to new URL (the VM internally calls `loadProfile()` on success).
4. Empty portfolio creator → empty-state visible (icon + "No videos yet"). No empty grid blank space.
5. Rotate device on Profile screen → no Snackbar replay, no duplicate state emission. (Verifies `repeatOnLifecycle(STARTED)` is correctly scoped to `viewLifecycleOwner`.)
6. Side-by-side compare with `Aura_UI_UX/aura_creator_profile_unified_system/screen.png` and `aura_brand_profile_polished/screen.png` — visual parity at >90% (banner radius, hero gradient, avatar offset, chip styles, stat pill colors, section header style).

---

### Task 4 (deferred) — Edit Profile vertical slice (pill inputs + Manrope form)

**Goal.** Replace `fragment_edit_profile.xml` with the Figma form pattern from `Aura_UI_UX/aura_personal_info_refined` (the same pill-input + ghost-border-on-focus + 11px error message pattern is used in both registration and edit-profile flows). Wire to `EditProfileViewModel`.

**Files to edit.**
- `app/src/main/res/layout/fragment_edit_profile.xml`:
  - **TopAppBar**: glass `bg-neutral-950/80 backdrop-blur-xl` (use existing `bg_glass_bottom_nav` analog or new drawable `bg_glass_top_app_bar.xml`). Back button = filled material-symbol `arrow_back` in `colorPrimary` (use `Widget.Material3.Button.IconButton.Filled` styled with `Widget.Aura.Button.Primary` background tint).
  - **Avatar with pencil overlay** (80dp circle + small filled `edit` icon at bottom-right, on `colorPrimary` background pill).
  - **`Widget.Aura.TextInputLayout`** (now pill-shaped after Task 1) for name + bio (multi-line, `app:counterEnabled` on bio, max 160).
  - **Tag chip group** using `Widget.Aura.Chip.Niche` + `.Selected` (already in styles). Verify the chip background swaps to `tertiary-container` for selected (per "Aura Chip" in DESIGN.md §5) — this may require adding `Widget.Aura.Chip.Aura` variant in Task 1's styles bundle.
  - **Save MaterialButton** with `Widget.Aura.Button.Primary` (gradient pill from Task 1).
  - Includes for `layout_loading`, `layout_error`.
- `app/src/main/java/com/aura/app/ui/main/EditProfileFragment.kt`:
  - Collect `viewModel.state` for prefill: `Loading` → spinner; `Success` → populate name field, bio field, select chips matching `creatorProfile.tags` or `brandProfile.industryTags`, load avatar via Glide.
  - Collect `viewModel.event`: `Saving` → disable Save button + show inline progress; `SaveSuccess` → Snackbar "Profile saved" + `findNavController().popBackStack()` + `viewModel.resetEvent()`; `SaveError(msg)` → setError on the affected field if name-blank, else Snackbar.
  - Image picker via `ActivityResultContracts.PickVisualMedia()` → cache Uri locally → pass to `viewModel.saveProfile(name, bio, selectedTags, uri)` on Save tap.

**Reused.**
- Niche / industry tag arrays — the same constants used in registration step 3 (creator) / step 4 (brand). Verify and reuse, don't duplicate.

**Definition of Done.**
1. From Profile (Task 3), tap Edit → form pre-fills name / bio / selected tags from existing profile. No empty fields for an already-set-up creator.
2. Modify name + bio + add/remove chip + tap Save → button enters loading state on `EditProfileEvent.Saving` → Snackbar "Profile saved" on `SaveSuccess` → back-navigates → Profile screen reflects new values. (May need explicit `viewModel.loadProfile()` in Profile's `onResume`, since profile fields are one-shot and only portfolio is reactive.)
3. Empty name → Save → form-level error rendered as `setError` on the name TIL with the 11px `error-dim` color treatment (matching Figma error state).
4. Brand role: bio + industryTags persist to `brandProfiles/{uid}` (verify in Firestore). Creator role: bio + tags + niche persist to `creatorProfiles/{uid}`. (Already correct in `EditProfileViewModel.saveProfile`.)
5. Avatar pick + save in one submission → avatar updates on Profile after pop.
6. Visual compare with `Aura_UI_UX/aura_personal_info_refined/screen.png` — pill inputs, ghost border on focus, label-above-input pattern, 11px error message, gradient progress fill on TopAppBar (if shown), Manrope throughout.

---

### Task 5 (deferred) — Video Feed vertical slice (creator feed Figma + niche-ranked content)

**Goal.** Replace the outdated `fragment_video_feed.xml` overlay with the Figma `aura_creator_feed` spec, wire to `VideoFeedViewModel`, surface the `NicheMatcher` ranking quietly (the Figma doesn't label "Recommended For You" — ranking is implicit; ordering is the signal).

**Files to edit.**
- `app/src/main/res/layout/fragment_video_feed.xml` — outer `ViewPager2` (vertical paging by creator) — already in place per audit. Per-page child `<include layout="@layout/item_feed_creator_page"/>`.
- `app/src/main/res/layout/item_feed_creator_page.xml` (verify exists; create if not):
  - Inner horizontal `RecyclerView` with `PagerSnapHelper` for portfolio items.
  - Two gradient overlay `View`s: `bg-gradient-to-t from-black/80 via-transparent to-black/40` (drawable `bg_feed_overlay_vertical.xml`) and `bg-gradient-to-r from-black/20 via-transparent to-transparent` (drawable `bg_feed_overlay_horizontal.xml`).
  - **Right-side action rail** (`absolute right-4 bottom-32`): bookmark + view-count + share buttons. Each is `material-symbol` 32dp icon + 12sp Manrope-bold count below. Bookmark uses `colorPrimary` filled, others use white.
  - **Bottom-left content overlay** (`absolute bottom-24 left-0 px-5`): 36dp circular avatar + handle + filled verified badge + multi-line caption in `BodyLarge`. (Optional audio source row with spinning music icon — skip for v1, code is `animate-spin-slow`.)
  - Top header `<header>` with translucent transparent bg and `Aura` wordmark — already in current implementation; just confirm Manrope.
- `app/src/main/java/com/aura/app/ui/feed/VideoFeedFragment.kt`:
  - Collect `viewModel.state` with `repeatOnLifecycle(STARTED)`. Branches: `Loading` → centered spinner overlay; `Empty` → empty state ("No matching creators yet — broaden your niche tags."); `Content(entries)` → submit to outer adapter; `Error(msg)` → error overlay with retry that calls `loadCreatorFeed()`.
  - Wire `loadMoreFeed()` to `ViewPager2.OnPageChangeCallback` — when `position >= adapter.itemCount - 2`, kick off `viewModel.loadMoreFeed()`.
  - Render avatar + handle in the bottom overlay using pre-resolved `entry.creatorName` + `entry.creatorProfileImageUrl` (already populated by VM step 4 in `loadCreatorFeed`).

**Reused (do NOT touch).**
- `ExoPlayerPool.kt` (audited as DONE — single-player or 8-cap pool, depending on which the latest commit shipped). Only the overlay XML and observer change.
- `CreatorRankingRepository` + `NicheMatcher` (already wired into the VM).

**Definition of Done.**
1. Sign in as a creator with tags `["fitness", "vegan"]` → first vertical page belongs to a creator with overlapping tags (verify by inspecting overlay handle + tapping into their profile to confirm tag overlap). The ordering reflects `NicheMatcher` similarity ranking.
2. Brand viewer → feed uses `industryTags` instead of niche (`VideoFeedViewModel.loadCreatorFeed` lines 59–63 already do this).
3. `Empty` state renders correct copy when `resolved.isEmpty()`.
4. `Error` state renders + retry button re-issues `loadCreatorFeed()`.
5. Vertical scroll → `loadMoreFeed()` fires near end → feed appends without duplicating creators (dedup-by-creatorId in VM).
6. Side-by-side compare with `Aura_UI_UX/aura_creator_feed/screen.png` — overlay gradients, action rail, bottom caption layout, glass bottom nav from Task 2.

---

## Cross-cutting verification (after Tasks 3–5, when resumed)

1. `./gradlew assembleDebug` clean.
2. `./gradlew lintDebug` — zero new `HardcodedText`, zero `HardcodedColor`, zero `MissingFont` warnings on touched files.
3. Smoke matrix on physical device:
   - Cold start → Login → Creator Profile populates → Edit → Save → Feed → tap creator overlay → that creator's Profile opens (Task 3 with `creatorId` arg).
   - Brand parallel run.
4. Detach `viewLifecycleOwner` collectors correctly: rotate device on each screen and verify no duplicate Snackbars / state replays (project memory `feedback_android_lifecycle_pitfalls.md`).
5. Each tab shows a real empty state on a fresh account (no portfolio, no feed matches yet).
6. Pixel-compare each screen against its `Aura_UI_UX/<folder>/screen.png` reference.

---

## Out of scope (deliberate)

- **YouTube analytics surfacing on Creator Profile.** `YouTubeRepository.fetchAndScore` exists post-merge but no Figma screen renders Engagement / Consistency / Base Creator Score. **Coordinate with the designer** to either add a section to `aura_creator_profile_unified_system` or commit to leaving the repo dormant for v1. (If the SRA grader expects §4.6 Portfolio Analytics: option A is lowest-effort = render the three scores in a simple "include_youtube_analytics.xml" card matching the existing stat-pill style, hide if no YouTube handle.)
- **Brand discovery / Browse Creators screen** (`ui/main/DiscoverFragment.kt` is still a stub) — `Aura_UI_UX/aura_brand_discovery_feed_unified` exists but is a separate vertical slice. Likely Member 2 in the team plan.
- **Send Deal flow** (SRA §4.7) — separate work-stream; the deal-workflow audit (earlier conversation in this session) covers it.
- **Recommendation engine consumption** (SRA §4.5) — backend-driven, not blocked on UI work here.
- **True backdrop-blur on the glass nav** — needs API 31+ `RenderEffect`; v1 ships the 80%-alpha tonal fallback (matches Figma at glance on dark backgrounds).
- **Animations** (`spin-slow` on audio source, button press scale-down `0.98`, gradient ring rotation on Suggested Creators) — polish, ship after vertical slices land.
