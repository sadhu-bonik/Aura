# Deal Lifecycle — Phase 3.5 finish-line pass (revised)

## Context

The prior Phase 3 plan merged from `develop` and — combined with the user's manual edits on this branch — landed most of the lifecycle work: the data models, repository methods, StubState helpers, `ChatViewModel` completion state, the completion action bar + conversation-closed footer, the stacked Cancel/Complete buttons on the info sheet, inline accept/reject circles on New Deals cards, per-status chip routing on the Past tab, and the `MaterialToolbar` on `DealHistoryFragment` are all in place and working.

The user's deliberate design decisions (confirmed 2026-04-18) that scope out work I initially misread as gaps:

- **Cancel + complete-confirm do NOT emit in-chat system-message dividers.** The persistent "CONVERSATION CLOSED" banner that replaces the input bar already conveys the terminal state — an in-chat "DEAL COMPLETED" row would be redundant noise.
- **Completion-request-triggered does NOT emit a system message either.** The action-bar pop-up on Party B's chat already prompts them to decide; nothing needs to be said in the timeline for Party A. Only the server-side flag (`completionRequestedBy`) needs to know.
- **No need to seed COMPLETED or CANCELLED stub deals.** The existing EXPIRED + REJECTED stubs are sufficient; cancelled/completed states get verified by running the cancel/complete flows live.
- **No green COMPLETED chip on Completed-tab cards.** The tab label carries the status.
- **`ChatViewModel.updateCompletionState`'s `StubSession.displayName()` is a harmless placeholder** — the `completion_prompt` string doesn't render the name, and there's no planned UI that would. Leave it.

That leaves three real pieces of work:

1. **Wire `sendSystemMessage` for completion-request DECLINE only.** Currently the decline path mutates state but emits nothing; the chat timeline should show a small-print "COMPLETION REQUEST DECLINED" divider so Party A sees their request was rejected. Previously this text was being rendered inside a regular user bubble — wrong. Routing it through `sendSystemMessage` picks up `MessageAdapter`'s VIEW_SYSTEM style automatically.

2. **Show the actual canceller's name in cancel-state copy.** Both `conversation_closed_cancelled_by_other` and `CampaignInfoBottomSheet`'s reason line currently say "cancelled by the counterparty" / "cancelled by other party." The intended behavior: when I cancelled, show "Cancelled by you"; when they cancelled, show "Cancelled by {their displayName}."

3. **Hook 7-day expiry into dashboard load.** `DealRepository.expireIfStale(deal)` exists (`DealRepository.kt:149-162`) but is private and never called. `DealDashboardViewModel.partition` iterates every deal on each flow emission — perfect sweep point. Promote `expireIfStale` to public, add stub-mode equivalent, call during partition so PENDING deals older than 7 days flip to EXPIRED before bucketing.

Plus a build + smoke test pass before declaring Phase 3 shippable.

## Deliverables

### 1. Emit a system divider only on completion-request DECLINE

Edit `ChatViewModel.respondToCompletion` (`ChatViewModel.kt:169-185`):

```kotlin
fun respondToCompletion(accepted: Boolean) {
    viewModelScope.launch {
        if (accepted) {
            if (Constants.USE_STUBS) {
                StubState.updateDealStatus(dealId, Constants.STATUS_COMPLETED, chatUnlocked = true)
            } else {
                dealRepository.confirmCompletion(dealId)
            }
            // No system message — closed banner communicates completion.
        } else {
            if (Constants.USE_STUBS) {
                StubState.clearCompletionRequest(dealId)
            } else {
                dealRepository.declineCompletion(dealId)
            }
            messageRepository.sendSystemMessage(
                dealId,
                getApplication<Application>().getString(R.string.system_msg_completion_declined)
            )
        }
    }
}
```

- Fire-and-forget: no snackbar on `sendSystemMessage` failure; the decline itself already succeeded.
- Ordering: state mutation first (so if it fails, no ghost divider), then the system-message write.
- No changes needed in `MessageRepository`, `MessageAdapter`, or `item_message_system.xml` — all still present and working.
- No other transitions (cancel, complete-confirm, completion-request) call `sendSystemMessage`.

### 2. Render the canceller's name in cancel-state copy

**String change (`app/src/main/res/values/strings.xml`):**

Change `conversation_closed_cancelled_by_other` to include a name placeholder:
```xml
<string name="conversation_closed_cancelled_by_other">You can no longer send messages to this thread because %1$s cancelled this deal.</string>
```
`conversation_closed_cancelled_by_self` and `conversation_closed_completed` stay as-is.

**ChatFragment wiring (`ChatFragment.kt:173-183`):**

Replace the when-branch for "cancelled by other":
```kotlin
val otherName = viewModel.otherUser.value?.displayName.orEmpty()
val subtitle = when {
    deal.status == Constants.STATUS_COMPLETED ->
        getString(R.string.conversation_closed_completed)
    deal.cancelledBy == myId ->
        getString(R.string.conversation_closed_cancelled_by_self)
    else ->
        getString(R.string.conversation_closed_cancelled_by_other, otherName)
}
binding.includeConversationClosed.tvClosedSubtitle.text = subtitle
```

