# Aura Work Context

## Current Goal

Continue implementing `docs/rough plans/profile-setup-final-plan.md` for the Android app while preserving the existing single-activity, XML, MVVM, Firebase repository architecture described in `AGENTS.md`.

## Repo Rules To Keep In Mind

- UI screens must be Fragments.
- Fragments should stay view-only; Firebase access belongs in repositories.
- No hardcoded strings/colors/dimens in XML when adding new UI.
- Firestore model classes need default values on every property.
- Do not commit secrets such as `google-services.json`.
- The user prefers to run Gradle themselves unless they ask otherwise.

## Important Recent User Requests

- User said the Deal Dashboard and Edit Profile loading issue is gone.
- User asked to implement profile setup from the plan, starting with common screens.
- User said password screen looks okay, but the Create Account button should match the UI design exactly.
- User asked for Common Profile Setup to match the UI design exactly.
- User asked to make Edit Profile support deleting the account/profile.
- User clarified deleting the profile should land on the Welcome screen.

## Changes Already Made

### Common Profile Setup Flow

Added:
- `app/src/main/java/com/aura/app/ui/auth/OnboardingDraftViewModel.kt`
- `app/src/main/java/com/aura/app/ui/auth/AuraCreatePasswordFragment.kt`
- `app/src/main/java/com/aura/app/ui/auth/CommonProfileSetupFragment.kt`
- `app/src/main/res/layout/fragment_create_password.xml`
- `app/src/main/res/layout/fragment_common_profile_setup.xml`

Navigation now routes:

`WelcomeFragment -> AuraCreatePasswordFragment -> CommonProfileSetupFragment -> RoleSelectionFragment`

`RoleSelectionFragment` copies `OnboardingDraftViewModel` values into:
- `RegistrationViewModel` for creators
- `BrandRegistrationViewModel` for brands

The current implementation keeps old role-specific registration screens and IDs in place, but skips the old account-create steps after the common screen:
- Creator routes to `creatorRegStep3Fragment`
- Brand routes to `brandRegStep2Fragment`

### Password Screen CTA

In `AuraCreatePasswordFragment`, the bottom button text is `Create account` and its icon is removed so it matches the pill-gradient design better.

### Common Profile UI

`fragment_common_profile_setup.xml` was rebuilt to match the available registration design language:
- top onboarding bar
- progress header
- progress indicator
- visual anchor panel
- uppercase secondary labels
- filled dark fields
- separate `+1` phone prefix block
- security question section
- existing bottom action bar

Phone values are saved with `+1` when the user enters a local number.

### Profile/Campaign Field Wiring

Partially implemented plan fields into existing screens:
- Creator setup stores target audience, portfolio link, headline, bio, YouTube handle.
- Brand setup stores target audience and website.
- Campaign setup stores title, description, goals, budget range, timeline, deliverables.

Added/updated data support:
- `Campaign.kt` has `updatedAt` and `isActive`.
- `CampaignRepository` has `createCampaign`, `saveCampaign`, `getCampaignsForBrand`, `getCampaign`, `deleteCampaign`, `setActive`.
- `BrandRegistrationViewModel` creates the initial campaign after brand registration succeeds.
- `BrandRegistrationRepository` writes `targetAudience` and `website`.
- `CreatorProfile` and `RegistrationViewModel` include `headline`, `targetAudience`, and `portfolioLink`.

### Feature Specs

Added:
- `docs/features/profile_setup.md`
- `docs/features/campaign_setup.md`

### Edit Profile Loading Fix

Deal Dashboard and Edit Profile no longer stay stuck loading when `FirebaseAuth.currentUser` is null but `SessionManager` has a user ID.

Touched:
- `DealDashboardViewModel`
- `DealDashboardFragment`
- `EditProfileViewModel`
- `EditProfileFragment`

### Account Deletion

Edit Profile delete button now deletes the account instead of showing a placeholder.

Touched:
- `AuthRepository.deleteCurrentUser()`
- `UserRepository.deleteAccountData(userId, role)`
- `EditProfileViewModel.deleteAccount()`
- `EditProfileFragment` confirmation dialog and delete-success navigation
- `fragment_edit_profile.xml` button text
- strings for delete dialog/button

Delete-success navigation now uses the root nav controller:

`popUpTo(R.id.homeContainerFragment, inclusive = true)` then navigate to `R.id.welcomeFragment`.

Note: Firebase Auth may require recent login. In that case the app shows: "Please log out and log back in before deleting this account."

## Files With Notable Uncommitted Work

Likely modified:
- `app/src/main/res/navigation/nav_graph.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/layout/fragment_common_profile_setup.xml`
- `app/src/main/res/layout/fragment_create_password.xml`
- `app/src/main/res/layout/fragment_creator_reg_step2.xml`
- `app/src/main/res/layout/fragment_creator_reg_step3.xml`
- `app/src/main/res/layout/fragment_brand_reg_step2.xml`
- `app/src/main/res/layout/fragment_brand_reg_step4.xml`
- `app/src/main/res/layout/fragment_brand_reg_step5.xml`
- `app/src/main/java/com/aura/app/ui/auth/*`
- `app/src/main/java/com/aura/app/ui/auth/creator/*`
- `app/src/main/java/com/aura/app/ui/auth/brand/*`
- `app/src/main/java/com/aura/app/ui/main/EditProfile*`
- `app/src/main/java/com/aura/app/ui/chat/DealDashboard*`
- `app/src/main/java/com/aura/app/data/model/*`
- `app/src/main/java/com/aura/app/data/repository/*`
- `docs/FIRESTORE_SCHEMA.md`

Untracked helper/constants/drawables may already exist:
- `BrandIndustryTags.kt`
- `TargetAudienceTags.kt`
- `CampaignGoals.kt`
- `CampaignDeliverables.kt`
- `BudgetRanges.kt`
- input/button/video-slot drawables

## Verification So Far

`git diff --check` has been run after recent edits and was clean.

Gradle was not run after the latest changes because the user said they will run builds when needed. Earlier Gradle attempts required access to `~/.gradle`; the user interrupted approval/build attempts.

## Likely Next Steps

1. User will inspect password/common profile UI.
2. If continuing the plan, next likely work is to polish role-specific creator/brand setup screens against the final UI design.
3. If user reports build errors, fix compile issues first. Likely areas to check:
   - generated binding names for new XML IDs
   - imports in `EditProfileFragment` / `EditProfileViewModel`
   - nav action IDs in `RoleSelectionFragment`
   - `TextInputLayout` style compatibility

