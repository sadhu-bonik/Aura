# Send Deal Feature Specification

## User Story
As a Brand, I want to send a collaboration offer (Deal) to a Creator I discover in the video feed, using one of my active Campaigns, so that we can begin a paid partnership.

## Screens / Flow
1. **Entry Point**: In `VideoFeedFragment`, a brand views a creator's portfolio video. The brand taps the "Deal" handshake button in the `FeedActionsOverlay`.
2. **Bottom Sheet (`SendDealBottomSheet`)**: A bottom sheet slides up, displaying a loading state, followed by a list of the brand's active campaigns.
    - **Empty State**: If the brand has no active campaigns, a text message "You don't have any active campaigns" is displayed, and the "Send Deal" button is disabled.
3. **Selection**: The brand taps a campaign card to select it. The card displays a "SELECTED" badge.
4. **Action**: The brand taps "Send Deal". A loading indicator may appear briefly.
5. **Success**: A Toast notification confirms success ("Deal sent!"), and the bottom sheet dismisses itself.

## Firestore Reads and Writes

### Reads
- **Collection**: `campaigns`
- **Query**: `where("brandId", ==, <currentUserId>).where("status", ==, "active")`
- **Purpose**: To populate the bottom sheet with valid campaigns the brand can offer.

### Writes
- **Collection**: `deals`
- **Action**: Create a new document.
- **Fields Written**:
    - `dealId`: auto-generated document ID
    - `brandId`: current user ID
    - `creatorId`: target creator ID
    - `campaignId`: ID of the selected campaign
    - `title`: copied from campaign title
    - `description`: copied from campaign description
    - `budget`: copied from campaign budget
    - `status`: `"pending"`
    - `chatUnlocked`: `false`
    - `createdAt`: `Timestamp.now()`
    - `updatedAt`: `Timestamp.now()`

## Edge Cases
- **No Active Campaigns**: Handled via Empty State text in the bottom sheet.
- **Network Failure**: Fetch or Write failures display an error message in the bottom sheet UI (using `SendDealState.Error`).
- **Brand Not Signed In**: `SessionManager.getUserId()` returns null; handled by ViewModel emitting an Error state.
- **Stale Cache / Offline**: Handled by Firestore's offline capabilities; snapshot flows will return cached active campaigns.

## Invariants Touched
- **Invariant 1 (Chat Locked)**: `deals/{id}.chatUnlocked` MUST be initialized to `false`. It only flips to true upon the `pending -> accepted` transition.
- **Invariant 4 (Role Check)**: The FeedActionsOverlay button is only visible/enabled if `isBrand == true`.
