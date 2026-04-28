package com.aura.app.data.repository

import com.aura.app.data.model.Deal
import com.aura.app.data.model.Notification
import com.aura.app.utils.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

class DealRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notifRepo: NotificationRepository = NotificationRepository(),
    private val userRepo: UserRepository = UserRepository(),
) {
    private val deals = firestore.collection(Constants.COLLECTION_DEALS)

    suspend fun createDeal(deal: Deal): Result<String> = runCatching {
        check(deal.campaignId.isNotBlank()) { "Deal must reference a campaign" }
        check(deal.brandId.isNotBlank() && deal.creatorId.isNotBlank()) {
            "Deal must have both a brand and a creator"
        }

        // One live deal per (campaign, brand, creator) tuple. Past/closed deals don't block re-sending.
        val existing = deals
            .whereEqualTo("brandId", deal.brandId)
            .whereEqualTo("creatorId", deal.creatorId)
            .whereEqualTo("campaignId", deal.campaignId)
            .whereIn("status", listOf(Constants.STATUS_PENDING, Constants.STATUS_ACCEPTED))
            .limit(1)
            .get()
            .await()
        check(existing.isEmpty) {
            "A deal for this campaign with this creator is already in progress"
        }

        val ref = deals.document()
        val dealWithId = deal.copy(
            dealId = ref.id,
            status = Constants.STATUS_PENDING,
            chatUnlocked = false,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        ref.set(dealWithId).await()

        // Notify the creator about the incoming deal
        val brandName = userRepo.getUserLite(deal.brandId)?.displayName ?: "A brand"
        notifRepo.createNotification(
            Notification(
                recipientId = deal.creatorId,
                actorId = deal.brandId,
                actorName = brandName,
                type = Notification.TYPE_DEAL_RECEIVED,
                dealId = ref.id,
                dealTitle = deal.title,
                message = "\"$brandName\" sent you a deal: ${deal.title}",
            )
        )

        ref.id
    }

    fun getDealsForCreator(creatorId: String): Flow<List<Deal>> =
        deals.whereEqualTo("creatorId", creatorId)
            .snapshots()
            .map { snap ->
                snap.documents
                    .mapNotNull { it.toObject(Deal::class.java)?.copy(dealId = it.id) }
                    .map { expireIfStale(it) }
                    .sortedByDescending { it.createdAt }
            }

    fun getDealsForBrand(brandId: String): Flow<List<Deal>> =
        deals.whereEqualTo("brandId", brandId)
            .snapshots()
            .map { snap ->
                snap.documents
                    .mapNotNull { it.toObject(Deal::class.java)?.copy(dealId = it.id) }
                    .map { expireIfStale(it) }
                    .sortedByDescending { it.createdAt }
            }

    suspend fun getDeal(dealId: String): Result<Deal> = runCatching {
        val doc = deals.document(dealId).get().await()
        val deal = doc.toObject(Deal::class.java)?.copy(dealId = doc.id)
            ?: error("Deal not found")
        expireIfStale(deal)
    }

    /** Real-time stream of a single deal — drives chat banner & completion-bar live updates. */
    fun streamDeal(dealId: String): Flow<Deal> =
        deals.document(dealId)
            .snapshots()
            .map { snap ->
                snap.toObject(Deal::class.java)?.copy(dealId = snap.id)
                    ?: error("Deal not found")
            }

    // Flips status → accepted AND chatUnlocked → true in one atomic transaction.
    suspend fun acceptDeal(dealId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            check(snap.getString("status") == Constants.STATUS_PENDING) {
                "Only pending deals can be accepted"
            }
            tx.update(
                ref,
                mapOf(
                    "status" to Constants.STATUS_ACCEPTED,
                    "chatUnlocked" to true,
                    "updatedAt" to Timestamp.now(),
                )
            )
        }.await()

        // Notify the brand that the deal was accepted
        val creatorName = userRepo.getUserLite(deal.creatorId)?.displayName ?: "The creator"
        notifRepo.createNotification(
            Notification(
                recipientId = deal.brandId,
                actorId = deal.creatorId,
                actorName = creatorName,
                type = Notification.TYPE_DEAL_ACCEPTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$creatorName\" accepted your deal: ${deal.title}",
            )
        )
    }

    suspend fun rejectDeal(dealId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        deals.document(dealId).update(
            mapOf(
                "status" to Constants.STATUS_REJECTED,
                "updatedAt" to Timestamp.now(),
            )
        ).await()

        // Notify the brand that the deal was rejected
        val creatorName = userRepo.getUserLite(deal.creatorId)?.displayName ?: "The creator"
        notifRepo.createNotification(
            Notification(
                recipientId = deal.brandId,
                actorId = deal.creatorId,
                actorName = creatorName,
                type = Notification.TYPE_DEAL_REJECTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$creatorName\" declined your deal: ${deal.title}",
            )
        )
    }

    /**
     * Immediate, unilateral cancel.
     *  - PENDING offers → status = CANCELLED, retraction notif sent.
     *  - ACCEPTED deals → status = CANCELLED, both parties notified, chat goes read-only.
     *
     * Reviews are NOT required for cancelled deals; the deal lands in History immediately.
     */
    suspend fun cancelDeal(dealId: String, cancelledBy: String = "", reason: String = ""): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        var wasPending = false
        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            val currentStatus = snap.getString("status")
            check(currentStatus in listOf(Constants.STATUS_PENDING, Constants.STATUS_ACCEPTED)) {
                "Only pending or active deals can be cancelled"
            }
            wasPending = currentStatus == Constants.STATUS_PENDING
            tx.update(
                ref,
                mapOf(
                    "status" to Constants.STATUS_CANCELLED,
                    "cancelledBy" to cancelledBy,
                    "cancelReason" to reason,
                    "cancelRequestedBy" to "",
                    "completionRequestedBy" to "",
                    "brandCloseConfirmed" to false,
                    "creatorCloseConfirmed" to false,
                    "chatUnlocked" to true,
                    "updatedAt" to Timestamp.now(),
                ),
            )
        }.await()

        val otherPartyId = if (cancelledBy == deal.creatorId) deal.brandId else deal.creatorId
        val actorName = userRepo.getUserLite(cancelledBy)?.displayName ?: "The other party"
        if (wasPending) {
            notifRepo.createNotification(Notification(
                recipientId = otherPartyId,
                actorId = cancelledBy,
                actorName = actorName,
                type = Notification.TYPE_DEAL_RETRACTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$actorName\" withdrew the deal \"${deal.title}\".",
            ))
        } else {
            val msg = if (reason.isNotBlank())
                "\"$actorName\" cancelled \"${deal.title}\": $reason"
            else
                "\"$actorName\" cancelled \"${deal.title}\"."
            notifRepo.createNotification(Notification(
                recipientId = otherPartyId,
                actorId = cancelledBy,
                actorName = actorName,
                type = Notification.TYPE_DEAL_CANCELED,
                dealId = dealId,
                dealTitle = deal.title,
                message = msg,
            ))
        }
    }

    /**
     * Either party clicks "Complete Deal". Flips that party's per-role close flag.
     * When both flags are true the deal enters review-pending phase: both parties get a
     * notification asking them to leave a review. Status stays ACCEPTED until both reviews
     * are submitted; the deal stays on the Active list throughout.
     */
    suspend fun requestCompletion(dealId: String, initiatorId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        val isCreator = initiatorId == deal.creatorId
        val myField = if (isCreator) "creatorCloseConfirmed" else "brandCloseConfirmed"
        val otherPartyId = if (isCreator) deal.brandId else deal.creatorId

        var bothNowConfirmed = false
        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            check(snap.getString("status") == Constants.STATUS_ACCEPTED) {
                "Only active deals can be marked complete"
            }
            val brandConfirmed = if (myField == "brandCloseConfirmed") true
                else snap.getBoolean("brandCloseConfirmed") ?: false
            val creatorConfirmed = if (myField == "creatorCloseConfirmed") true
                else snap.getBoolean("creatorCloseConfirmed") ?: false
            bothNowConfirmed = brandConfirmed && creatorConfirmed

            val updates = mutableMapOf<String, Any>(
                myField to true,
                "updatedAt" to Timestamp.now(),
            )
            // Keep `completionRequestedBy` so older callers/UI that read it as a "someone wants to
            // close" signal still work. Cleared once both have confirmed.
            if (bothNowConfirmed) {
                updates["completionRequestedBy"] = ""
            } else {
                updates["completionRequestedBy"] = initiatorId
            }
            tx.update(ref, updates)
        }.await()

        val actorName = userRepo.getUserLite(initiatorId)?.displayName
            ?: if (isCreator) "The creator" else "The brand"

        if (bothNowConfirmed) {
            // Tell BOTH parties the deal is ready for reviews. Submitting reviews finalizes it.
            val readyMsg = "\"${deal.title}\" was marked complete by both parties. Please leave your review to finalize the deal."
            notifRepo.createNotification(Notification(
                recipientId = deal.brandId,
                actorId = "",
                actorName = "Aura",
                type = Notification.TYPE_REVIEW_REQUESTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = readyMsg,
            ))
            notifRepo.createNotification(Notification(
                recipientId = deal.creatorId,
                actorId = "",
                actorName = "Aura",
                type = Notification.TYPE_REVIEW_REQUESTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = readyMsg,
            ))
        } else {
            notifRepo.createNotification(
                Notification(
                    recipientId = otherPartyId,
                    actorId = initiatorId,
                    actorName = actorName,
                    type = Notification.TYPE_DEAL_COMPLETION_REQUESTED,
                    dealId = dealId,
                    dealTitle = deal.title,
                    message = "\"$actorName\" marked ${deal.title} as complete. Please confirm if completed.",
                )
            )
        }
    }

    /**
     * The other party confirms a pending completion request via the chat bar.
     * Functionally equivalent to `requestCompletion` called by [confirmerId] — flips their flag.
     */
    suspend fun confirmCompletion(dealId: String, confirmerId: String): Result<Unit> =
        requestCompletion(dealId, confirmerId)

    /**
     * The other party declines a completion request — clears the requester's close flag so the
     * deal stays in the normal active state.
     */
    suspend fun declineCompletion(dealId: String, declinerId: String = ""): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        // Whoever already had their flag set (the requester) gets it cleared.
        val requesterId = when {
            deal.brandCloseConfirmed && !deal.creatorCloseConfirmed -> deal.brandId
            deal.creatorCloseConfirmed && !deal.brandCloseConfirmed -> deal.creatorId
            else -> deal.completionRequestedBy
        }

        deals.document(dealId).update(
            mapOf(
                "brandCloseConfirmed" to false,
                "creatorCloseConfirmed" to false,
                "completionRequestedBy" to "",
                "updatedAt" to Timestamp.now(),
            )
        ).await()

        if (requesterId.isNotBlank()) {
            val actorId = if (declinerId.isNotBlank()) declinerId
                else if (requesterId == deal.creatorId) deal.brandId else deal.creatorId
            val actorName = userRepo.getUserLite(actorId)?.displayName ?: "The other party"
            notifRepo.createNotification(Notification(
                recipientId = requesterId,
                actorId = actorId,
                actorName = actorName,
                type = Notification.TYPE_DEAL_COMPLETION_DECLINED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$actorName\" declined completion for \"${deal.title}\". The deal remains active.",
            ))
        }
    }

    suspend fun requestCancellation(dealId: String, initiatorId: String, reason: String = ""): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            check(snap.getString("status") == Constants.STATUS_ACCEPTED) {
                "Only active deals can be cancelled via bilateral request"
            }
            tx.update(
                ref,
                mapOf(
                    "cancelRequestedBy" to initiatorId,
                    "cancelReason" to reason,
                    "updatedAt" to Timestamp.now(),
                )
            )
        }.await()

        val otherPartyId = if (initiatorId == deal.creatorId) deal.brandId else deal.creatorId
        val actorName = userRepo.getUserLite(initiatorId)?.displayName ?: "The other party"
        notifRepo.createNotification(Notification(
            recipientId = otherPartyId,
            actorId = initiatorId,
            actorName = actorName,
            type = Notification.TYPE_DEAL_CANCEL_REQUESTED,
            dealId = dealId,
            dealTitle = deal.title,
            message = "\"$actorName\" requested to cancel ${deal.title}. Please confirm.",
        ))
    }

    suspend fun confirmCancellation(dealId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")
        val initiator = deal.cancelRequestedBy.ifBlank { deal.cancelledBy }
        cancelDeal(dealId, cancelledBy = initiator, reason = deal.cancelReason).getOrThrow()
    }

    suspend fun declineCancellation(dealId: String, declinerId: String = ""): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        deals.document(dealId).update(
            mapOf(
                "cancelRequestedBy" to "",
                "cancelReason" to "",
                "updatedAt" to Timestamp.now(),
            )
        ).await()

        if (deal.cancelRequestedBy.isNotBlank()) {
            val actorId = if (declinerId.isNotBlank()) declinerId
                else if (deal.cancelRequestedBy == deal.creatorId) deal.brandId else deal.creatorId
            val actorName = userRepo.getUserLite(actorId)?.displayName ?: "The other party"
            notifRepo.createNotification(Notification(
                recipientId = deal.cancelRequestedBy,
                actorId = actorId,
                actorName = actorName,
                type = Notification.TYPE_DEAL_CANCEL_DECLINED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$actorName\" declined cancellation for \"${deal.title}\". The deal remains active.",
            ))
        }
    }

    suspend fun updateDealDetails(dealId: String, title: String, description: String): Result<Unit> = runCatching {
        val snap = deals.document(dealId).get().await()
        check(snap.getString("status") == Constants.STATUS_ACCEPTED) {
            "Deal details can only be edited while the deal is active"
        }
        deals.document(dealId).update(
            mapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "updatedAt" to Timestamp.now(),
            )
        ).await()
    }

    /**
     * Records that [userId] submitted their review. When BOTH reviews are in AND both parties had
     * already confirmed completion, the deal transitions to COMPLETED and is archived to History.
     * Cancellation no longer routes through reviews; cancelled deals are already in CANCELLED state.
     */
    suspend fun markUserReviewed(dealId: String, userId: String): Result<Unit> = runCatching {
        val snap = deals.document(dealId).get().await()
        val deal = snap.toObject(Deal::class.java)?.copy(dealId = snap.id)
            ?: error("Deal not found")

        val isCreator = deal.creatorId == userId
        val field = if (isCreator) "creatorReviewedAt" else "brandReviewedAt"
        val otherPartyId = if (isCreator) deal.brandId else deal.creatorId
        val otherHasReviewed = if (isCreator) deal.brandReviewedAt != null else deal.creatorReviewedAt != null

        var dealCompleted = false
        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val fresh = tx.get(ref)
            val now = Timestamp.now()
            val updates = mutableMapOf<String, Any>(
                field to now,
                "updatedAt" to now,
            )

            val creatorReviewed = if (field == "creatorReviewedAt") true
                else fresh.getTimestamp("creatorReviewedAt") != null
            val brandReviewed = if (field == "brandReviewedAt") true
                else fresh.getTimestamp("brandReviewedAt") != null
            val bothReviewed = creatorReviewed && brandReviewed
            val brandClosed = fresh.getBoolean("brandCloseConfirmed") ?: false
            val creatorClosed = fresh.getBoolean("creatorCloseConfirmed") ?: false
            val bothClosed = brandClosed && creatorClosed

            if (bothReviewed && bothClosed && fresh.getString("status") == Constants.STATUS_ACCEPTED) {
                updates["status"] = Constants.STATUS_COMPLETED
                updates["completedAt"] = now
                updates["completionRequestedBy"] = ""
                dealCompleted = true
            }
            tx.update(ref, updates)
        }.await()

        val myName = userRepo.getUserLite(userId)?.displayName ?: "The other party"
        if (dealCompleted) {
            val msg = "\"${deal.title}\" is now complete. Thanks for using Aura!"
            notifRepo.createNotification(Notification(
                recipientId = deal.brandId,
                actorId = "",
                actorName = "Aura",
                type = Notification.TYPE_DEAL_COMPLETED,
                dealId = dealId,
                dealTitle = deal.title,
                message = msg,
            ))
            notifRepo.createNotification(Notification(
                recipientId = deal.creatorId,
                actorId = "",
                actorName = "Aura",
                type = Notification.TYPE_DEAL_COMPLETED,
                dealId = dealId,
                dealTitle = deal.title,
                message = msg,
            ))
        } else if (!otherHasReviewed) {
            // First reviewer — let the other party know a review is in. No popup auto-trigger.
            notifRepo.createNotification(Notification(
                recipientId = otherPartyId,
                actorId = userId,
                actorName = myName,
                type = Notification.TYPE_REVIEW_REQUESTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$myName\" left a review for \"${deal.title}\". Open the chat and tap Review User to submit yours.",
            ))
        }
    }

    // Checks if a pending deal is older than 7 days and marks it expired locally.
    // Suspend so it can also write expiry notifications for both parties.
    private suspend fun expireIfStale(deal: Deal): Deal {
        if (deal.status != Constants.STATUS_PENDING) return deal
        val createdAt = deal.createdAt?.toDate() ?: return deal
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        if (Date().time - createdAt.time < sevenDaysMs) return deal

        deals.document(deal.dealId).update(
            mapOf(
                "status" to Constants.STATUS_EXPIRED,
                "updatedAt" to Timestamp.now(),
            )
        )

        val expiryMsg = "The deal \"${deal.title}\" expired before it was accepted."
        notifRepo.createNotification(Notification(
            recipientId = deal.creatorId,
            actorId = "",
            actorName = "Aura",
            type = Notification.TYPE_DEAL_EXPIRED,
            dealId = deal.dealId,
            dealTitle = deal.title,
            message = expiryMsg,
        ))
        notifRepo.createNotification(Notification(
            recipientId = deal.brandId,
            actorId = "",
            actorName = "Aura",
            type = Notification.TYPE_DEAL_EXPIRED,
            dealId = deal.dealId,
            dealTitle = deal.title,
            message = "Your deal \"${deal.title}\" expired before the creator accepted it.",
        ))

        return deal.copy(status = Constants.STATUS_EXPIRED)
    }
}
