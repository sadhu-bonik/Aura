package com.aura.app.utils

import com.aura.app.data.model.Deal
import com.aura.app.data.model.UserLite
import com.google.firebase.Timestamp
import java.util.Date

object StubData {

    const val CREATOR_ID = "stub_creator_001"
    const val BRAND_ID_NOVA = "stub_brand_001"   // keeps existing Firestore messages valid
    const val BRAND_ID_APEX = "stub_brand_apex"

    private fun ts(minutesAgo: Long): Timestamp =
        Timestamp(Date(System.currentTimeMillis() - minutesAgo * 60_000))

    val users: Map<String, UserLite> = mapOf(
        CREATOR_ID to UserLite(
            userId = CREATOR_ID,
            displayName = "Alice Chen",
            profileImageUrl = "https://i.pravatar.cc/150?img=47",
        ),
        BRAND_ID_NOVA to UserLite(
            userId = BRAND_ID_NOVA,
            displayName = "Nova Studio",
            profileImageUrl = "https://i.pravatar.cc/150?img=33",
        ),
        BRAND_ID_APEX to UserLite(
            userId = BRAND_ID_APEX,
            displayName = "Apex Co.",
            profileImageUrl = "https://i.pravatar.cc/150?img=12",
        ),
    )

    val deals = listOf(
        // ── Active (accepted + chatUnlocked) ──────────────────────────────────
        Deal(
            dealId = "stub_deal_001",
            brandId = BRAND_ID_NOVA,
            creatorId = CREATOR_ID,
            campaignId = "camp_001",
            title = "Summer Collection Drop",
            description = "Promote our new summer line across your social channels.",
            budget = 500,
            status = Constants.STATUS_ACCEPTED,
            chatUnlocked = true,
            createdAt = ts(120),
            updatedAt = ts(60),
            lastMessageText = "Sounds good, I'll get started this weekend!",
            lastMessageTime = ts(55),
            unreadCounts = mapOf(BRAND_ID_NOVA to 2L),
        ),
        Deal(
            dealId = "stub_deal_002",
            brandId = BRAND_ID_APEX,
            creatorId = CREATOR_ID,
            campaignId = "camp_002",
            title = "Tech Gadget Review",
            description = "Honest review of our latest wireless earbuds.",
            budget = 250,
            status = Constants.STATUS_ACCEPTED,
            chatUnlocked = true,
            createdAt = ts(500),
            updatedAt = ts(200),
            lastMessageText = "Photo",
            lastMessageTime = ts(180),
            unreadCounts = mapOf(CREATOR_ID to 1L),
        ),

        // ── Pending (new deals — accept/reject to test the flow) ──────────────
        Deal(
            dealId = "stub_deal_003",
            brandId = BRAND_ID_NOVA,
            creatorId = CREATOR_ID,
            campaignId = "camp_003",
            title = "Winter Lookbook",
            description = "Feature our winter collection in a 3-post series.",
            budget = 800,
            status = Constants.STATUS_PENDING,
            chatUnlocked = false,
            createdAt = ts(30),
            updatedAt = ts(30),
        ),
        Deal(
            dealId = "stub_deal_004",
            brandId = BRAND_ID_APEX,
            creatorId = CREATOR_ID,
            campaignId = "camp_004",
            title = "Fitness App Launch",
            description = "Promote the Apex Fit app to your fitness audience.",
            budget = 350,
            status = Constants.STATUS_PENDING,
            chatUnlocked = false,
            createdAt = ts(10),
            updatedAt = ts(10),
        ),

        // ── Past (expired / declined) ─────────────────────────────────────────
        Deal(
            dealId = "stub_deal_005",
            brandId = BRAND_ID_NOVA,
            creatorId = CREATOR_ID,
            campaignId = "camp_005",
            title = "Spring Festival Campaign",
            description = "We needed a last minute post but you didn't see this in time.",
            budget = 400,
            status = Constants.STATUS_EXPIRED,
            chatUnlocked = false,
            createdAt = ts(15000),
            updatedAt = ts(10000),
        ),
        Deal(
            dealId = "stub_deal_006",
            brandId = BRAND_ID_APEX,
            creatorId = CREATOR_ID,
            campaignId = "camp_006",
            title = "Shady Crypto Promo",
            description = "Promote our unverified coin to your followers.",
            budget = 5000,
            status = Constants.STATUS_REJECTED,
            chatUnlocked = false,
            createdAt = ts(20000),
            updatedAt = ts(19000),
        ),
        // stub_deal_007 — COMPLETED deal (needed to test review UI)
        Deal(
            dealId = "stub_deal_007",
            creatorId = CREATOR_ID,
            brandId = BRAND_ID_NOVA,
            title = "Summer Collection Drop",
            description = "Promote our new summer line across your social channels.",
            status = Constants.STATUS_COMPLETED,
            chatUnlocked = true,
            completedAt = Timestamp(Date(System.currentTimeMillis() - 7 * 86400 * 1000L)),
            updatedAt = Timestamp.now(),
            createdAt = Timestamp(Date(System.currentTimeMillis() - 14 * 86400 * 1000L)),
        )
    )

    val mutableCreatorRatings: MutableMap<String, Pair<Double, Long>> = mutableMapOf()

    fun updateCreatorRating(userId: String, newAvg: Double, newCount: Long) {
        mutableCreatorRatings[userId] = Pair(newAvg, newCount)
    }
}
