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

    suspend fun completeDeal(dealId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        deals.document(dealId).update(
            mapOf(
                "status" to Constants.STATUS_COMPLETED,
                "updatedAt" to Timestamp.now(),
                "completedAt" to Timestamp.now(),
            )
        ).await()

        val message = "Your deal \"${deal.title}\" is now complete. Great work!"
        notifRepo.createNotification(Notification(
            recipientId = deal.creatorId,
            actorId = deal.brandId,
            actorName = userRepo.getUserLite(deal.brandId)?.displayName ?: "The brand",
            type = Notification.TYPE_DEAL_COMPLETED,
            dealId = dealId,
            dealTitle = deal.title,
            message = message,
        ))
        notifRepo.createNotification(Notification(
            recipientId = deal.brandId,
            actorId = deal.creatorId,
            actorName = userRepo.getUserLite(deal.creatorId)?.displayName ?: "The creator",
            type = Notification.TYPE_DEAL_COMPLETED,
            dealId = dealId,
            dealTitle = deal.title,
            message = message,
        ))
    }

    // Direct cancel. Pending offers close immediately; accepted deals stay active until both
    // parties submit the required review rating, then markUserReviewed() finalizes cancellation.
    suspend fun cancelDeal(dealId: String, cancelledBy: String = "", reason: String = ""): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        var isPendingCancel = false
        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            val currentStatus = snap.getString("status")
            check(currentStatus in listOf(Constants.STATUS_PENDING, Constants.STATUS_ACCEPTED)) {
                "Only pending or active deals can be cancelled"
            }
            isPendingCancel = currentStatus == Constants.STATUS_PENDING
            val updates = if (isPendingCancel) {
                mapOf(
                    "status" to Constants.STATUS_CANCELLED,
                    "cancelledBy" to cancelledBy,
                    "cancelReason" to reason,
                    "cancelRequestedBy" to "",
                    "completionRequestedBy" to "",
                    "chatUnlocked" to true,
                    "updatedAt" to Timestamp.now(),
                )
            } else {
                mapOf(
                    "cancelRequestedBy" to cancelledBy,
                    "cancelReason" to reason,
                    "completionRequestedBy" to "",
                    "updatedAt" to Timestamp.now(),
                )
            }
            tx.update(ref, updates)
        }.await()

        val otherPartyId = if (cancelledBy == deal.creatorId) deal.brandId else deal.creatorId
        val actorName = userRepo.getUserLite(cancelledBy)?.displayName ?: "The other party"
        if (isPendingCancel) {
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
            notifRepo.createNotification(Notification(
                recipientId = otherPartyId,
                actorId = cancelledBy,
                actorName = actorName,
                type = Notification.TYPE_DEAL_CANCEL_REQUESTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$actorName\" requested to cancel \"${deal.title}\".",
            ))
        }
    }

    suspend fun requestCompletion(dealId: String, initiatorId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        deals.document(dealId).update(
            mapOf(
                "completionRequestedBy" to initiatorId,
                "updatedAt" to Timestamp.now(),
            )
        ).await()

        // Notify the other party that the initiator marked the deal complete and needs confirmation.
        val recipientId = if (initiatorId == deal.creatorId) deal.brandId else deal.creatorId
        val actorName = userRepo.getUserLite(initiatorId)?.displayName
            ?: if (initiatorId == deal.creatorId) "The creator" else "The brand"
        notifRepo.createNotification(
            Notification(
                recipientId = recipientId,
                actorId = initiatorId,
                actorName = actorName,
                type = Notification.TYPE_DEAL_COMPLETION_REQUESTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$actorName\" marked ${deal.title} as complete. Please confirm completion.",
            )
        )
    }

    suspend fun confirmCompletion(dealId: String): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            check(snap.getString("completionRequestedBy")?.isNotEmpty() == true) {
                "No completion request pending"
            }
            tx.update(ref, "updatedAt", Timestamp.now())
        }.await()
    }

    suspend fun declineCompletion(dealId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")
        val requesterId = deal.completionRequestedBy

        deals.document(dealId).update(
            mapOf(
                "completionRequestedBy" to "",
                "updatedAt" to Timestamp.now(),
            )
        ).await()

        if (requesterId.isNotBlank()) {
            val actorId = if (requesterId == deal.creatorId) deal.brandId else deal.creatorId
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

    // Kept for older callers; now cancellation is immediate and unilateral.
    suspend fun requestCancellation(dealId: String, initiatorId: String, reason: String = ""): Result<Unit> = runCatching {
        cancelDeal(dealId, initiatorId, reason).getOrThrow()
    }

    suspend fun confirmCancellation(dealId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")
        val initiator = deal.cancelRequestedBy
        cancelDeal(dealId, cancelledBy = initiator, reason = deal.cancelReason).getOrThrow()
    }

    suspend fun declineCancellation(dealId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")
        val requesterId = deal.cancelRequestedBy

        deals.document(dealId).update(
            mapOf(
                "cancelRequestedBy" to "",
                "cancelReason" to "",
                "updatedAt" to Timestamp.now(),
            )
        ).await()

        if (requesterId.isNotBlank()) {
            val actorId = if (requesterId == deal.creatorId) deal.brandId else deal.creatorId
            val actorName = userRepo.getUserLite(actorId)?.displayName ?: "The other party"
            notifRepo.createNotification(Notification(
                recipientId = requesterId,
                actorId = actorId,
                actorName = actorName,
                type = Notification.TYPE_DEAL_CANCEL_DECLINED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$actorName\" declined your cancellation request for \"${deal.title}\".",
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

    suspend fun markUserReviewed(dealId: String, userId: String): Result<Unit> = runCatching {
        val snap = deals.document(dealId).get().await()
        val deal = snap.toObject(Deal::class.java)?.copy(dealId = snap.id)
            ?: error("Deal not found")

        val isCreator = deal.creatorId == userId
        val field = if (isCreator) "creatorReviewedAt" else "brandReviewedAt"
        val otherPartyId = if (isCreator) deal.brandId else deal.creatorId
        val otherHasReviewed = if (isCreator) deal.brandReviewedAt != null else deal.creatorReviewedAt != null

        var dealSettled = false
        var settledAsCompleted = false
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
            val completionRequestedBy = fresh.getString("completionRequestedBy").orEmpty()
            val cancelRequestedBy = fresh.getString("cancelRequestedBy").orEmpty()

            if (bothReviewed && fresh.getString("status") == Constants.STATUS_ACCEPTED) {
                when {
                    completionRequestedBy.isNotBlank() -> {
                        updates["status"] = Constants.STATUS_COMPLETED
                        updates["completedAt"] = now
                        updates["completionRequestedBy"] = ""
                        dealSettled = true
                        settledAsCompleted = true
                    }
                    cancelRequestedBy.isNotBlank() -> {
                        updates["status"] = Constants.STATUS_CANCELLED
                        updates["cancelledBy"] = cancelRequestedBy
                        updates["cancelRequestedBy"] = ""
                        dealSettled = true
                        settledAsCompleted = false
                    }
                }
            }
            tx.update(ref, updates)
        }.await()

        val myName = userRepo.getUserLite(userId)?.displayName ?: "The other party"
        if (dealSettled) {
            // Both reviews are in and the deal transitioned — notify both parties
            val settledMsg = if (settledAsCompleted)
                "\"${deal.title}\" is now complete. Thanks for using Aura!"
            else
                "\"${deal.title}\" has been cancelled and both reviews are submitted."
            notifRepo.createNotification(Notification(
                recipientId = otherPartyId,
                actorId = userId,
                actorName = myName,
                type = if (settledAsCompleted) Notification.TYPE_DEAL_COMPLETED else Notification.TYPE_DEAL_CANCELED,
                dealId = dealId,
                dealTitle = deal.title,
                message = settledMsg,
            ))
        } else if (!otherHasReviewed) {
            // First reviewer — prompt the other party to submit their review
            notifRepo.createNotification(Notification(
                recipientId = otherPartyId,
                actorId = userId,
                actorName = myName,
                type = Notification.TYPE_REVIEW_REQUESTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$myName\" left a review for \"${deal.title}\". Leave yours to close the deal!",
                openReviewPopup = true,
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
