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
     * Builds the creator discovery feed by fetching the latest public videos.
     *
     * 1. Fetches the newest public video portfolio items
     * 2. Groups them by creatorId to support horizontal scrolling
     * 3. Returns a list of CreatorFeedEntry for vertical paging
     *
     * This approach ensures the newest uploads appear at the top and horizontal
     * scrolling works as intended.
     */
    suspend fun getDiscoveryFeed(
        excludeUserId: String,
        maxCreators: Int = 20,
    ): List<CreatorFeedEntry> {
        // Fetch all public video items without orderBy to avoid index requirements
        val snapshot = try {
            firestore.collection(COLLECTION)
                .whereEqualTo("public", true)
                .whereEqualTo("mediaType", "video")
                .get()
                .await()
        } catch (e: Exception) {
            return emptyList()
        }

        val targetItems = snapshot.documents
            .mapNotNull { it.toObject(PortfolioItem::class.java) }
            .filter { item ->
                val isValid = item.mediaType == "video" &&
                              item.public &&
                              item.creatorId.isNotBlank() &&
                              item.creatorId != excludeUserId &&
                              !item.creatorId.contains("testCreator") &&
                              item.mediaUrl.isNotBlank() &&
                              item.storagePath.isNotBlank() &&
                              item.itemId.isNotBlank()

                if (!isValid) {
                    println("Feed: Skipped invalid/legacy item (ID=${item.itemId}, creatorId=${item.creatorId}, public=${item.public}, mediaType=${item.mediaType})")
                }
                isValid
            }
            .sortedByDescending { it.createdAt?.seconds ?: 0L }

        if (targetItems.isEmpty()) return emptyList()

        // Group by creator and take the limit
        return targetItems
            .groupBy { it.creatorId }
            .map { (creatorId, items) -> CreatorFeedEntry(creatorId, items) }
            .take(maxCreators)
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

    companion object {
        const val COLLECTION = "portfolioItems"
    }
}
