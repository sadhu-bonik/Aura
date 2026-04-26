package com.aura.app.data.repository

import com.aura.app.data.model.CreatorFeedEntry
import com.aura.app.data.model.PortfolioItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PortfolioRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun streamPublicVideos(limit: Long = 50L): Flow<List<PortfolioItem>> =
        firestore.collection(COLLECTION)
            .whereEqualTo("public", true)
            .whereEqualTo("mediaType", "video")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toObject(PortfolioItem::class.java) } }

    fun getCreatorPortfolio(creatorId: String): Flow<List<PortfolioItem>> =
        firestore.collection(COLLECTION)
            .whereEqualTo("creatorId", creatorId)
            .snapshots()
            .map { snap -> 
                snap.documents
                    .mapNotNull { it.toObject(PortfolioItem::class.java) }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }

    /**
     * Builds the creator discovery feed using a Creator-First strategy:
     *
     * 1. Query `users` collection for all users with role == "creator" (excluding self)
     *    OR use a pre-ranked list of creator IDs provided by [rankedCreatorIds].
     * 2. For each discovered creatorId, query `portfolioItems` where creatorId matches.
     * 3. Group items by creator → each entry = one vertical page.
     */
    suspend fun getDiscoveryFeed(
        excludeUserId: String,
        maxCreators: Int = 20,
        rankedCreatorIds: List<String>? = null
    ): List<CreatorFeedEntry> {
        // Step 1: Determine which creators to fetch
        val creatorIds = if (rankedCreatorIds != null) {
            rankedCreatorIds
        } else {
            try {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("role", "creator")
                    .get()
                    .await()
                snapshot.documents
                    .mapNotNull { it.getString("userId") ?: it.id.takeIf { id -> id.isNotBlank() } }
                    .filter { it != excludeUserId && it.isNotBlank() }
            } catch (e: Exception) {
                return emptyList()
            }
        }

        if (creatorIds.isEmpty()) return emptyList()

        // Step 2: For each creator, fetch their public portfolio videos
        val allItems = mutableListOf<PortfolioItem>()
        val batches = creatorIds.chunked(30)
        for (batch in batches) {
            try {
                val snapshot = firestore.collection(COLLECTION)
                    .whereIn("creatorId", batch)
                    .whereEqualTo("public", true)
                    .whereEqualTo("mediaType", "video")
                    .get()
                    .await()
                snapshot.documents
                    .mapNotNull { it.toObject(PortfolioItem::class.java) }
                    .filter { item ->
                        item.mediaUrl.isNotBlank() &&
                        item.itemId.isNotBlank() &&
                        item.creatorId.isNotBlank()
                    }
                    .also { allItems.addAll(it) }
            } catch (e: Exception) {
                // skip failed batches
            }
        }

        if (allItems.isEmpty()) return emptyList()

        // Step 3: Group by creatorId, sort videos within each group newest-first
        val grouped = allItems
            .groupBy { it.creatorId }
            .map { (creatorId, items) ->
                CreatorFeedEntry(
                    creatorId = creatorId,
                    items = items.sortedByDescending { it.createdAt?.seconds ?: 0L }
                )
            }

        // Step 4: Sort creator groups. 
        // If we have ranked IDs, we MUST maintain that specific order.
        // Otherwise, sort by most recent video.
        return if (rankedCreatorIds != null) {
            grouped.sortedBy { entry -> rankedCreatorIds.indexOf(entry.creatorId) }
        } else {
            grouped.sortedByDescending { it.items.first().createdAt?.seconds ?: 0L }
        }.take(maxCreators)
    }

    /**
     * Queries the `users` collection for all users with role == "creator",
     * excluding the specified userId. Errors propagate to the caller.
     */
    suspend fun getCreatorUserIds(excludeUserId: String): List<String> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", "creator")
                .get()
                .await()
            snapshot.documents
                .mapNotNull { it.getString("userId") ?: it.id }
                .filter { it != excludeUserId && it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Returns the current count of portfolio items for a creator.
     * Used to enforce the max-10-items limit before starting an upload.
     */
    suspend fun getPortfolioCount(creatorId: String): Int {
        val snapshot = firestore.collection(COLLECTION)
            .whereEqualTo("creatorId", creatorId)
            .get()
            .await()
        return snapshot.size()
    }

    /**
     * Generates a new auto-ID for a portfolio item document (without writing yet).
     */
    fun generateItemId(): String =
        firestore.collection(COLLECTION).document().id

    /**
     * Saves a portfolio item document to Firestore.
     * The document ID is the itemId field in the data class.
     */
    suspend fun savePortfolioItem(item: PortfolioItem): Result<Unit> {
        return try {
            firestore.collection(COLLECTION)
                .document(item.itemId)
                .set(item)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a portfolio item document from Firestore.
     * Storage file deletion (best-effort rollback) is handled by the caller.
     */
    suspend fun deletePortfolioItem(itemId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION).document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION = "portfolioItems"
    }
}
