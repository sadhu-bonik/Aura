# Profile Setup

## User Story

A new user creates an Aura account, selects either creator or brand, fills the role-specific profile fields, and lands in the app with their profile stored in Firestore.

Creators provide categories, target audience, location, optional portfolio link, headline, bio, YouTube handle, photo, and up to three featured videos. Brands provide industries, target audience, location, website, identity details, logo, and verification document.

## Screens / Flow

Current implementation keeps the existing registration destinations stable while migrating their fields toward the final profile setup plan:

- `WelcomeFragment`
- `RoleSelectionFragment`
- Creator: `CreatorRegStep1Fragment` -> `CreatorRegStep3Fragment` -> `CreatorRegStep2Fragment`
- Brand: `BrandRegStep1Fragment` -> `BrandRegStep2Fragment` -> `BrandRegStep4Fragment`

The final Figma split into create-password and common-profile screens is planned as a follow-up navigation pass; the current change focuses on the profile data contract and role-specific fields.

## Firestore Reads / Writes

- `users/{uid}` stores base account fields: email, role, displayName, profileImageUrl, phone, security question/answer, profile completion flag, and created timestamp.
- `creatorProfiles/{uid}` stores creator setup fields: `headline`, `bio`, `motto`, `tags`, `targetAudience`, `portfolioLink`, YouTube handle, location, portfolio count, and analytics fields.
- `brandProfiles/{uid}` stores brand setup fields: `brandName`, optional advanced identity fields, `bio`, `motto`, `website`, `industryTags`, `targetAudience`, location fields, logo metadata, verification metadata, and counters.

## Edge Cases

- Required text fields show inline errors before moving forward.
- Creator setup requires at least one category, one target audience, location, headline, bio, and at least one featured video before account completion.
- Brand setup requires at least one industry, one target audience, location, brand identity fields, and a business license document before account completion.
- Draft fields live in activity-scoped ViewModels so rotation preserves typed fields and selected URIs.

## Invariants Touched

- Role is fixed at registration.
- Firestore fields use camelCase and model properties have default values.
- UI screens write through ViewModels and repositories; Fragments do not call Firebase directly.
