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
        deals.document(dealId).update(
            mapOf(
                "status" to Constants.STATUS_COMPLETED,
                "updatedAt" to Timestamp.now(),
                "completedAt" to Timestamp.now(),
            )
        ).await()
    }

    // Direct cancel — used only for unilateral pre-acceptance withdrawal (brand withdraws a
    // pending offer). After acceptance, callers must use the request/confirm flow below so
    // both parties have to agree.
    suspend fun cancelDeal(dealId: String, cancelledBy: String = "", reason: String = ""): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            val currentStatus = snap.getString("status")
            check(currentStatus == Constants.STATUS_PENDING) {
                "Accepted deals can only be cancelled with both-party approval"
            }
            tx.update(
                ref,
                mapOf(
                    "status" to Constants.STATUS_CANCELLED,
                    "cancelledBy" to cancelledBy,
                    "cancelReason" to reason,
                    "chatUnlocked" to true,
                    "updatedAt" to Timestamp.now(),
                )
            )
        }.await()

        // Pre-acceptance withdraw is always brand → creator. Notify the creator.
        val recipientId = if (cancelledBy == deal.brandId) deal.creatorId else deal.brandId
        val actorName = userRepo.getUserLite(cancelledBy)?.displayName
            ?: if (cancelledBy == deal.brandId) "The brand" else "The creator"
        notifRepo.createNotification(
            Notification(
                recipientId = recipientId,
                actorId = cancelledBy,
                actorName = actorName,
                type = Notification.TYPE_DEAL_RETRACTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "\"$actorName\" withdrew the deal request: ${deal.title}",
            )
        )
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
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")

        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            check(snap.getString("completionRequestedBy")?.isNotEmpty() == true) {
                "No completion request pending"
            }
            tx.update(
                ref,
                mapOf(
                    "status" to Constants.STATUS_COMPLETED,
                    "completedAt" to Timestamp.now(),
                    "completionRequestedBy" to "",
                    "chatUnlocked" to true,
                    "updatedAt" to Timestamp.now(),
                )
            )
        }.await()

        // Both sides have agreed — fan out REVIEW_REQUESTED to creator and brand.
        val brandName = userRepo.getUserLite(deal.brandId)?.displayName ?: "the brand"
        val creatorName = userRepo.getUserLite(deal.creatorId)?.displayName ?: "the creator"

        // Brand reviews creator
        notifRepo.createNotification(
            Notification(
                recipientId = deal.brandId,
                actorId = deal.creatorId,
                actorName = creatorName,
                type = Notification.TYPE_REVIEW_REQUESTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "Deal completed. Please review \"$creatorName\".",
                openReviewPopup = true,
            )
        )
        // Creator reviews brand
        notifRepo.createNotification(
            Notification(
                recipientId = deal.creatorId,
                actorId = deal.brandId,
                actorName = brandName,
                type = Notification.TYPE_REVIEW_REQUESTED,
                dealId = dealId,
                dealTitle = deal.title,
                message = "Deal completed. Please review \"$brandName\".",
                openReviewPopup = true,
            )
        )
    }

    suspend fun declineCompletion(dealId: String): Result<Unit> = runCatching {
        deals.document(dealId).update(
            mapOf(
                "completionRequestedBy" to "",
                "updatedAt" to Timestamp.now(),
            )
        ).await()
    }

    // After acceptance, cancellation needs both-party approval. The initiator opens a request,
    // the other side confirms or declines.
    suspend fun requestCancellation(dealId: String, initiatorId: String, reason: String = ""): Result<Unit> = runCatching {
        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            check(snap.getString("status") == Constants.STATUS_ACCEPTED) {
                "Cancellation requests are only valid for accepted deals"
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
    }

    suspend fun confirmCancellation(dealId: String): Result<Unit> = runCatching {
        val dealSnap = deals.document(dealId).get().await()
        val deal = dealSnap.toObject(Deal::class.java)?.copy(dealId = dealSnap.id)
            ?: error("Deal not found")
        val initiator = deal.cancelRequestedBy

        firestore.runTransaction { tx ->
            val ref = deals.document(dealId)
            val snap = tx.get(ref)
            val initiatorTx = snap.getString("cancelRequestedBy").orEmpty()
            check(initiatorTx.isNotEmpty()) { "No cancellation request pending" }
            tx.update(
                ref,
                mapOf(
                    "status" to Constants.STATUS_CANCELLED,
                    "cancelledBy" to initiatorTx,
                    "cancelRequestedBy" to "",
                    "chatUnlocked" to true,
                    "updatedAt" to Timestamp.now(),
                )
            )
        }.await()

        // Notify the original initiator (the side who proposed the cancellation) that it was confirmed.
        if (initiator.isNotBlank()) {
            val confirmerId = if (initiator == deal.creatorId) deal.brandId else deal.creatorId
            val confirmerName = userRepo.getUserLite(confirmerId)?.displayName
                ?: if (confirmerId == deal.brandId) "The brand" else "The creator"
            notifRepo.createNotification(
                Notification(
                    recipientId = initiator,
                    actorId = confirmerId,
                    actorName = confirmerName,
                    type = Notification.TYPE_DEAL_CANCELED,
                    dealId = dealId,
                    dealTitle = deal.title,
                    message = "\"$confirmerName\" confirmed cancellation of ${deal.title}.",
                )
            )
        }
    }

    suspend fun declineCancellation(dealId: String): Result<Unit> = runCatching {
        deals.document(dealId).update(
            mapOf(
                "cancelRequestedBy" to "",
                "cancelReason" to "",
                "updatedAt" to Timestamp.now(),
            )
        ).await()
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
        val field = if (deal.creatorId == userId) "creatorReviewedAt" else "brandReviewedAt"
        deals.document(dealId).update(
            mapOf(
                field to Timestamp.now(),
                "updatedAt" to Timestamp.now(),
            )
        ).await()
    }

    // Checks if a pending deal is older than 7 days and marks it expired locally.
    // The Firestore write is fire-and-forget; the returned deal reflects the new status.
    private fun expireIfStale(deal: Deal): Deal {
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
        return deal.copy(status = Constants.STATUS_EXPIRED)
    }
}
