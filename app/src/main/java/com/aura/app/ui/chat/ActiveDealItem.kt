package com.aura.app.ui.chat

import com.aura.app.data.model.Deal
import com.aura.app.data.model.UserLite

data class ActiveDealItem(
    val deal: Deal,
    val otherUser: UserLite?,
    val unreadCount: Int,
)
