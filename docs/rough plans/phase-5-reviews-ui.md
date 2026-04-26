# Phase 5 — Reviews UI (Execution fixes + Deal-gating)

## Context

The user has implemented the core Review feature (model, repository, ViewModel, ReviewFlow, adapter badge, triggers). Two items remain:

1. **Critical bug** — `ChatFragment` shows `ReviewFlow` but does NOT inject `ReviewViewModel by activityViewModels()`, so `markReviewPromptShown()` is never called → `DealDashboardFragment` will show a duplicate review prompt after the user already reviewed from chat.

2. **New requirement** — When the confirmer approves completion, the **initiator's** deal must NOT yet move to the Completed tab. The deal stays visible in the Active tab (read-only) until the initiator submits their review. Only then does it move to Completed for them. Both parties independently gate their own tab placement via their review submission.

---

## Architecture: deal-gating via review fields

Add two nullable timestamp fields to `Deal.kt`. The dashboard's partition logic uses these — not just `status` — to decide which tab each deal belongs to.

```
Deal.status == COMPLETED
    + currentUser's reviewedAt field is NULL  → show in Active tab  (read-only chat, review pending)
    + currentUser's reviewedAt field is SET   → show in Completed tab
```

When the user submits their review (step 2 "Done" in `ReviewFlow`), `ReviewViewModel` calls `DealRepository.markUserReviewed()` which sets the field → Firestore / StubState update → `DealDashboardViewModel` re-partitions → card moves from Active → Completed in real time.

---

## Deliverables

### §A — `Deal.kt` — add review timestamp fields

File: `app/src/main/java/com/aura/app/data/model/Deal.kt`

Add two fields (default null):
```kotlin
val creatorReviewedAt: Timestamp? = null,
val brandReviewedAt: Timestamp? = null,
```

---

### §B — `DealRepository.kt` — add `markUserReviewed`

File: `app/src/main/java/com/aura/app/data/repository/DealRepository.kt`

```kotlin
suspend fun markUserReviewed(dealId: String, userId: String): Result<Unit> = runCatching {
    if (StubSession.isStub()) {
        StubState.markUserReviewed(dealId, userId)
        return@runCatching
    }
    val deal = firestore.collection("deals").document(dealId).get().await()
        .toObject(Deal::class.java) ?: error("Deal not found")
    val field = if (deal.creatorId == userId) "creatorReviewedAt" else "brandReviewedAt"
    firestore.collection("deals").document(dealId)
        .update(field, FieldValue.serverTimestamp()).await()
}
```

---

### §C — `StubState.kt` — add `markUserReviewed`

File: `app/src/main/java/com/aura/app/utils/StubState.kt`

```kotlin
fun markUserReviewed(dealId: String, userId: String) {
    deals.value = deals.value.map { deal ->
        if (deal.id == dealId) {
            val now = Timestamp.now()
            if (deal.creatorId == userId) deal.copy(creatorReviewedAt = now)
            else deal.copy(brandReviewedAt = now)
        } else deal
    }
}
```

---

### §D — `DealDashboardViewModel.kt` — update `partition()` logic

File: `app/src/main/java/com/aura/app/ui/chat/DealDashboardViewModel.kt`

In the partition function, change how COMPLETED deals are routed:

```kotlin
// Before (incorrect — moves deal immediately to completed):
DealStatus.COMPLETED -> completed.add(deal)

// After (gate on current user's review submission):
DealStatus.COMPLETED -> {
    val hasReviewed = if (userId == deal.creatorId) deal.creatorReviewedAt != null
                      else deal.brandReviewedAt != null
    if (hasReviewed) completed.add(deal)
    else active.add(deal)   // stays in Active (read-only) until review submitted
}
```

---

### §E — `DealOfferAdapter.kt` — Active tab card for COMPLETED-unreviewed deal

File: `app/src/main/java/com/aura/app/ui/chat/DealOfferAdapter.kt`

The Active tab's `bindActive()` (or whichever method binds ACCEPTED deals) needs to also handle `DealStatus.COMPLETED` cards (the "pending review" ones). When `deal.status == COMPLETED`:
- Show a "Review Required" badge/chip in place of the normal status chip (use `chip_status_accepted` or a new `chip_review_required` drawable — tint it `colorPrimary`)
- The card remains tappable (opens read-only chat → ReviewFlow auto-appears)
- No send-message affordance needed (chat is already closed)

Also fix Issue 4 from the audit: the "★ Aura Reviewed" badge in `bindCompleted()` currently uses `chip_status_cancelled` (grey). **Change it to use `chip_status_accepted` or a new `chip_status_reviewed` drawable** so the badge reads as positive (primary/lavender tint), not as a cancelled/negative state.

---

### §F — `ReviewViewModel.kt` — call `markUserReviewed` on step 2 submit

File: `app/src/main/java/com/aura/app/ui/chat/ReviewViewModel.kt`

