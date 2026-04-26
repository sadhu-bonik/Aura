# Send Deal: Migrate from Mock Campaigns to Firestore

## Context

The "Send Deal" bottom sheet currently renders four hardcoded `MockCampaign` items from `SendDealViewModel.kt:39-44` and the underlying `DealRepository.sendDeal()` (`DealRepository.kt:57`) hardcodes `campaignId = "direct_deal"`, so deals cannot be linked to real campaigns. Additionally, the Send Deal button in `FeedActionsOverlay.kt:45-47` only shows a Toast — the bottom sheet is never launched, so the feature is unreachable in the running app. We need to migrate the read path to Firestore (`campaigns` collection per `docs/FIRESTORE_SCHEMA.md:106-121`), thread `campaignId` through the write path, render the empty state when a brand has no active campaigns, and wire up the entry point so the feature is end-to-end verifiable.

A separate build error (`drawable/bg_drag_handle` and `drawable/bg_stat_pill` not found) is from a stale incremental cache — the drawables and layouts exist on disk and the merger metadata catalogues them, but `app/build/intermediates/incremental/debug/mergeDebugResources/merged.dir/` is missing the drawable subdirectory. A `./gradlew clean` will resolve it; no source changes are needed.

## Decisions (confirmed with user)

- **Custom deals**: Out of scope. A campaign must be selected to send a deal.
- **Card pill mapping**: Left pill = `niche` (string from schema). Right pill = formatted USD budget (e.g. `$3,500` derived from `budget / 100` cents → dollars). Drop the `deliverables` and `budgetRange` strings — they don't exist in the schema.
- **FeedActions wiring**: Included here so the feature is reachable end-to-end.
- **Stub strategy**: `CampaignRepository` always hits Firestore (no `StubState` flow). Manual verification with `USE_STUBS=true` requires a real campaign doc in the test brand's Firestore.
- **Empty state**: If the brand has no active campaigns, show centered text "You don't have any active campaigns" inside `layout_campaigns`. No "Create Campaign" navigation since that screen isn't built — a Toast direction is acceptable when the user requests one.

## Plan

### Step 1 — Fix the build error (no code change)

Run `./gradlew clean` once before the next build. Document this in the verification section so the user knows it's a one-time cache wipe, not a recurring requirement.

### Step 2 — Feature spec doc

**NEW** `docs/features/send_deal.md` — required by `AGENTS.md` §7. Sections: User story, Screens/flow (entry from FeedActionsOverlay → SendDealBottomSheet → success Toast + dismiss), Firestore reads (`campaigns where brandId == X and status == "active"`) and writes (`deals` doc with `campaignId`), Edge cases (no campaigns, brand not signed in, send fails), Invariants touched (`chatUnlocked = false` on creation per §4 of AGENTS.md).

### Step 3 — Data model

**NEW** `app/src/main/java/com/aura/app/data/model/Campaign.kt`. Mirror the schema at `docs/FIRESTORE_SCHEMA.md:106-121` exactly. Fields all default-valued for Firestore deserialization — same pattern as `app/src/main/java/com/aura/app/data/model/Deal.kt`:

```kotlin
data class Campaign(
    val campaignId: String = "",
    val brandId: String = "",
    val title: String = "",
    val description: String = "",
    val niche: String = "",
    val budget: Long = 0L,
    val deadline: Timestamp? = null,
    val status: String = "",
    val targetFollowerCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)
```

### Step 4 — Repository

**NEW** `app/src/main/java/com/aura/app/data/repository/CampaignRepository.kt`. Mirror `DealRepository.getDealsForBrand()` at `DealRepository.kt:28-36` — a Firestore `snapshots()` Flow with `.mapNotNull { toObject + copy(campaignId = it.id) }`. Constants: `Constants.COLLECTION_CAMPAIGNS` already exists.

```kotlin
fun getActiveCampaigns(brandId: String): Flow<List<Campaign>> =
    campaigns
        .whereEqualTo("brandId", brandId)
        .whereEqualTo("status", "active")
        .snapshots()
        .map { snap -> snap.documents.mapNotNull { it.toObject(Campaign::class.java)?.copy(campaignId = it.id) } }
```

### Step 5 — Modify `DealRepository.sendDeal()`

**MODIFY** `app/src/main/java/com/aura/app/data/repository/DealRepository.kt:45-67`. Add a `campaignId: String` parameter (non-default — required) and use it instead of the hardcoded `"direct_deal"` at line 57. The Deal model's `campaignId` field already exists (`Deal.kt`).

### Step 6 — ViewModel

**MODIFY** `app/src/main/java/com/aura/app/ui/feed/SendDealViewModel.kt`:

- Delete `MockCampaign` data class (lines 16-23) and `mockCampaigns` list (lines 39-44).
- Constructor takes `campaignRepository: CampaignRepository` and `dealRepository: DealRepository`.
- `Factory` constructs both with defaults (matches `AGENTS.md` §5.1 manual factory convention).
- New state shape: keep the existing `SendDealState` (Idle/Loading/Success/Error) for the **send action**, but add a separate `campaignsState: StateFlow<CampaignsState>` (sealed: `Loading`, `Empty`, `Loaded(List<Campaign>)`, `Error(String)`) for the **fetch path**, since these are independent UI concerns.
- Init block: collect `campaignRepository.getActiveCampaigns(brandId)` in `viewModelScope`. Get `brandId` from `SessionManager.getUserId()` — for brand users `brandId == userId` (consistent with how `FirebaseAuth.getInstance().currentUser?.uid` is used at `SendDealBottomSheet.kt`). Default `selectedCampaignId` to first loaded campaign's id (or empty if none).
- `sendDeal(creatorId)`: derive `brandId` internally from SessionManager, find selected campaign in current `Loaded` list, call `dealRepository.sendDeal(brandId, creatorId, campaignId, title, description, budget)`. The fragment should not pass `brandId` anymore.
- Keep the `Constants.USE_STUBS` branch on the **deal write** path (it calls `StubState.sendDeal` for in-memory deals testing) — only the campaign **fetch** is always-Firestore.

