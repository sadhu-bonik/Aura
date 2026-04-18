# Feature Spec: Messaging (Active Deals + Chat)

## User story

**Creator:** After a brand accepts a deal, the creator opens the Active Deals window from the bottom nav, sees the deal listed, taps it, and chats with the brand in real time. They can view the campaign details by tapping the top banner.

**Brand:** After sending a deal that gets accepted, the brand opens Active Deals, taps the deal, and chats with the creator. The banner shows the campaign name and sliding it up reveals the campaign brief, creator photo, and a shortcut to the creator's profile.

---

## Screens & flow

```
Bottom Nav → ActiveDealsFragment
                │
                └─ tap deal card ──→ ChatFragment
                                          │
                                          └─ tap top banner ──→ CampaignInfoBottomSheet
```

### ActiveDealsFragment
- Queries deals where current user is `creatorId` or `brandId` AND `chatUnlocked == true`
- Shows completed deals too (chat history, read-only indicator)
- Each row: other party's avatar, name, deal title, last message preview, timestamp
- Empty state: "No active deals yet"
- Loading / error states required

### ChatFragment
- Receives `dealId` as nav argument
- Streams messages in real time via `MessageRepository.streamMessages(dealId)`
- Top banner: other party's name + deal title. Tap → opens `CampaignInfoBottomSheet`
- Message input + send button at bottom
- Send button disabled when input is empty OR deal status is `completed`
- Marks all unread messages as read on `onResume`
- Scrolls to bottom on new message

### CampaignInfoBottomSheet (BottomSheetDialogFragment)
- Receives `dealId` as argument, loads deal + other party's profile
- Brand photo (or creator photo depending on viewer role)
- "View Brand Profile" / "View Creator Profile" button (navigates to profile fragment — stubbed until teammates merge)
- Campaign description
- Shared media section (messages with `mediaUrl` non-empty, shown as a horizontal thumbnail strip)
- Close / back button

---

## Firestore reads / writes

| Operation | Collection | Query |
|---|---|---|
| List active deals (creator) | `deals` | `creatorId == uid`, `chatUnlocked == true`, order by `updatedAt` DESC |
| List active deals (brand) | `deals` | `brandId == uid`, `chatUnlocked == true`, order by `updatedAt` DESC |
| Stream messages | `messages` | `dealId == id`, order by `sentAt` ASC |
| Send message | `messages` | new document write (guarded by `chatUnlocked` check in repo) |
| Mark read | `messages` | batch update `isRead = true` where `receiverId == uid` and `isRead == false` |
| Load deal for bottom sheet | `deals` | single document get by `dealId` |

**Indexes needed:** `messages` composite index on `dealId ASC, sentAt ASC` (add to `firestore.indexes.json`).

---

## Edge cases

| Case | Behaviour |
|---|---|
| No active deals | Empty state illustration + "No active deals yet" |
| Deal status = `completed` | Chat visible, send input hidden/disabled, banner shows "Completed" badge |
| `chatUnlocked == false` reached somehow | `sendMessage` returns failure, show snackbar error |
| Empty message input | Send button disabled — no Firestore write |
| Offline | Firestore cache serves existing messages; send fails gracefully with snackbar |
| Other party has no profile image | Glide placeholder avatar |

---

## Invariants touched (from AGENTS.md §4)

- **§4.1** — Chat is only accessible when `chatUnlocked == true`. `MessageRepository.sendMessage()` re-checks the flag before every write.
- **§4.1** — Completed deals remain readable (chat history) but not writable.
