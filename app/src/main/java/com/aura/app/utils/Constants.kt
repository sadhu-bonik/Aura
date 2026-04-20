package com.aura.app.utils

object Constants {
    // Collections
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CREATOR_PROFILES = "creatorProfiles"
    const val COLLECTION_BRAND_PROFILES = "brandProfiles"
    const val COLLECTION_PORTFOLIO_ITEMS = "portfolioItems"
    const val COLLECTION_CAMPAIGNS = "campaigns"
    const val COLLECTION_DEALS = "deals"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_SHORTLISTS = "shortlists"
    const val COLLECTION_REVIEWS = "reviews"
    const val COLLECTION_RECOMMENDATIONS = "recommendations"

    // Roles
    const val ROLE_CREATOR = "creator"
    const val ROLE_BRAND = "brand"

    // Deal statuses
    const val STATUS_PENDING = "pending"
    const val STATUS_ACCEPTED = "accepted"
    const val STATUS_REJECTED = "rejected"
    const val STATUS_COMPLETED = "completed"
    const val STATUS_CANCELLED = "cancelled"
    const val STATUS_EXPIRED = "expired"

    // Storage paths
    const val STORAGE_PROFILE_IMAGES = "profileImages"
    const val STORAGE_PORTFOLIO_ITEMS = "portfolioItems"
    const val STORAGE_BRAND_LOGOS = "brandLogos"
    const val STORAGE_CAMPAIGN_ASSETS = "campaignAssets"

    // Stub — replace with FirebaseAuth.getInstance().currentUser?.uid once auth lands
    const val STUB_USER_ID = "stub_user_001"
    const val STUB_USER_ROLE = ROLE_CREATOR

    // Flip to false to use real Firestore
    const val USE_STUBS = false
}