`_otherUser` is already populated by the time `deal` emits a CANCELLED state (both fire off the same `load()` path and the deal observer reads `viewModel.otherUser.value` synchronously). If it's ever null, `.orEmpty()` + the string still reads cleanly (just "because cancelled this deal.").

**CampaignInfoBottomSheet wiring (`CampaignInfoBottomSheet.kt:175-184`):**

The Kotlin-side template for `tvReasonText` currently hardcodes "other party." Swap to the actual name:
```kotlin
Constants.STATUS_CANCELLED -> {
    val byMe = deal.cancelledBy == myId
    val cancellerName = viewModel.otherParty.value?.displayName.orEmpty()
    val actor = if (byMe) "you" else cancellerName.ifBlank { "other party" }
    val baseText = "Cancelled by $actor"
    if (deal.cancelReason.isNotBlank()) {
        "$baseText because \"${deal.cancelReason}\""
    } else {
        "$baseText."
    }
}
```

The `.ifBlank { "other party" }` keeps the fallback for the rare case `otherParty` hasn't loaded yet. Also updates the REJECTED branch to use the same pattern (`val actor = if (byMe) "you" else cancellerName.ifBlank { "other party" }`) for consistency — single helper inlined twice.

String literals here stay in Kotlin (existing convention in this file; extracting to `strings.xml` is cosmetic polish, deferred).

### 3. Wire 7-day expiry sweep on dashboard load

**Promote `expireIfStale` (`DealRepository.kt:149`):** change `private fun expireIfStale(deal: Deal): Deal` to `fun expireIfStale(deal: Deal): Deal` — no body changes needed. It already no-ops for non-PENDING deals, guards on `createdAt`, and fires a Firestore update only when stale.

**Add a stub-mode equivalent in `StubState`:**
```kotlin
fun expireIfStale(deal: Deal): Deal {
    if (deal.status != Constants.STATUS_PENDING) return deal
    val createdAt = deal.createdAt?.toDate() ?: return deal
    val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
    if (System.currentTimeMillis() - createdAt.time < sevenDaysMs) return deal
    updateDealStatus(deal.dealId, Constants.STATUS_EXPIRED, chatUnlocked = false)
    return deal.copy(status = Constants.STATUS_EXPIRED)
}
```
This mirrors the repo version exactly and goes through the existing `updateDealStatus` so the flow emits the new value downstream.

**Call it from `DealDashboardViewModel.partition` (`DealDashboardViewModel.kt:119`):**

```kotlin
deals.forEach { raw ->
    val deal = if (Constants.USE_STUBS) {
        StubState.expireIfStale(raw)
    } else {
        dealRepository.expireIfStale(raw)
    }
    val otherUserId = if (role == Constants.ROLE_CREATOR) deal.brandId else deal.creatorId
    val otherUser = userRepository.getUserLite(otherUserId)
    when { /* existing buckets */ }
}
```

**Feedback-loop risk & mitigation:** In stub mode, `updateDealStatus` mutates `dealsFlow`, which re-triggers `partition`. On the second pass the deal is already EXPIRED so `expireIfStale` short-circuits. Idempotent — no infinite loop. No extra `distinctUntilChanged` wrapping needed.

**Live-mode note:** `DealRepository.expireIfStale` fires-and-forgets the Firestore write, so there's a small window where the cached `deal` object reports EXPIRED while Firestore is still PENDING. The next Firestore snapshot corrects it. Acceptable.

**Visual verification uses the existing `stub_deal_005`** (EXPIRED already, `createdAt = ts(15000)` minutes ago ≈ 10 days). No new stub data needed — the sweep's *behavior* is covered by unit-style reasoning (it no-ops on already-EXPIRED deals, and would flip a hypothetical freshly-stale deal).

### 4. Build + smoke test

1. `./gradlew assembleDebug` clean.
2. Launch in stub mode (`Constants.USE_STUBS = true`).
3. Exercise this matrix — should match every Stitch reference:

| Step | Expected |
|---|---|
| Alice → Active → Summer Collection Drop → banner → sheet → Cancel Deal → "Budget shifted." | Sheet dismisses; chat closed banner subtitle reads "...because you cancelled this deal."; **no in-chat system divider**; deal moves to Past → Cancelled |
| Cycle to Nova → Summer Collection Drop in Past → tap row | Chat opens read-only; closed banner subtitle reads "...because Alice Chen cancelled this deal." (real name, not "counterparty") |
| Reopen sheet from that cancelled chat | Reason line reads "Cancelled by Alice Chen because 'Budget shifted.'" |
| Alice → Active → Tech Gadget Review → sheet → Complete Deal | Sheet dismisses; **no in-chat system divider**; sheet reopens with "Waiting for response..." |
| Cycle to Apex → same chat → action-bar pop-up visible | Pop-up prompt is generic ("The other party wants to complete...") — not named |
| Apex taps **No** | Action bar disappears; **small-print system divider "COMPLETION REQUEST DECLINED" appears in chat history**; deal stays ACCEPTED; Alice's sheet re-enables Cancel/Complete |
| Apex taps **Yes** (separate run) | Action bar disappears; **no in-chat system divider**; closed banner takes over with "deal has been completed" subtitle |
| Alice → Dashboard (fresh app start) | PENDING deals newer than 7 days stay in New Deals; `stub_deal_005` (10 days old EXPIRED) stays in Past. If any real PENDING deal on the branch crosses the 7-day line, it auto-flips on load |

