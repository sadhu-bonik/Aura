# Deal Dashboard + Deal History + Review Popup — Full Integration

## Context

Why this change: SRA §4.10 requires both roles (creator, brand) to see active and past deals and to leave reviews on completed ones. Commit `01c551d` ("Integrated Campaign selection, Deal Dashboard fixes, and Send Deal documentation") landed a working baseline, but three things are still missing or wrong:

1. The new Figma in `Aura_Final_UI/Feed+SendDeal+DealDashboard+UI/` is the final visual standard for these screens — current implementation is functional but visually off (no bento stats styling, no Figma-matching status pills, plain review popup).
2. Completion-without-review is silently treated as "done." Per product decision, an unreviewed completed deal must NOT move to History → Completed; it stays visible to the user (with a red exclamation) until they review. Reviews are now a *required* step of the completion lifecycle from a UX perspective (the deal data still uses the existing `creatorReviewedAt` / `brandReviewedAt` timestamps — no schema change).
3. Past-deal tap behavior in History is undefined: spec is `CANCELLED + COMPLETED → open chat (read-only)`, `EXPIRED + DECLINED → open info bottom sheet (avatar + View Profile + deal title + detail)`.

Outcome: a Figma-faithful Deal Dashboard, Deal History, and Review popup, with the review step gating the deal's visible lifecycle, and consistent past-deal interactions.

Project deadline: **2026-04-28** (one day from today). Scope keeps to what the SRA defines — no filters, no completion-handshake UI, no profile-rating widgets in this PR.

## Decisions captured from clarification

| Topic | Decision |
|---|---|
| Visual scope | Full rebuild to Figma fidelity (dashboard, history, popup) |
| Completed-but-unreviewed | Stays on Dashboard with red exclamation badge; does NOT enter History → Completed until current user reviews |
| Past tab status pills | Render `EXPIRED` / `CANCELLED` / `DECLINED` chips per Figma |
| Past tab tap behavior | `CANCELLED` and `COMPLETED` → open chat (read-only). `EXPIRED` and `REJECTED` (DECLINED) → open new sliding info bottom sheet |
| Info bottom sheet content | Avatar/profile pic, "View Profile" button, deal title, deal detail |

## Out of scope (deliberately)

