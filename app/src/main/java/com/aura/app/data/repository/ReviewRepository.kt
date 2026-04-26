package com.aura.app.data.repository

import android.app.Application
import com.aura.app.data.model.Review
import com.aura.app.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ReviewRepository(private val app: Application) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun createReview(review: Review): Result<String> = runCatching {
        val reviewId = UUID.randomUUID().toString()
        val newReview = review.copy(reviewId = reviewId)

        val reviewsRef = firestore.collection(Constants.COLLECTION_REVIEWS)

        val existing = getExistingReview(review.dealId, review.reviewerId).getOrNull()
        if (existing != null) {
            throw IllegalStateException("Already reviewed")
        }

        val newRef = reviewsRef.document(reviewId)
        newRef.set(newReview).await()

        val profileRef = firestore.collection(Constants.COLLECTION_USERS).document(review.revieweeId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(profileRef)
            val currentAvg = snapshot.getDouble("averageRating") ?: 0.0
            val currentTotal = snapshot.getLong("totalReviews") ?: 0L

            val newTotal = currentTotal + 1
            val newAvg = ((currentAvg * currentTotal) + review.rating) / newTotal

            transaction.update(profileRef, "averageRating", newAvg)
            transaction.update(profileRef, "totalReviews", newTotal)
        }.await()

        reviewId
    }

    suspend fun updateReviewComment(reviewId: String, comment: String): Result<Unit> = runCatching {
        firestore.collection(Constants.COLLECTION_REVIEWS).document(reviewId).update("comment", comment).await()
    }

    fun streamMyReviews(reviewerId: String): Flow<Map<String, Review>> =
        firestore.collection(Constants.COLLECTION_REVIEWS)
            .whereEqualTo("reviewerId", reviewerId)
            .snapshots()
            .map { snap ->
                snap.documents
                    .mapNotNull { it.toObject(Review::class.java) }
                    .associateBy { it.dealId }
            }

    suspend fun getExistingReview(dealId: String, reviewerId: String): Result<Review?> = runCatching {
        val snapshot = firestore.collection(Constants.COLLECTION_REVIEWS)
            .whereEqualTo("dealId", dealId)
            .whereEqualTo("reviewerId", reviewerId)
            .get()
            .await()

        if (snapshot.isEmpty) {
            null
        } else {
            snapshot.documents[0].toObject(Review::class.java)
        }
    }
}
