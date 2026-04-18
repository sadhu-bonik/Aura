package com.aura.app.data.repository

import com.aura.app.data.model.Deal
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
) {
    private val deals = firestore.collection(Constants.COLLECTION_DEALS)

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
    }

    suspend fun rejectDeal(dealId: String): Result<Unit> = runCatching {
        deals.document(dealId).update(
            mapOf(
                "status" to Constants.STATUS_REJECTED,
                "updatedAt" to Timestamp.now(),
            )
        ).await()
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

    suspend fun cancelDeal(dealId: String): Result<Unit> = runCatching {
        deals.document(dealId).update(
            mapOf(
                "status" to Constants.STATUS_CANCELLED,
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
