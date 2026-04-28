# Profile Setup + Campaign Setup — Full Integration Plan

## Context

Why this change: SRA §4.1–§4.4 + §4.7 require email/password sign-up, role selection, role-specific profile, and (for brands) campaign creation. The repo already has a working 4-step creator registration and 5-step brand registration backed by Firebase Auth + Firestore (commit `3fc9709 "Ui for login/profile/campaign"`), but the new Figma in `Aura_Final_UI/Login+profileSetup_UI/` defines a different flow shape: shorter steps, a separate password screen, a common pre-role screen (name + phone + security Q&A), and richer campaign fields (goals chips, budget dropdown, timeline date, deliverables chips).

Outcome: a Figma-faithful sign-up flow with `aura_create_password → common_profile_setup → role_choosing → role-specific 2-screen setup (+ optional Advanced details) → home`. For brand, the final onboarding step is the new campaign setup. Existing fields not in the Figma main flow (creator motto, brand legal name / rep / company email / LinkedIn / Twitter) are preserved on a single optional "Advanced details" screen so no data is lost. A new `Campaign` model + repository replaces the current placeholder `firstCampaignName` / `firstCampaignBrief` fields on `BrandProfile`.

Project deadline: **2026-04-28**. Scope is the eight in-scope screens plus the supporting data layer.

## Decisions captured from clarification

| Topic | Decision |
|---|---|
| Flow shape | Collapse to Figma's 2-screen-per-role visible flow, with an optional **Advanced details** step before finish (no data lost) |
| Creator motto | Keep on Advanced details screen |
| Brand legal name / rep / company email / LinkedIn / Twitter | Keep on Advanced details screen (optional) |
| Campaign | Extend brand onboarding's final step — replace `BrandRegStep5` with the Figma `campaign_setup` screen (goals chips, budget dropdown, timeline, deliverables) |
| Account creation split | Split per Figma: `aura_create_password` → `common_profile_setup` → `role_choosing` |

## Out of scope (deliberately)

- Post-onboarding "Create Campaign" UI on brand home (the user picked **Extend brand registration final step**, not "Both")
- Post-onboarding profile-completion fix-up flow for incomplete creator portfolios (separate work)
- Edit Profile screens (`profile+editProfile_UI/` folder) — those are a separate plan
- Removing fields from `CreatorProfile` / `BrandProfile` Firestore docs. We keep the existing schema; the Advanced screen stays as the write site for the optional fields. No back-fill, no migration.
- Per `feedback_ask_before_assuming_gaps.md`: don't drop existing fields just because the Figma main flow hides them — they live on Advanced

## Final flow

```
WelcomeFragment
   └─▶ AuraCreatePasswordFragment        (new — email + password)
         └─▶ CommonProfileSetupFragment  (new — first name, last name, phone, security Q&A)
               └─▶ RoleSelectionFragment (restyle)
                     ├─▶ Creator path
                     │     └─▶ CreatorProfileSetup1Fragment   (categories, target audience, location, portfolio link)
                     │           └─▶ CreatorProfileSetup2Fragment (photo, headline, bio, youtube, 3 featured videos)
                     │                 └─▶ CreatorAdvancedFragment  (optional motto, skip → finish)
                     │                       └─▶ HomeContainer
                     └─▶ Brand path
                           └─▶ BrandProfileSetup1Fragment       (industry, target audience, location, website)
                                 └─▶ BrandProfileSetup2Fragment (brand name, verification doc, website)
                                       └─▶ BrandAdvancedFragment  (optional legal name, rep, company email, LinkedIn, twitter, skip → next)
                                             └─▶ CampaignSetupFragment (title, description, goals chips, budget, timeline, deliverables)
                                                   └─▶ HomeContainer
```

The Advanced screen has **Skip** and **Save & Continue** buttons. Both progress to the next destination — Skip just doesn't write the optional fields.