- Filter button on dashboard (stays a stub — not asked for)
- Two-step completion handshake UI (`requestCompletion` / `confirmCompletion` exist in repo but aren't wired here)
- Public review display on creator/brand profiles
- Pagination, pull-to-refresh
- Removing `ActiveDealsFragment.kt` (legacy standalone) — leave for separate cleanup PR
- Per `feedback_ask_before_assuming_gaps.md`: don't restyle/rewire the bottom nav, top app bar, or campaign-info sheet beyond what this feature touches

## Implementation plan

### Phase 1 — Data + ViewModel partitioning (the lifecycle gating)

**Goal**: a `completed` deal where the current user hasn't yet stamped their `*ReviewedAt` field is treated as a "needs review" item, not as history.

Files:
- `app/src/main/java/com/aura/app/ui/chat/DealDashboardViewModel.kt`
- `app/src/main/java/com/aura/app/ui/chat/DealHistoryViewModel.kt`

Changes:
- Add an extension `Deal.needsReviewBy(uid: String, role: String): Boolean` returning `status == COMPLETED && (role == creator → creatorReviewedAt == null) || (role == brand → brandReviewedAt == null)`. Keep it next to `Deal` so both ViewModels reuse it.
- `DealDashboardViewModel.activeDeals` LiveData = `status == ACCEPTED` ∪ `needsReviewBy(me)`. Order needs-review items first.
- `DealDashboardViewModel.completedCount` (hero stat) = count of deals where current user **has** reviewed (i.e., the same set History → Completed will show). Keeps stat consistent with what the user can find in History.
- `DealHistoryViewModel.completedDeals` = `status == COMPLETED && !needsReviewBy(me)`.
- `DealHistoryViewModel.pastDeals` = `status ∈ {REJECTED, CANCELLED, EXPIRED}` (unchanged).

Reuse: `DealRepository.getDealsForCreator/Brand` already streams everything; partition is a transform on the existing flow.

### Phase 2 — Visual rebuild (XML layouts and tokens)

Per `feedback_skip_top_header.md`: skip the small avatar+title+bell sticky header in each Figma file; start from the hero section.

Per `feedback_android_lifecycle_pitfalls.md`: every list keeps an empty state; `TabLayoutMediator` detached in `onDestroyView`.

#### Tokens / drawables to add (`res/values/colors.xml`, `res/drawable/`)

- Status pill backgrounds + text colors:
  - `bg_status_pill_expired.xml` — `error/10` fill, `error/20` stroke, error text
  - `bg_status_pill_cancelled.xml` — `surface_container_high` fill, `outline_variant/20` stroke, `on_surface_variant` text
  - `bg_status_pill_declined.xml` — `outline/10` fill, `outline/20` stroke, `outline` text
  - `bg_status_pill_completed.xml` — `tertiary_container/10` fill, `tertiary/20` stroke, `tertiary` text
- `bg_review_avatar_ring.xml` — gradient ring drawable (primary → tertiary)
- `bg_btn_aura_gradient.xml` — primary→indigo gradient submit button
- `bg_red_exclamation_dot.xml` — red filled circle for the needs-review badge
- `bg_card_aura_surface.xml` — `surface_container` fill, `corner_radius_md`, no stroke (per "no-line" rule)
- New color tokens (only if missing): `auraTertiary`, `auraTertiaryContainer`, `auraSurfaceContainerLow`, `auraSurfaceContainerHighest`, `auraOutlineVariant`. Map to existing `colorPrimary`/`colorSecondary`/etc when possible; only add what's new.

#### Layouts

- `fragment_deal_dashboard.xml`
  - Hero: title `@string/title_deal_dashboard`, subtitle `@string/subtitle_deal_dashboard`, history button (top-right), filter button (kept as visual stub).
  - Bento grid: three `MaterialCardView`s (Active / Pending|Sent / Completed) with `display_lg` numbers, `label_md` labels.
  - Pill tabs: `bg_pill_segmented` background, two tabs (`Active`, `Pending`/`Sent`), gradient on selected pill.
  - `ViewPager2` for the two tabs.

- `fragment_deal_history.xml`
  - Hero: title `@string/title_deal_history`, subtitle `@string/subtitle_deal_history`, back button.
  - Pill tabs: two tabs (`Completed`, `Past Deals`).
  - `ViewPager2`.

- `item_deal_offer.xml` (reused by Active and Sent/Pending tabs)
  - Avatar 40dp, name, campaign sub-line, timestamp; **add** a 12dp red dot view (`@id/dot_needs_review`, drawable `bg_red_exclamation_dot`) shown when `deal.needsReviewBy(me)`. Existing `chip_status` View stays hidden by default.
  - Role-aware accept/reject buttons stay (Pending tab, creator only).

- New `item_deal_history_completed.xml`
  - 56dp rounded-square avatar, brand label, campaign title, chevron right. No status badge (always Completed).

- New `item_deal_past.xml`
  - Avatar with grayscale tint applied via `setColorFilter` for EXPIRED/CANCELLED/REJECTED.
  - Brand label small caps, campaign title.
  - Status pill (`@id/chip_status`) bound to one of the three pill drawables based on status.
  - Chevron muted.

- `fragment_review_flow.xml` (restyle, keep ViewFlipper logic)
  - Step 1: avatar with gradient ring, "Rate their Aura", prompt "How was your collaboration experience with {name}?", 5 sparkle (`auto_awesome`) stars, "Maybe later" skip.
  - Step 2: "Add a private note" prompt, textarea hint "Share a private note about {name}…", "Submit Aura Score" gradient button.
  - Bottom accent gradient bar 1.5dp.

- New `dialog_deal_info_sheet.xml` (BottomSheetDialogFragment)
  - Drag handle, large avatar, primary name, secondary role label, "View Profile" outlined button (navigates to brand/creator profile screen), deal title, deal description, dismiss handle.

#### Strings (`res/values/strings.xml`)

Add:
```
title_deal_dashboard = Deal Dashboard
subtitle_deal_dashboard = Track your collaborations
title_deal_history = Deal History
subtitle_deal_history = Review past collaborations
tab_dashboard_active = Active
tab_dashboard_pending = Pending
tab_dashboard_sent = Sent
tab_history_completed = Completed
tab_history_past = Past Deals
stat_active = Active Deals
stat_pending = Pending
stat_completed = Completed
status_expired = EXPIRED
status_cancelled = CANCELLED
status_declined = DECLINED
status_completed = COMPLETED
empty_active_deals = No active deals — start collaborating
empty_pending_deals_creator = No pending deal offers
empty_sent_deals_brand = No deals sent yet
empty_completed_deals = No completed reviews yet
empty_past_deals = No past deals found in your history.
review_title = Rate their Aura
review_prompt = How was your collaboration experience with %1$s?
review_comment_hint = Share a private note about %1$s…
review_submit = Submit Aura Score
review_skip = Maybe later
needs_review_a11y = Review pending
deal_info_view_profile = View Profile
```

### Phase 3 — Wiring (Fragments + adapters)

Files:
- `app/src/main/java/com/aura/app/ui/chat/DealDashboardFragment.kt`
- `app/src/main/java/com/aura/app/ui/chat/ActiveDealsTabFragment.kt`
- `app/src/main/java/com/aura/app/ui/chat/NewDealsTabFragment.kt` (the Pending/Sent tab)
- `app/src/main/java/com/aura/app/ui/chat/DealHistoryFragment.kt`
- `app/src/main/java/com/aura/app/ui/chat/CompletedDealsTabFragment.kt`
- `app/src/main/java/com/aura/app/ui/chat/PastDealsTabFragment.kt`
- `app/src/main/java/com/aura/app/ui/chat/DealOfferAdapter.kt`
- `app/src/main/java/com/aura/app/ui/chat/ReviewFlow.kt`
- New `app/src/main/java/com/aura/app/ui/chat/DealInfoBottomSheet.kt`
- `app/src/main/res/navigation/nav_graph.xml`

Behavior:

- **Dashboard → Active tab**
  - Renders accepted-or-needs-review deals (Phase 1).
  - Click on `needsReviewBy(me)` card → open `ReviewFlow` for that deal (replaces auto-prompt path for that card).
  - Click on plain accepted card → open chat (existing `action_homeContainer_to_chat`).
  - Pending/Sent unchanged behavior; tab title bound to role.

- **Dashboard auto-prompt**: keep `ReviewViewModel.pendingReviewDeal` observer in `DealDashboardFragment` (existing). It now fires on any newly-completed deal not yet reviewed; user can dismiss with "Maybe later" and the card retains its red exclamation until tapped to retry.

- **History → Completed tab**
  - Card click → open chat read-only (chat is already gated by `chatUnlocked` and `status == COMPLETED`; no new chat changes).

- **History → Past tab**
  - `DealOfferAdapter` (PAST mode) selects the right pill drawable and label per `deal.status`.
  - Click handling:
    - `CANCELLED` → open chat (read-only); reuse `action_history_to_chat`.
    - `EXPIRED` and `REJECTED` → open `DealInfoBottomSheet.newInstance(dealId)`.
  - The existing `CampaignInfoBottomSheet` is brand/campaign-focused; the new `DealInfoBottomSheet` is deal-focused (other-party avatar, View Profile, deal title + description) and is the right component per the user's spec. Don't repurpose `CampaignInfoBottomSheet`.

- **DealInfoBottomSheet**
  - Loads deal via `DealRepository.getDeal(dealId)`.
  - Loads other-party profile via existing user/brand repository methods.
  - "View Profile" navigates to existing creator/brand profile destination (look up which one in `nav_graph.xml`).
  - Read-only — no actions besides View Profile and dismiss.

- **ReviewFlow**
  - Keep two-step logic. After step 2 successfully calls `ReviewRepository.updateReviewComment` + `DealRepository.markUserReviewed`, the dashboard's flow re-emits and the card disappears from Active (because `needsReviewBy(me)` is now false), and appears in History → Completed.
  - "Maybe later" skip on step 1: dismiss without writing rating. Dashboard auto-prompt's already-shown set prevents loop; the card retains red exclamation; user can re-open via tap.

### Phase 4 — Verification

Manual end-to-end (per `project_test_users.md` test users):

1. **Creator full path**
   - Brand sends deal → creator sees in Pending tab.
   - Creator accepts → moves to Active tab (no exclamation).
   - Brand calls `completeDeal` (any path) → card now shows red exclamation in creator's Active tab. Hero "Active" count includes it; "Completed" count does not.
   - Tap card → `ReviewFlow` opens. Submit rating + comment.
   - Card disappears from Active, appears in History → Completed. "Completed" hero count increments.
2. **Brand full path** — symmetric, with `brandReviewedAt`.
3. **Cancellation**
   - Cancel an accepted deal → appears in History → Past with `CANCELLED` pill, grayscale avatar.
   - Tap → opens chat read-only.
4. **Expiration**
   - Leave a pending deal for 7 days (or set `createdAt` 8 days back via Firestore console) → on next `getDealsForCreator/Brand` call `expireIfStale` flips status to `EXPIRED`.
   - Card appears in History → Past with `EXPIRED` pill.
   - Tap → opens `DealInfoBottomSheet`. View Profile navigates correctly.
5. **Rejection (DECLINED)**
   - Creator rejects pending → status `REJECTED` → appears in brand's History → Past with `DECLINED` pill.
   - Tap → opens `DealInfoBottomSheet`.
6. **Empty states** — log out from each fixture and verify each tab's empty copy and icon render (no list = no crash).
7. **Lifecycle** — rotate device on each tab; confirm `TabLayoutMediator` detaches in `onDestroyView` (no leak, no double-attach crash).

Type-check + run app on emulator (`./gradlew assembleDebug` then run via Android Studio). Watch Logcat for the existing `CreatorRanking` tag and any new exceptions during the review write.

## Critical files (touched)

| File | Change type |
|---|---|
| `app/src/main/java/com/aura/app/data/model/Deal.kt` | Add `needsReviewBy()` extension |
| `app/src/main/java/com/aura/app/ui/chat/DealDashboardViewModel.kt` | Re-partition `activeDeals` and `completedCount` |
| `app/src/main/java/com/aura/app/ui/chat/DealHistoryViewModel.kt` | Filter `completedDeals` to reviewed-only |
| `app/src/main/java/com/aura/app/ui/chat/DealDashboardFragment.kt` | Tab title role-aware, observe new state |
| `app/src/main/java/com/aura/app/ui/chat/ActiveDealsTabFragment.kt` | Bind needs-review dot, route taps |
| `app/src/main/java/com/aura/app/ui/chat/NewDealsTabFragment.kt` | Adapter mode for Pending/Sent already exists; restyle only |
| `app/src/main/java/com/aura/app/ui/chat/CompletedDealsTabFragment.kt` | Tap → chat |
| `app/src/main/java/com/aura/app/ui/chat/PastDealsTabFragment.kt` | Pill rendering, conditional tap routing |
| `app/src/main/java/com/aura/app/ui/chat/DealOfferAdapter.kt` | Bind status pill, needs-review dot, role-aware copy |
| `app/src/main/java/com/aura/app/ui/chat/ReviewFlow.kt` | Restyle layout, keep logic; ensure `markUserReviewed` runs on submit |
| **New** `app/src/main/java/com/aura/app/ui/chat/DealInfoBottomSheet.kt` | New BottomSheetDialogFragment |
| `app/src/main/res/layout/fragment_deal_dashboard.xml` | Visual rebuild |
| `app/src/main/res/layout/fragment_deal_history.xml` | Visual rebuild |
| `app/src/main/res/layout/fragment_review_flow.xml` | Visual rebuild |
| `app/src/main/res/layout/item_deal_offer.xml` | Add red dot, restyle card |
| **New** `app/src/main/res/layout/item_deal_history_completed.xml` | New |
| **New** `app/src/main/res/layout/item_deal_past.xml` | New |
| **New** `app/src/main/res/layout/dialog_deal_info_sheet.xml` | New |
| `app/src/main/res/values/strings.xml` | Add new copy |
| `app/src/main/res/values/colors.xml` | Add Figma tokens (only what's new) |
| `app/src/main/res/drawable/` | Add pill, gradient, ring, dot drawables |
| `app/src/main/res/navigation/nav_graph.xml` | Add `dealInfoBottomSheet` destination |
| `app/src/main/java/com/aura/app/utils/Constants.kt` | (no change needed — status strings already present) |

## Reusable existing assets (don't recreate)

- `DealRepository.kt` — `getDealsForCreator/Brand`, `markUserReviewed`, `expireIfStale`, `getDeal`
- `ReviewRepository.kt` — `createReview`, `streamMyReviews`, `updateReviewComment`, `getExistingReview`
- `ReviewViewModel.kt` — `pendingReviewDeal` StateFlow, dedup of already-shown deals
- `ReviewFlow.kt` — two-step ViewFlipper structure, validation, snackbars
- `DealOfferAdapter.kt` — extend modes, don't fork
- `nav_graph.xml::action_history_to_chat` — already wired for past-cancelled tap
- `Constants.kt` — status string constants (`STATUS_*`)
- `feedback_skip_top_header.md` — skip the sticky avatar+title+bell header from Figma
- `feedback_android_lifecycle_pitfalls.md` — empty states + TabLayoutMediator cleanup
