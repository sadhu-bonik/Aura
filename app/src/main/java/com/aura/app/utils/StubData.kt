package com.aura.app.utils

import com.aura.app.data.model.Deal
import com.aura.app.data.model.UserLite
import com.google.firebase.Timestamp
import java.util.Date

object StubData {

    const val CREATOR_ID = "stub_creator_001"
    const val BRAND_ID = "stub_brand_001"

    private fun ts(minutesAgo: Long): Timestamp =
        Timestamp(Date(System.currentTimeMillis() - minutesAgo * 60_000))

    val users: Map<String, UserLite> = mapOf(
        CREATOR_ID to UserLite(
            userId = CREATOR_ID,
            displayName = "Alice Chen",
            profileImageUrl = "https://i.pravatar.cc/150?img=47",
        ),
        BRAND_ID to UserLite(
            userId = BRAND_ID,
            displayName = "Nova Studio",
            profileImageUrl = "https://i.pravatar.cc/150?img=33",
        ),
    )

    val deals = listOf(
        Deal(
            dealId = "stub_deal_001",
            brandId = BRAND_ID,
            creatorId = CREATOR_ID,
            campaignId = "camp_001",
            title = "Summer Collection Drop",
            description = "Promote our new summer line across your social channels.",
            budget = 500,
            status = Constants.STATUS_ACCEPTED,
            chatUnlocked = true,
            createdAt = ts(120),
            updatedAt = ts(60),
        ),
        Deal(
            dealId = "stub_deal_002",
            brandId = BRAND_ID,
            creatorId = CREATOR_ID,
            campaignId = "camp_002",
            title = "Tech Gadget Review",
            description = "Honest review of our latest wireless earbuds.",
            budget = 250,
            status = Constants.STATUS_COMPLETED,
            chatUnlocked = true,
            createdAt = ts(2880),
            updatedAt = ts(1440),
            completedAt = ts(1440),
        ),
    )
}