Email + password are entered on `AuraCreatePasswordFragment`. The Firebase Auth user is **not created until the final finish step** (creator's photo screen → finish, or brand's campaign setup → finish), so partial sign-ups don't pollute Firestore. Until then, everything is held in the `RegistrationViewModel` / `BrandRegistrationViewModel` draft (mirrors current behavior).

## Implementation plan

### Phase 1 — Data model + repository changes

Files (modify):
- `app/src/main/java/com/aura/app/data/model/CreatorProfile.kt`
- `app/src/main/java/com/aura/app/data/model/BrandProfile.kt`

Add fields (each defaulted for Firestore `toObject()`):
- `CreatorProfile.targetAudience: List<String> = emptyList()`
- `CreatorProfile.portfolioLink: String = ""`
- `CreatorProfile.headline: String = ""` *(currently using `motto` — keep both; `headline` is the new Figma "Headline" field, `motto` becomes the legacy/optional Advanced field)*
- `BrandProfile.targetAudience: List<String> = emptyList()`
- `BrandProfile.brandWebsite: String = ""` *(if not already present — verify on `BrandProfile`)*

Files (new):
- `app/src/main/java/com/aura/app/data/model/Campaign.kt`
  ```
  data class Campaign(
      val campaignId: String = "",
      val brandId: String = "",
      val title: String = "",
      val description: String = "",
      val goals: List<String> = emptyList(),
      val budgetRange: String = "",
      val timeline: com.google.firebase.Timestamp? = null,
      val deliverables: List<String> = emptyList(),
      val createdAt: com.google.firebase.Timestamp? = null,
      val updatedAt: com.google.firebase.Timestamp? = null,
      val isActive: Boolean = true
  )
  ```
- `app/src/main/java/com/aura/app/data/repository/CampaignRepository.kt`
  - `createCampaign(campaign: Campaign): Result<String>` — writes to `campaigns/{auto}`, returns id
  - `getCampaignsForBrand(brandId: String): Flow<List<Campaign>>` — used by Send Deal flow already; keep query consistent with existing send-deal references
  - `getCampaign(campaignId: String): Result<Campaign>`
  - `setActive(campaignId: String, active: Boolean): Result<Unit>` *(stub for future)*

Update Firestore schema doc:
- `docs/FIRESTORE_SCHEMA.md` — document `campaigns/{campaignId}` collection if not yet documented; add `targetAudience` field on creator + brand profiles. Per AGENTS.md §4.6, all fields use camelCase + defaults.

Existing references to honor:
- Send Deal flow already takes a `campaignId` (per `docs/features/send_deal.md`); confirm it now reads from the new `campaigns` collection rather than `BrandProfile.firstCampaignName`.

### Phase 2 — New shared Constants

File (new):
- `app/src/main/java/com/aura/app/utils/BrandIndustryTags.kt`
  - List of industries from Figma: `Fashion, Tech, Beauty, Fitness, Food, Travel, Gaming` (centralize what is currently hardcoded inside `BrandRegStep4Fragment`).
- `app/src/main/java/com/aura/app/utils/TargetAudienceTags.kt`
  - `Gen Z, Millennials, Professionals, Students`
- `app/src/main/java/com/aura/app/utils/CampaignGoals.kt`
  - `Brand Awareness, Product Launch, Sales Conversion, Social Media Growth`
- `app/src/main/java/com/aura/app/utils/CampaignDeliverables.kt`
  - `Instagram Post, Reel, Story, YouTube Video`
- `app/src/main/java/com/aura/app/utils/BudgetRanges.kt`
  - `$1000 - $5000`, `$5000 - $10,000`, `$10,000+`

`CreatorNicheTags.kt` already exists and covers Figma's creator categories (`Fashion, Tech, Fitness, Travel, Food, Gaming, Lifestyle`) — reuse it.

### Phase 3 — Layouts (visual rebuild)

Per `feedback_skip_top_header.md`: skip the small avatar+title+bell sticky header from each Figma file; start at the hero. Per `feedback_android_lifecycle_pitfalls.md`: every list/chip layout still gets an empty state (the chip groups are pre-populated, but Featured Videos grid needs an empty-slot placeholder).

