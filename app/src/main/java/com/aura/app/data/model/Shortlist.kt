package com.aura.app.data.model

import com.google.firebase.Timestamp

data class Shortlist(
    val shortlistId: String = "",
    val brandId: String = "",
    val creatorId: String = "",
    val campaignId: String = "",
    val note: String = "",
    val savedAt: Timestamp = Timestamp.now(),
)
