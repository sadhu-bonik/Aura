# Campaign Setup

## User Story

A brand finishes onboarding by creating its first campaign with a title, description, goals, budget range, deadline, and deliverables. After registration succeeds, the campaign is available for send-deal flows.

## Screens / Flow

`BrandRegStep5Fragment` is the onboarding campaign setup screen. The brand fills campaign fields and taps Finish. The ViewModel registers the brand account first, then writes the campaign document, and finally navigates to the home container.

## Firestore Reads / Writes

- Writes `campaigns/{campaignId}` with:
  - `campaignId`
  - `brandId`
  - `title`
  - `description`
  - `goals`
  - `budgetRange`
  - `budgetMin`
  - `budgetMax`
  - `timeline`
  - `deliverables`
  - `createdAt`
  - `updatedAt`
  - `isActive`

`CampaignRepository.getCampaignsForBrand(brandId)` streams campaigns for the brand and is the shared source for campaign pickers.

## Edge Cases

- All campaign setup fields are required during onboarding.
- If brand registration succeeds but campaign creation fails, the ViewModel surfaces an error instead of navigating silently.
- Budget labels are converted to min/max cents through `BudgetRanges`.

## Invariants Touched

- Firestore fields use camelCase and Kotlin models have defaults.
- Campaign data is written through `CampaignRepository`, not directly from UI.
