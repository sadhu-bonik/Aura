package com.aura.app.data.model

import com.google.firebase.Timestamp

/**
 * Notification — persisted in `notifications/{notifId}`.
 *
 * Written client-side when a deal status changes. Drives the in-app badge count
 * and the notification inbox sheet on the Deal Dashboard.
 *
 * All fields must have defaults so Firestore toObject() can deserialize via
 * the no-arg constructor (per AGENTS.md §4.6).
 */
data class Notification(
    val notifId: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val type: String = "",       // see TYPE_* constants below
    val dealId: String = "",
    val message: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null,
) {
    companion object {
        const val TYPE_DEAL_RECEIVED = "deal_received"
        const val TYPE_DEAL_ACCEPTED = "deal_accepted"
        const val TYPE_DEAL_REJECTED = "deal_rejected"
    }
}
