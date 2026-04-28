# Firestore Schema

Authoritative schema. Any change here must be announced in the team chat and reflected in code the same day. Collection names are camelCase plural; field names are camelCase.

---

## Collections

| Collection | Document ID | Purpose |
|---|---|---|
| `users` | Firebase Auth UID | Base record for every user |
| `creatorProfiles` | userId | Creator-specific profile |
| `brandProfiles` | userId | Brand-specific profile |
| `portfolioItems` | auto | Creator uploads (image/video) |
| `campaigns` | auto | Brand-created campaigns |
| `deals` | auto | Collaboration requests |
| `messages` | auto | Chat messages (one collection, query by `dealId`) |
| `shortlists` | auto | Brand's saved creators |
| `reviews` | auto | Post-deal reviews |
| `recommendations` | auto | System-generated creator rankings per brand (read-only from client) |

---

## `users/{userId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `userId` | String | ✅ | Matches document ID |
| `email` | String | ✅ | |
| `role` | String | ✅ | `"creator"` or `"brand"` |
| `displayName` | String | ✅ | |
| `profileImageUrl` | String | | Storage URL |
| `phone` | String | | |
| `securityQuestion` | String | | Used for password recovery (client-side UX only) |
| `securityAnswer` | String | | |
| `createdAt` | Timestamp | ✅ | |
| `lastActiveAt` | Timestamp | | |
| `isProfileComplete` | Boolean | ✅ | Onboarding flag |
| `fcmToken` | String | | For push notifications |

> **Security note:** Passwords are managed exclusively by Firebase Auth. No password, hash,
> or derived secret is ever stored in Firestore documents.

---

## `creatorProfiles/{userId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `userId` | String | ✅ | |
| `motto` | String | | Short tagline (legacy / Advanced details) |
| `headline` | String | ✅ | Primary creator headline (e.g. `"Digital Creator"`) |
| `bio` | String | ✅ | |
| `niche` | String | ✅ | e.g. `"fashion"`, `"tech"` |
| `tags` | List\<String\> | | Categories chosen at setup |
| `targetAudience` | List\<String\> | | e.g. `["Gen Z", "Millennials"]` |
| `youtubeHandle` | String | | Without `@` |
| `tiktokHandle` | String | | Without `@` |
| `portfolioLink` | String | | External portfolio URL |
| `followerCount` | Long | | Combined estimate |
| `averageRating` | Double | | 0.0–5.0, computed from reviews |
| `totalReviews` | Long | | |
| `completedDeals` | Long | | |
| `isAvailable` | Boolean | ✅ | Open to deals |
| `minimumDealBudget` | Long | | USD cents |
| `location` | String | | City, Country |
| `updatedAt` | Timestamp | ✅ | |

---

## `brandProfiles/{userId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `uid` | String | ✅ | Matches document ID |
| `brandName` | String | ✅ | Display name |
| `legalName` | String | | Legal business name (Advanced details) |
| `repName` | String | | Representative name (Advanced details) |
| `companyEmail` | String | | Business email (Advanced details) |
| `motto` | String | | |
| `bio` | String | | |
| `industryTags` | List\<String\> | ✅ | e.g. `["Fashion", "Tech"]` |
| `targetAudience` | List\<String\> | | e.g. `["Gen Z", "Professionals"]` |
| `website` | String | | |
| `linkedinUrl` | String | | Advanced details |
| `twitterHandle` | String | | Advanced details, without `@` |
| `city` | String | | |
| `state` | String | | |
| `country` | String | | |
| `firstCampaignName` | String | | Legacy onboarding cache (now stored in `campaigns/`) |
| `firstCampaignBrief` | String | | Legacy onboarding cache |
| `logoUrl` | String | | Storage URL |
| `logoPath` | String | | Storage path for rollback |
| `verificationFileUrl` | String | | Storage URL |
| `verificationFilePath` | String | | Storage path for rollback |
| `verificationFileName` | String | | Display name |
| `verificationMimeType` | String | | |
| `totalCampaigns` | Long | | |
| `activeDeals` | Long | | |
| `industry` | String | | Compatibility shim — derived from `industryTags[0]` |
| `updatedAt` | Timestamp | ✅ | |

---

## `portfolioItems/{itemId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `itemId` | String | ✅ | |
| `creatorId` | String | ✅ | |
| `title` | String | ✅ | |
| `description` | String | | |
| `mediaUrl` | String | ✅ | Storage URL |
| `mediaType` | String | ✅ | `"image"` or `"video"` |
| `thumbnailUrl` | String | | For video |
| `storagePath` | String | ✅ | Firebase Storage path for deletion/rollback |
| `mimeType` | String | | e.g. `"video/mp4"` |
| `originalFileName` | String | | Display name from user's device |
| `tags` | List\<String\> | | |
| `isPublic` | Boolean | ✅ | |
| `createdAt` | Timestamp | ✅ | |


---