After `ReviewRepository.updateReviewComment()` succeeds in `submitComment()`, call:

```kotlin
dealRepository.markUserReviewed(dealId, currentUserId)
```

This triggers the re-partition and moves the deal from Active → Completed tab.

`ReviewViewModel` will need a reference to `DealRepository`. Since it's already an `AndroidViewModel`, inject it the same way as the existing repos.

`submitComment()` needs `dealId` added to its signature (it's needed for `markUserReviewed`):
```kotlin
fun submitComment(reviewId: String, dealId: String, comment: String)
```

Update `ReviewFlow.kt` to pass `dealId` when calling `submitComment()` (it already has `dealId` as an argument from `newInstance`).

---

### §G — `ChatFragment.kt` — fix duplicate-prompt bug

File: `app/src/main/java/com/aura/app/ui/chat/ChatFragment.kt`

**Bug:** `ReviewViewModel` is not injected, so `markReviewPromptShown()` is never called from chat.

**Fix — add ViewModel injection (line ~28):**
```kotlin
private val reviewViewModel: ReviewViewModel by activityViewModels()
```

**Fix — call markReviewPromptShown when showing the sheet (lines ~187–194):**
```kotlin
if (deal.status == Constants.STATUS_COMPLETED && !hasShownReviewPrompt) {
    // Guard: don't re-show if user already submitted a rating this session
    val alreadyReviewed = reviewViewModel.reviewsByDealId.value.containsKey(deal.id)
    if (!alreadyReviewed) {
        hasShownReviewPrompt = true
        reviewViewModel.markReviewPromptShown(deal.id)   // ← ADD THIS
        val otherParty = viewModel.otherUser.value ?: return@collect
        ReviewFlow.newInstance(deal.id, otherParty.userId, otherParty.displayName, otherParty.profileImageUrl)
            .show(childFragmentManager, "review_flow")
    }
}
```

The `alreadyReviewed` guard prevents re-showing `ReviewFlow` if the user has already submitted a rating (e.g., they come back to the chat from the Active tab for an unreviewed deal but have already rated in a previous session or via the dashboard trigger).

---

### §H — Smoke test

1. `./gradlew assembleDebug` — zero errors.
2. **Confirmer path:**
   - As STUB_USER_2 (brand), open chat for a deal and confirm completion.
   - `ReviewFlow` appears. Complete both steps (star + message).
   - After "Done": deal disappears from Active tab, appears in Completed tab with "★ Aura Reviewed" badge (primary tint, not grey).
3. **Initiator path (dashboard prompt):**
   - As STUB_USER_1 (creator), navigate to Deals tab.
   - stub_deal_007 appears in **Active tab** (not Completed) with "Review Required" badge.
   - `ReviewFlow` auto-appears (DashboardFragment trigger).
   - Complete review → deal moves to Completed tab.
4. **No double-prompt:** go back to Deals tab after reviewing — `ReviewFlow` does NOT appear again.
5. **Re-open chat for pending deal:** from Active tab, tap the COMPLETED-unreviewed card → read-only chat opens → `ReviewFlow` appears (ChatFragment trigger). Complete → navigating back → deal is now in Completed tab.
6. **Already-reviewed guard:** if user has already submitted rating (stub_review pre-seeded) → `ReviewFlow` does NOT appear in ChatFragment.

---

## Critical files

### Edited files
| Path | Change |
|---|---|
| `app/src/main/java/com/aura/app/data/model/Deal.kt` | Add `creatorReviewedAt` + `brandReviewedAt` fields |
| `app/src/main/java/com/aura/app/data/repository/DealRepository.kt` | Add `markUserReviewed()` |
| `app/src/main/java/com/aura/app/utils/StubState.kt` | Add `markUserReviewed()` mutation |
| `app/src/main/java/com/aura/app/ui/chat/DealDashboardViewModel.kt` | Update partition(): COMPLETED deals route based on reviewed field |
| `app/src/main/java/com/aura/app/ui/chat/DealOfferAdapter.kt` | Handle COMPLETED-unreviewed in Active tab; fix Reviewed badge color |
| `app/src/main/java/com/aura/app/ui/chat/ReviewViewModel.kt` | Call `markUserReviewed` in `submitComment`; add `dealId` to signature |
| `app/src/main/java/com/aura/app/ui/chat/ReviewFlow.kt` | Pass `dealId` through to `submitComment` |
| `app/src/main/java/com/aura/app/ui/chat/ChatFragment.kt` | Inject ReviewViewModel; call markReviewPromptShown; add alreadyReviewed guard |

### No new files needed — all changes are to existing files.

## Reused patterns
- `StubState` mutation pattern → copy from `StubState.cancelDeal()`
- `runCatching { }` + `FieldValue.serverTimestamp()` → copy from `DealRepository.cancelDeal()`
- `by activityViewModels()` already used in DealDashboardFragment — copy pattern to ChatFragment
