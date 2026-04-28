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
    val dealTitle: String = "",
    val message: String = "",
    val read: Boolean = false,
    val openReviewPopup: Boolean = false,
    val createdAt: Timestamp? = null,
) {
    companion object {
        const val TYPE_DEAL_RECEIVED = "deal_received"        // brand → creator: new deal sent
        const val TYPE_DEAL_ACCEPTED = "deal_accepted"        // creator → brand: accepted
        const val TYPE_DEAL_REJECTED = "deal_rejected"        // creator → brand: declined
        const val TYPE_DEAL_RETRACTED = "deal_retracted"      // brand → creator: pre-accept withdraw
        const val TYPE_DEAL_CANCEL_REQUESTED = "deal_cancel_requested" // one side proposes cancel, awaiting confirm
        const val TYPE_DEAL_CANCEL_DECLINED = "deal_cancel_declined"   // other side rejected the cancel proposal
        const val TYPE_DEAL_CANCELED = "deal_canceled"        // mutual cancel confirmation
        const val TYPE_DEAL_COMPLETION_REQUESTED = "deal_completion_requested" // one side asks to mark complete
        const val TYPE_DEAL_COMPLETED = "deal_completed"      // both sides confirmed
        const val TYPE_REVIEW_REQUESTED = "review_requested"  // prompt to leave a review
    }
}
