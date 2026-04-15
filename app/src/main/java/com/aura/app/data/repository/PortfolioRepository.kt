package com.aura.app.data.repository

import com.aura.app.data.model.PortfolioItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PortfolioRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun streamPublicVideos(limit: Long = 50L): Flow<List<PortfolioItem>> =
        firestore.collection(COLLECTION)
            .whereEqualTo("mediaType", "video")
            .whereEqualTo("isPublic", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toObject(PortfolioItem::class.java) } }

    private companion object {
        const val COLLECTION = "portfolioItems"
    }
}