## `campaigns/{campaignId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `campaignId` | String | ✅ | |
| `brandId` | String | ✅ | |
| `title` | String | ✅ | |
| `description` | String | ✅ | |
| `goals` | List\<String\> | | e.g. `["Brand Awareness", "Product Launch"]` |
| `deliverables` | List\<String\> | | e.g. `["Instagram Post", "Reel"]` |
| `budgetRange` | String | | Display label, e.g. `"$1000 - $5000"` |
| `budgetMin` | Long | | USD cents |
| `budgetMax` | Long | | USD cents |
| `timeline` | Timestamp | | Deadline date |
| `imageUrl` | String | | Storage URL (post-onboarding edit) |
| `imagePath` | String | | Storage path |
| `createdAt` | Timestamp | ✅ | |

---

## `deals/{dealId}`

A deal is one offer the brand sends to one creator under a specific campaign. The same campaign can spawn many deals — one per creator the brand targets — so deals share a `campaignId` but always differ in `creatorId`.

| Field | Type | Req | Notes |
|---|---|---|---|
| `dealId` | String | ✅ | Auto-generated Firestore doc id |
| `brandId` | String | ✅ | Sender |
| `creatorId` | String | ✅ | Recipient |
| `campaignId` | String | ✅ | Parent campaign (`campaigns/{campaignId}`) |
| `title` | String | ✅ | Copied from campaign at send time; editable while accepted |
| `description` | String | ✅ | Copied from campaign at send time; editable while accepted |
| `budget` | Long | ✅ | USD cents |
| `status` | String | ✅ | `"pending"`, `"accepted"`, `"rejected"`, `"completed"`, `"cancelled"`, `"expired"` |
| `chatUnlocked` | Boolean | ✅ | `true` after acceptance, and remains true for `"completed"` / `"cancelled"` history |
| `createdAt` | Timestamp | ✅ | |
| `updatedAt` | Timestamp | ✅ | |
| `completedAt` | Timestamp | | |

**Invariants** (from `AGENTS.md` §4):
- `chatUnlocked` flips to `true` on acceptance and remains true for readable completed/cancelled history.
- A deal in `pending` for 7 days transitions to `expired`.
- The tuple `(campaignId, brandId, creatorId)` is unique among deals in `pending` or `accepted`. Once a deal closes (`rejected`, `cancelled`, `expired`, `completed`), the brand may resend the same campaign to the same creator. Enforced client-side in `DealRepository.createDeal`.
- Cancellation is unilateral for accepted deals, but the deal remains `accepted` with `cancelRequestedBy` populated until both parties submit review ratings. Completion uses `completionRequestedBy` the same way. Only after both `creatorReviewedAt` and `brandReviewedAt` are set does the deal move to `completed` or `cancelled` history.

---

## `messages/{messageId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `messageId` | String | ✅ | |
| `dealId` | String | ✅ | Query pivot |
| `senderId` | String | ✅ | |
| `receiverId` | String | ✅ | |
| `content` | String | ✅ | |
| `mediaUrl` | String | | Optional attachment |
| `isRead` | Boolean | ✅ | |
| `sentAt` | Timestamp | ✅ | |

Query a conversation with `where("dealId", ==, <id>).orderBy("sentAt", ASCENDING)`. Writes are rejected when `deals/{dealId}.chatUnlocked == false`.

---

## `shortlists/{shortlistId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `shortlistId` | String | ✅ | |
| `brandId` | String | ✅ | |
| `creatorId` | String | ✅ | |
| `campaignId` | String | | Which campaign it's for |
| `note` | String | | Brand's internal note |
| `savedAt` | Timestamp | ✅ | |

---

## `reviews/{reviewId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `reviewId` | String | ✅ | |
| `dealId` | String | ✅ | |
| `reviewerId` | String | ✅ | |
| `revieweeId` | String | ✅ | |
| `rating` | Double | ✅ | 1.0–5.0 |
| `comment` | String | | |
| `createdAt` | Timestamp | ✅ | |

Writes only allowed when the referenced deal's status is `"completed"` / `"cancelled"`, or while an accepted deal has `completionRequestedBy` or `cancelRequestedBy` populated.

---

## `recommendations/{recommendationId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `recommendationId` | String | ✅ | |
| `brandId` | String | ✅ | |
| `creatorId` | String | ✅ | |
| `score` | Double | ✅ | 0.0–100.0 |
| `matchReasons` | List\<String\> | | e.g. `["niche_match", "high_rating"]` |
| `generatedAt` | Timestamp | ✅ | |

Read-only from the client.

---

## Storage paths

```
profileImages/{userId}/avatar.jpg
portfolioItems/{userId}/{filename}
brandLogos/{userId}/logo.jpg
campaignAssets/{campaignId}/{filename}
```

---

## Security rule summary

Full rules live in `firestore.rules` (to be written). Key principles:

- A user can read/write only their own `users/{userId}` document.
- Only brands can write to `deals` (as sender) and `shortlists`.
- Only creators can write to `portfolioItems`.
- Both parties in a deal can read/write `messages` for that deal, **only if `chatUnlocked == true`**.
- `reviews` writes are allowed only for completed/cancelled deals or accepted deals with a pending completion/cancellation review requirement.
- `recommendations` are read-only from the client — written by backend/admin only.