#### Drawables / tokens to add (`res/drawable/`, `res/values/colors.xml`)

- `bg_input_pill.xml` — surface_container_low fill, 28dp corner radius
- `bg_input_pill_error.xml` — error_dim/30 stroke, 28dp corner radius
- `bg_chip_selected.xml` — surface_container_highest fill, primary stroke 1dp, primary text
- `bg_chip_unselected.xml` — surface_container_low fill, no stroke, on_surface_variant text
- `bg_btn_aura_gradient.xml` — already in dashboard plan; reuse
- `bg_btn_outlined_pill.xml` — outline_variant 1dp stroke, 28dp radius
- `bg_video_slot_dashed.xml` — outline_variant/30 dashed stroke, surface_container_highest fill, 16dp corner
- `bg_role_card_selected.xml` — primary 4dp stroke, surface fill, glow shadow
- `bg_role_card_unselected.xml` — outline_variant 1dp stroke, surface fill
- New tokens (only if missing): `auraErrorDim` (#c44b5f), `auraSurfaceContainerLow` (#141318), `auraSurfaceContainerHighest` (#27252d), `auraOutlineVariant` (#494650). Map to existing `colorPrimary`/`colorError` etc. where they already match.

#### Layouts (new + restyle)

| Layout file | New / Modify | Maps to Figma | Notes |
|---|---|---|---|
| `fragment_welcome.xml` | Modify (restyle) | `aura_welcomeScreen` | Existing — restyle to match |
| `fragment_create_password.xml` | **New** | `aura_create_password` | Email, password, confirm password fields |
| `fragment_common_profile_setup.xml` | **New** | `Common_profileSetup_beforeRoleChoosing` | First name, last name, phone, security question dropdown, security answer |
| `fragment_role_selection.xml` | Modify | `RoleChoosing` | Two role cards with selected/unselected drawables |
| `fragment_creator_profile_setup_1.xml` | Modify (was `fragment_creator_reg_step3.xml`; rename or repurpose) | `Creator_profilesetup_1stScreen` | Categories chips, Target Audience chips, Location, Portfolio Link |
| `fragment_creator_profile_setup_2.xml` | Modify (was `fragment_creator_reg_step2.xml` + parts of step4) | `Creator_profileSetup_2ndScreen` | Avatar upload, Headline, Bio (textarea), YouTube, 3 Featured Videos grid |
| `fragment_creator_advanced_details.xml` | **New** | (no Figma — internal) | Motto (optional), Skip / Save & Continue |
| `fragment_brand_profile_setup_1.xml` | Modify (was `fragment_brand_reg_step4.xml`) | `Brand_profile_setup_1stScreen` | Industry chips, Target Audience chips, Location, Brand Website |
| `fragment_brand_profile_setup_2.xml` | Modify (was `fragment_brand_reg_step2.xml`) | `Brand_profileSetup_2ndScreen` | Avatar upload (logo), Brand Name, Verification Document tile, Brand Website |
| `fragment_brand_advanced_details.xml` | **New** (was parts of step3) | (no Figma — internal) | Legal Name, Rep Name, Company Email, LinkedIn URL, Twitter handle (all optional). Skip / Save & Continue |
| `fragment_campaign_setup.xml` | Modify (was `fragment_brand_reg_step5.xml`) | `campaign_setup` | Campaign Title, Description (textarea), Goals chips, Budget dropdown, Timeline date input, Deliverables chips, Save Campaign |
| `fragment_creator_reg_step1.xml` | **Delete** (after migration) | — | Replaced by aura_create_password + common_profile_setup |
| `fragment_creator_reg_step4.xml` | **Delete** | — | Featured Videos folded into setup_2 |
| `fragment_brand_reg_step1.xml` | **Delete** | — | Replaced |
| `fragment_brand_reg_step3.xml` | **Delete** (fields → Advanced) | — | |

Wrong-field error state for Creator setup_2 (`Creator_profileSetup_EnteringWrongField_2ndScreen`) is **state on the existing layout** — implemented via `TextInputLayout.error` + the `bg_input_pill_error.xml` drawable when the ViewModel publishes a validation error. No separate layout file needed.

#### Strings (`res/values/strings.xml`)

Add the verbatim copy from the Figma audit (use the strings already extracted in the design report). Key new groups:
- `password_create_*`
- `common_profile_*`
- `role_*` (already partial — supplement)
- `creator_setup_*` (replace existing `creator_reg_step{2,3,4}_*` keys)
- `brand_setup_*` (new — brand strings are missing today)
- `advanced_details_*`
- `campaign_*`
- Validation messages: `error_first_name_required`, `error_phone_invalid`, `error_security_question_required`, `error_categories_min_one`, `error_target_audience_required`, `error_location_required`, `error_headline_required`, `error_bio_required`, `error_brand_name_required`, `error_verification_required`, `error_campaign_title_required`, `error_campaign_description_required`, `error_campaign_goals_required`, `error_budget_required`, `error_timeline_required`, `error_deliverables_required`

Buttons: `button_next`, `button_finish_setup`, `button_save_exit`, `button_save_campaign`, `button_skip_advanced`, `button_create_account`, `button_cancel`.

### Phase 4 — Fragments + ViewModels

Files (new):
- `app/src/main/java/com/aura/app/ui/auth/AuraCreatePasswordFragment.kt`
- `app/src/main/java/com/aura/app/ui/auth/CommonProfileSetupFragment.kt`
- `app/src/main/java/com/aura/app/ui/auth/creator/CreatorProfileSetup1Fragment.kt` *(rename / repurpose existing)*
- `app/src/main/java/com/aura/app/ui/auth/creator/CreatorProfileSetup2Fragment.kt` *(rename / repurpose existing)*
- `app/src/main/java/com/aura/app/ui/auth/creator/CreatorAdvancedFragment.kt`
- `app/src/main/java/com/aura/app/ui/auth/brand/BrandProfileSetup1Fragment.kt`
- `app/src/main/java/com/aura/app/ui/auth/brand/BrandProfileSetup2Fragment.kt`
- `app/src/main/java/com/aura/app/ui/auth/brand/BrandAdvancedFragment.kt`
- `app/src/main/java/com/aura/app/ui/auth/brand/CampaignSetupFragment.kt`

Files (modify):
- `app/src/main/java/com/aura/app/ui/auth/RegistrationViewModel.kt`
- `app/src/main/java/com/aura/app/ui/auth/brand/BrandRegistrationViewModel.kt`
- `app/src/main/java/com/aura/app/data/repository/BrandRegistrationRepository.kt`

Behavior:

- **AuraCreatePasswordFragment**: collects email + password + confirm. Validates email format and password strength (`8+ chars, 1 uppercase, 1 number` — same rules as existing step1). Stores in shared draft (`activityViewModels` on RegistrationViewModel / BrandRegistrationViewModel — but at this point we don't know the role yet, so use a shared `OnboardingDraftViewModel` — see note below).

- **CommonProfileSetupFragment**: first name, last name, phone, security question (dropdown bound to `R.array.security_questions`), security answer. The wrong-field validation in the Figma is on phone — implement phone format check via `android.telephony.PhoneNumberUtils`. Stores in shared draft.

- **OnboardingDraftViewModel** (new, scoped to nav graph): a single ViewModel keeping the cross-role draft (email, password, name, phone, security). When the user picks a role on `RoleSelectionFragment`, the data is copied into either `RegistrationViewModel` or `BrandRegistrationViewModel`. This avoids the awkward "which ViewModel do I write to before role is known" problem and prevents clearing data on back-press.

- **RoleSelectionFragment**: existing logic — write `role` into draft and route to the role-specific setup_1.

- **Creator setup_1** → categories chips (CreatorNicheTags), target audience chips (TargetAudienceTags), location (text), portfolio link (optional URL). Validate ≥1 category and ≥1 audience, location non-empty. Save into RegistrationViewModel draft.

- **Creator setup_2** → photo upload (Glide preview, lifecycle-aware launcher), headline, bio (with 200-char counter), YouTube URL, 3-slot featured videos grid (mirror existing portfolio video upload pattern from `CreatorRegStep4Fragment` but capped at 3). Validate headline non-empty, bio non-empty (per Figma error variant). Save into draft.

- **CreatorAdvancedFragment** → motto (optional, single line). Skip → directly call `RegistrationViewModel.completeRegistration()`. Save & Continue → write motto to draft, then call complete. Either way, finish lands on `homeContainerFragment` with `popUpTo welcomeFragment`.

- **Brand setup_1** → industry chips (BrandIndustryTags), target audience chips, location, brand website. Validate ≥1 industry, ≥1 audience, location non-empty.

- **Brand setup_2** → logo upload, brand name (required), verification document tile (file picker → ACTION_OPEN_DOCUMENT, mime types: `application/pdf, image/*, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document`), brand website. Validate brand name + verification doc.

- **BrandAdvancedFragment** → legal name, rep name, company email, LinkedIn URL, Twitter handle (all optional). Skip → straight to campaign setup. Save & Continue → write to draft, advance.

- **CampaignSetupFragment** → title (required), description (required), goals chips (≥1), budget dropdown (required, options from BudgetRanges), timeline (DatePickerDialog → store as `Timestamp`), deliverables chips (≥1). On Save Campaign:
  1. `BrandRegistrationViewModel.completeRegistration()` — runs the existing 5-step flow (Auth → uploads → Firestore writes for `users` + `brandProfiles`), now fed from the new draft layout.
  2. After brand profile is committed, `CampaignRepository.createCampaign(brandId, …)` writes the first campaign to `campaigns/{auto}`.
  3. Both succeed → navigate to `homeContainerFragment` with `popUpTo welcomeFragment`. Failure of either rolls back via existing error-handling paths in `BrandRegistrationRepository`.

- **Validation styling**: when a ViewModel publishes an error for a field, the Fragment swaps the input drawable to `bg_input_pill_error.xml` and surfaces the error message under the input (per Figma `Creator_profileSetup_EnteringWrongField_2ndScreen` reference). Reuse `TextInputLayout.error` where the layout uses `TextInputLayout`; otherwise use a sibling TextView pre-wired in the layout.

- **Advanced details writes**: `RegistrationViewModel` / `BrandRegistrationViewModel` already have setters for the existing fields — wire them through the new Advanced fragments without changing repository signatures.

### Phase 5 — Navigation graph

File: `app/src/main/res/navigation/nav_graph.xml`

- Add destinations: `auraCreatePasswordFragment`, `commonProfileSetupFragment`, `creatorAdvancedFragment`, `brandAdvancedFragment`, `campaignSetupFragment`.
- Rename / repurpose: `creatorRegStep1/2/3/4Fragment` → `creatorProfileSetup1/2Fragment` + `creatorAdvancedFragment`. Same for brand. Keep old IDs aliased for one cycle if any external deep links use them — easiest: just rename the IDs; `LoginFragment`'s "incomplete profile" routing needs the new IDs.
- Actions to wire:
  - `welcomeFragment → auraCreatePasswordFragment`
  - `auraCreatePasswordFragment → commonProfileSetupFragment`
  - `commonProfileSetupFragment → roleSelectionFragment`
  - `roleSelectionFragment → creatorProfileSetup1Fragment` and `→ brandProfileSetup1Fragment`
  - Creator chain → `creatorProfileSetup2Fragment → creatorAdvancedFragment → homeContainerFragment` (popUpTo welcome)
  - Brand chain → `brandProfileSetup2Fragment → brandAdvancedFragment → campaignSetupFragment → homeContainerFragment` (popUpTo welcome)
  - Login "incomplete profile" → routes to `commonProfileSetupFragment` if account exists but role/profile missing (preserve existing behavior)

### Phase 6 — Send Deal alignment

File: review `docs/features/send_deal.md` and the send-deal fragment (`SendDealBottomSheet`). The plan adds a real `campaigns` collection. Today, send-deal references campaigns from `BrandProfile.firstCampaignName`. After this PR, send-deal must read `CampaignRepository.getCampaignsForBrand(brandId)`. This is a small wiring change but listed here so it isn't forgotten — the campaign field on the `Deal` doc (`campaignId`) finally has a real document to point at.

### Phase 7 — Verification

Manual end-to-end on a fresh device / cleared app data:

1. **Creator full path**
   - Welcome → Create password (typo password → see error state) → fix → Common profile setup → fill all fields, leave phone blank → see "Phone number is required" red border → fix → Role selection → tap Creator card (verify selected drawable swaps) → Setup 1 (pick 2 categories, 1 audience, location) → Setup 2 (photo, headline, bio with 250-char string → see counter clamp + error if Figma spec enforces; otherwise allow) → 1 featured video uploaded → Advanced (skip) → land on home, verify Firebase Auth user created, `users/{uid}` and `creatorProfiles/{uid}` written with `targetAudience`, `headline`, `portfolioLink`, niches, location.
   - Repeat with motto entered on Advanced; verify `creatorProfiles/{uid}.motto` is set.

2. **Brand full path**
   - Welcome → Create password → Common profile → Role selection → tap Brand → Setup 1 (industry, audience, location, website) → Setup 2 (logo, brand name, verification doc PDF, website) → Advanced (fill legal name, rep, company email, LinkedIn, twitter) → Campaign setup (title, description, 2 goals, $5k–$10k budget, timeline next month, 2 deliverables) → Save Campaign → home.
   - Verify: `users/{uid}`, `brandProfiles/{uid}`, `campaigns/{auto}` all written with correct fields. `BrandProfile.firstCampaignName` may stay set from legacy — ignore in code going forward.

3. **Skip Advanced**
   - Brand Advanced → Skip → land on Campaign setup directly. Verify `brandProfiles` doc is missing `legalName/repName/etc.` (defaults are empty strings).
   - Creator Advanced → Skip → land on home. Verify `creatorProfiles.motto == ""`.

4. **Validation error states**
   - On every screen: tap primary CTA with empty required fields → confirm each field shows the correct red error drawable + copy from `strings.xml`. The Figma `Creator_profileSetup_EnteringWrongField_2ndScreen` reference is the visual benchmark.

5. **Lifecycle**
   - Rotate device on each fragment → drafts persist (since they live on the shared `OnboardingDraftViewModel` / role ViewModel, not on the fragment). Photo + verification doc URIs survive rotation (existing pattern uses ViewModel-held URIs).

6. **Send Deal sanity**
   - As the freshly-created brand, run an existing send-deal flow → verify the new campaign shows up in `SendDealBottomSheet`'s campaign picker (because `CampaignRepository.getCampaignsForBrand(brandId)` reads `campaigns/{}` directly).

7. **Build / type-check** — `./gradlew assembleDebug`. No Gradle module changes expected.

## Critical files (touched)

| File | Change type |
|---|---|
| `app/src/main/java/com/aura/app/data/model/CreatorProfile.kt` | Add `targetAudience`, `portfolioLink`, `headline` fields |
| `app/src/main/java/com/aura/app/data/model/BrandProfile.kt` | Add `targetAudience`, confirm `brandWebsite` field |
| **New** `app/src/main/java/com/aura/app/data/model/Campaign.kt` | New model |
| **New** `app/src/main/java/com/aura/app/data/repository/CampaignRepository.kt` | New repo |
| `app/src/main/java/com/aura/app/data/repository/BrandRegistrationRepository.kt` | After registerBrand, hand off to CampaignRepository.createCampaign for first campaign |
| `app/src/main/java/com/aura/app/ui/auth/RegistrationViewModel.kt` | Refactor draft to match new field set |
| `app/src/main/java/com/aura/app/ui/auth/brand/BrandRegistrationViewModel.kt` | Refactor draft + add campaign draft fields |
| **New** `app/src/main/java/com/aura/app/ui/auth/OnboardingDraftViewModel.kt` | Cross-role draft for password + common profile |
| **New** `app/src/main/java/com/aura/app/ui/auth/AuraCreatePasswordFragment.kt` | New |
| **New** `app/src/main/java/com/aura/app/ui/auth/CommonProfileSetupFragment.kt` | New |
| `app/src/main/java/com/aura/app/ui/auth/RoleSelectionFragment.kt` | Restyle + write role into draft |
| `app/src/main/java/com/aura/app/ui/auth/creator/CreatorRegStep{1,2,3,4}Fragment.kt` | Migrate / delete |
| **New** `app/src/main/java/com/aura/app/ui/auth/creator/CreatorProfileSetup1Fragment.kt` | New (or repurpose Step3) |
| **New** `app/src/main/java/com/aura/app/ui/auth/creator/CreatorProfileSetup2Fragment.kt` | New (or repurpose Step2) |
| **New** `app/src/main/java/com/aura/app/ui/auth/creator/CreatorAdvancedFragment.kt` | New |
| `app/src/main/java/com/aura/app/ui/auth/brand/BrandRegStep{1..5}Fragment.kt` | Migrate / delete |
| **New** `app/src/main/java/com/aura/app/ui/auth/brand/BrandProfileSetup1Fragment.kt` | New |
| **New** `app/src/main/java/com/aura/app/ui/auth/brand/BrandProfileSetup2Fragment.kt` | New |
| **New** `app/src/main/java/com/aura/app/ui/auth/brand/BrandAdvancedFragment.kt` | New |
| **New** `app/src/main/java/com/aura/app/ui/auth/brand/CampaignSetupFragment.kt` | New (replaces BrandRegStep5) |
| **New** `app/src/main/java/com/aura/app/utils/BrandIndustryTags.kt` | New constant |
| **New** `app/src/main/java/com/aura/app/utils/TargetAudienceTags.kt` | New constant |
| **New** `app/src/main/java/com/aura/app/utils/CampaignGoals.kt` | New constant |
| **New** `app/src/main/java/com/aura/app/utils/CampaignDeliverables.kt` | New constant |
| **New** `app/src/main/java/com/aura/app/utils/BudgetRanges.kt` | New constant |
| `app/src/main/java/com/aura/app/utils/Constants.kt` | Add `COLLECTION_CAMPAIGNS = "campaigns"` if missing |
| **New / modify** layouts as listed in Phase 3 | |
| `app/src/main/res/values/strings.xml` | Add Figma copy verbatim |
| `app/src/main/res/values/colors.xml` | Add tokens if missing |
| `app/src/main/res/drawable/` | Add input/chip/button/role-card drawables |
| `app/src/main/res/navigation/nav_graph.xml` | New chain wiring |
| `docs/FIRESTORE_SCHEMA.md` | Document `campaigns` collection + new profile fields |
| `docs/features/profile_setup.md` | **New** feature spec (per AGENTS.md §7 — write before coding lands) |
| `docs/features/campaign_setup.md` | **New** feature spec |
| `docs/features/send_deal.md` | Note: campaigns now read from `campaigns` collection |

## Reusable existing assets (don't recreate)

- `StorageRepository` — `uploadProfilePicture`, `uploadVerificationDoc(Result)`, `uploadPortfolioVideo`, `deleteFile` (rollback)
- `BrandRegistrationRepository.registerBrand` — keep the orchestration; only change is post-success campaign write
- `RegistrationViewModel.completeRegistration` — same; new fields just add to the existing call
- `YouTubeRepository` — creator's YouTube analytics fetch on finish stays unchanged
- `CreatorNicheTags.kt` — already covers Figma's creator categories (use directly)
- `SessionManager.saveUserId` — call site after each role's finish step
- `LoginFragment` "incomplete profile" routing — keep, just update destinations
- `EditProfileFragment` — out of scope for this plan; do **not** rework
- Memory: `feedback_skip_top_header.md`, `feedback_android_lifecycle_pitfalls.md`, `feedback_ask_before_assuming_gaps.md` — followed throughout
