package com.aura.app.data.repository

import android.app.Application
import com.aura.app.data.model.Review
import com.aura.app.utils.Constants
import com.aura.app.utils.StubState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ReviewRepository(private val app: Application) {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun createReview(review: Review): Result<String> = runCatching {
        val reviewId = UUID.randomUUID().toString()
        val newReview = review.copy(reviewId = reviewId)

        if (Constants.USE_STUBS) {
            StubState.addReview(newReview)
            return@runCatching reviewId
        }

        val reviewsRef = firestore.collection(Constants.COLLECTION_REVIEWS)

        val existing = getExistingReview(review.dealId, review.reviewerId).getOrNull()
        if (existing != null) {
            throw IllegalStateException("Already reviewed")
        }

        val dealSnap = firestore.collection(Constants.COLLECTION_DEALS).document(review.dealId).get().await()
        val dealStatus = dealSnap.getString("status").orEmpty()
        val bothClosedConfirmed =
            dealSnap.getBoolean("brandCloseConfirmed") == true &&
            dealSnap.getBoolean("creatorCloseConfirmed") == true
        val hasPendingClosure =
            bothClosedConfirmed ||
            (dealStatus == Constants.STATUS_ACCEPTED &&
                (dealSnap.getString("completionRequestedBy").orEmpty().isNotBlank() ||
                 dealSnap.getString("cancelRequestedBy").orEmpty().isNotBlank()))
        check(dealStatus in listOf(Constants.STATUS_COMPLETED, Constants.STATUS_CANCELLED) || hasPendingClosure) {
            "Reviews can only be submitted after both parties agree to close the deal"
        }

        val newRef = reviewsRef.document(reviewId)
        newRef.set(newReview).await()

        // Aggregate rating goes onto the reviewee's role-specific profile doc
        // (creatorProfiles/{id} or brandProfiles/{id}). This is what the profile screen reads.
        val profileCollection = when (review.revieweeRole) {
            Constants.ROLE_CREATOR -> Constants.COLLECTION_CREATOR_PROFILES
            Constants.ROLE_BRAND -> Constants.COLLECTION_BRAND_PROFILES
            else -> Constants.COLLECTION_USERS // fallback if role missing — keeps old behavior
        }
        val profileRef = firestore.collection(profileCollection).document(review.revieweeId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(profileRef)
            val currentAvg = snapshot.getDouble("averageRating") ?: 0.0
            val currentTotal = snapshot.getLong("totalReviews") ?: 0L

            val newTotal = currentTotal + 1
            val newAvg = ((currentAvg * currentTotal) + review.rating) / newTotal

            // Set (not update) to also create the field if the doc lacks it.
            transaction.set(
                profileRef,
                mapOf("averageRating" to newAvg, "totalReviews" to newTotal),
                com.google.firebase.firestore.SetOptions.merge(),
            )
        }.await()

        reviewId
    }

    suspend fun updateReviewComment(reviewId: String, comment: String): Result<Unit> = runCatching {
        if (Constants.USE_STUBS) {
            StubState.updateReviewComment(reviewId, comment)
            return@runCatching
        }
        firestore.collection(Constants.COLLECTION_REVIEWS).document(reviewId).update("comment", comment).await()
    }

    /** Reviews authored by [reviewerId] — used for duplicate detection. */
    fun streamMyReviews(reviewerId: String): Flow<Map<String, Review>> {
        if (Constants.USE_STUBS) {
            return StubState.stubReviews.map { list ->
                list.filter { it.reviewerId == reviewerId }.associateBy { it.dealId }
            }
        }
        return firestore.collection(Constants.COLLECTION_REVIEWS)
            .whereEqualTo("reviewerId", reviewerId)
            .snapshots()
            .map { snap ->
                snap.documents
                    .mapNotNull { it.toObject(Review::class.java) }
                    .associateBy { it.dealId }
            }
    }

    /** Reviews received by [revieweeId] — feeds the user's reviews list screen. */
    fun streamReviewsForUser(revieweeId: String): Flow<List<Review>> {
        if (Constants.USE_STUBS) {
            return StubState.stubReviews.map { list ->
                list.filter { it.revieweeId == revieweeId }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        }
        return firestore.collection(Constants.COLLECTION_REVIEWS)
            .whereEqualTo("revieweeId", revieweeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { it.toObject(Review::class.java) }
            }
    }

    suspend fun getExistingReview(dealId: String, reviewerId: String): Result<Review?> = runCatching {
        if (Constants.USE_STUBS) {
            return@runCatching StubState.stubReviews.value.firstOrNull {
                it.dealId == dealId && it.reviewerId == reviewerId
            }
        }

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
