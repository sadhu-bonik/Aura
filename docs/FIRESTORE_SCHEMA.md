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
| `bio` | String | ✅ | |
| `niche` | String | ✅ | e.g. `"fashion"`, `"tech"` |
| `tags` | List\<String\> | | Discoverability keywords |
| `instagramHandle` | String | | Without `@` |
| `youtubeHandle` | String | | Without `@` |
| `tiktokHandle` | String | | Without `@` |
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
| `userId` | String | ✅ | |
| `companyName` | String | ✅ | |
| `industry` | String | ✅ | |
| `companyBio` | String | ✅ | |
| `website` | String | | |
| `logoUrl` | String | | Storage URL |
| `location` | String | | |
| `totalCampaigns` | Long | | |
| `activeDeals` | Long | | |
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
| `niche` | String | ✅ | Target creator niche |
| `budget` | Long | ✅ | USD cents |
| `deadline` | Timestamp | ✅ | |
| `status` | String | ✅ | `"active"`, `"paused"`, `"completed"` |
| `targetFollowerCount` | Long | | Minimum |
| `createdAt` | Timestamp | ✅ | |
| `updatedAt` | Timestamp | ✅ | |

---

## `deals/{dealId}`

| Field | Type | Req | Notes |
|---|---|---|---|
| `dealId` | String | ✅ | |
| `brandId` | String | ✅ | |
| `creatorId` | String | ✅ | |
| `campaignId` | String | | Optional link |
| `title` | String | ✅ | |
| `description` | String | ✅ | |
| `budget` | Long | ✅ | USD cents |
| `status` | String | ✅ | `"pending"`, `"accepted"`, `"rejected"`, `"completed"`, `"cancelled"`, `"expired"` |
| `chatUnlocked` | Boolean | ✅ | `true` iff status is `"accepted"` or `"completed"` |
| `createdAt` | Timestamp | ✅ | |
| `updatedAt` | Timestamp | ✅ | |
| `completedAt` | Timestamp | | |

**Invariants** (from `AGENTS.md` §4):
- `chatUnlocked` flips to `true` only on the `pending → accepted` transition, in the same write as the status change.
- A deal in `pending` for 7 days transitions to `expired`.

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

Writes only allowed when the referenced deal's status is `"completed"`.

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
- `reviews` writes are allowed only when `deals/{dealId}.status == "completed"`.
- `recommendations` are read-only from the client — written by backend/admin only.