### Step 7 — Bottom sheet

**MODIFY** `app/src/main/java/com/aura/app/ui/feed/SendDealBottomSheet.kt`:

- Stop reading `viewModel.mockCampaigns`. Observe `campaignsState` and re-render `layout_campaigns` whenever it changes.
- `renderCampaigns()` now takes `List<Campaign>`. Bind:
  - `tv_campaign_title` ← `campaign.title`
  - `tv_campaign_desc` ← `campaign.description`
  - `tv_deliverables` ← `campaign.niche`
  - `tv_budget` ← `formatBudget(campaign.budget)` — helper that renders `$X,XXX` from Long cents (small private fun in fragment or extension — no need for a util module).
- States in `layout_campaigns`:
  - `Loading` → centered `ProgressBar` (programmatic; no layout change required).
  - `Empty` → centered `TextView` "You don't have any active campaigns".
  - `Error` → centered `TextView` with the message.
  - `Loaded` → existing card-inflation loop.
- Disable `btn_send_deal` whenever `campaignsState` isn't `Loaded` or `selectedCampaignId` is empty.
- Update `sendDeal` click: pass only `creatorId` (ViewModel now derives `brandId`).

### Step 8 — Wire FeedActionsOverlay

**MODIFY** `app/src/main/java/com/aura/app/ui/feed/FeedActionsOverlay.kt:45-47`. Replace the Toast with:

```kotlin
btnDeal.setOnClickListener {
    val creatorId = viewModel.currentVideoCreatorId  // verify exposure during impl
    SendDealBottomSheet.newInstance(creatorId)
        .show(fragment.parentFragmentManager, SendDealBottomSheet.TAG)
}
```

Confirm during implementation that `FeedActionsViewModel` exposes the current video's creator id; if not, add a getter (the overlay already gates on `viewModel.isBrand` at lines 31-34, so the data is on hand).

## Critical files

- `docs/features/send_deal.md` (new)
- `app/src/main/java/com/aura/app/data/model/Campaign.kt` (new)
- `app/src/main/java/com/aura/app/data/repository/CampaignRepository.kt` (new)
- `app/src/main/java/com/aura/app/data/repository/DealRepository.kt` (modify `sendDeal`)
- `app/src/main/java/com/aura/app/ui/feed/SendDealViewModel.kt` (rewrite mock layer)
- `app/src/main/java/com/aura/app/ui/feed/SendDealBottomSheet.kt` (state-driven render)
- `app/src/main/java/com/aura/app/ui/feed/FeedActionsOverlay.kt` (replace Toast with launch)
- `app/src/main/res/layout/item_campaign_card.xml` and `fragment_send_deal_bottom_sheet.xml` (no XML changes needed — bind to existing ids)

## Existing utilities to reuse

- `DealRepository.getDealsForBrand()` (`DealRepository.kt:28-36`) — snapshots-Flow template for `CampaignRepository`.
- `Deal.kt` data class — template shape for `Campaign.kt`.
- `SessionManager.getUserId()` (`SessionManager.kt:9-19`) — current user/brand id source.
- `Constants.COLLECTION_CAMPAIGNS` and `Constants.USE_STUBS` already exist.
- Existing `SendDealState` sealed class — keep for the write path.
- `item_campaign_card.xml` ids `tv_campaign_title`, `tv_campaign_desc`, `tv_deliverables`, `tv_budget`, `tv_badge_selected` — bind directly, no layout edits.

## Verification

1. **Build**: `./gradlew clean assembleDebug` — clean clears the stale resource cache; debug build must succeed with no R-class errors and no resource linking errors.
2. **Manual happy path** (`Constants.USE_STUBS = false`):
   - Sign in as a brand whose Firestore `users/{uid}` has `role == "brand"`.
   - Manually insert one `campaigns` doc in Firestore Console with `brandId == <that uid>`, `status == "active"`, populated `title`, `description`, `niche`, `budget` (e.g. `350000` for $3,500).
   - In the app, scroll to a creator video on the feed → tap the Deal action button.
   - Bottom sheet opens. The campaign appears with title, description, niche pill, and `$3,500` budget pill.
   - Tap the card → "SELECTED" badge appears. Tap "Send Deal" → success Toast, sheet dismisses.
   - In Firestore Console, verify a new `deals` doc exists with `campaignId` == the campaign just selected (not `"direct_deal"`), `status == "pending"`, `chatUnlocked == false`.
3. **Manual empty state**: Sign in as a brand with zero active campaigns. Open bottom sheet → "You don't have any active campaigns" text shown, Send Deal button disabled.
4. **Manual loading state**: With network throttling, verify the ProgressBar appears briefly before campaigns render.
5. **Regression**: Confirm existing deals dashboard still loads (`DealRepository.getDealsForBrand` unchanged), and that non-brand (creator) users do not see the Deal button (`FeedActionsOverlay.kt:31-34` gating).