Live-mode sanity (flip `USE_STUBS = false` once): accept → complete-request → decline round-trip on Firestore; verify exactly one `isSystem = true` message lands in the `messages` collection on the decline, none on the other three transitions; dashboard opens without any crash in the sweep path.

## Critical files

### Edits
| Purpose | Path |
|---|---|
| Add `sendSystemMessage` call on decline path only | `app/src/main/java/com/aura/app/ui/chat/ChatViewModel.kt` |
| Read canceller name from `otherUser` for closed-banner subtitle | `app/src/main/java/com/aura/app/ui/chat/ChatFragment.kt` |
| Render canceller name in info-sheet reason line (CANCELLED + REJECTED branches) | `app/src/main/java/com/aura/app/ui/chat/CampaignInfoBottomSheet.kt` |
| Add `%1$s` name placeholder to closed-by-other string | `app/src/main/res/values/strings.xml` |
| Promote `expireIfStale` to public | `app/src/main/java/com/aura/app/data/repository/DealRepository.kt` |
| Add `expireIfStale(deal)` stub-mode twin | `app/src/main/java/com/aura/app/utils/StubState.kt` |
| Call `expireIfStale` once per deal during `partition` | `app/src/main/java/com/aura/app/ui/chat/DealDashboardViewModel.kt` |

### No new files.

## Reused / already-in-place (do NOT re-implement)

- `MessageRepository.sendSystemMessage` — batched write at `MessageRepository.kt:155`; handles `isSystem = true`, deal `lastMessageText`/`lastMessageTime`/`updatedAt` update, no unread-counter increment.
- `MessageAdapter` VIEW_SYSTEM rendering at `MessageAdapter.kt:143-182` + `item_message_system.xml` — small-print centered divider with side lines, exactly the style the user wants for the decline message.
- `StubState.updateDealStatus` — already clears `completionRequestedBy` on COMPLETED/CANCELLED and emits on `dealsFlow`.
- `DealRepository.cancelDeal(id, by, reason)`, `requestCompletion`, `confirmCompletion`, `declineCompletion` — all in place.
- `CampaignInfoBottomSheet` waiting-state gating matrix (`CampaignInfoBottomSheet.kt:131-158`) — already correct; no changes.
- `ChatFragment` closed-footer + completion-bar observer plumbing — structure stays; only the subtitle string changes.
- `DealHistoryFragment` + `fragment_deal_history.xml` MaterialToolbar with centered title + back nav + menu stub — already refactored.
- Existing EXPIRED + REJECTED stubs (`stub_deal_005`, `stub_deal_006`) — enough to verify Past tab rendering.
- `conversation_closed_cancelled_by_self` and `conversation_closed_completed` strings — unchanged.

## Explicitly out of scope

- In-chat system dividers for cancel, complete-confirm, completion-request-triggered (deliberate design: closed banner + action-bar pop-up already convey those states).
- Green COMPLETED chip on Completed-tab cards (tab label is enough).
- Fix to `ChatViewModel.updateCompletionState`'s `StubSession.displayName()` placeholder (not displayed anywhere; leave).
- New COMPLETED / CANCELLED stub deals (flows produce them live).
- Bottom navigation bar refactor — Phase 4.
- Gradient Complete button, review-after-completion, brand retract offer, FCM push, scheduled worker for expiry sweep when app is closed, wiring chat header search / more_vert.
- Extracting `CampaignInfoBottomSheet`'s Kotlin-side closed-state copy to `strings.xml` — cosmetic polish.

## Risks & notes

- **`sendSystemMessage` failure on decline:** fire-and-forget; log only. The state mutation succeeded, and retrying the message write without de-dup logic could post duplicate dividers on network flakiness.
- **`otherUser.value` nullability in closed subtitle:** `.orEmpty()` + string with `%1$s` still reads grammatically if name is blank ("You can no longer send messages to this thread because  cancelled this deal."). Minor cosmetic issue only during a brief load race; not worth gating on.
- **`AndroidViewModel` already in place** for `ChatViewModel` (line 46); no VM-type migration needed for the decline-system-message wiring.
- **Stale sweep is cheap:** `expireIfStale` runs only for PENDING deals, does a date comparison, and early-exits for everything else. Running it per-deal on every `partition` pass is O(n) with tiny constants.
- **Branch just merged `develop`:** last commit is literally "resolved all merge conflicts from develop branch tested". Build-and-smoke is non-negotiable before calling this done.
