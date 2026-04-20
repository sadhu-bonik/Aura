package com.aura.app.data.repository

import android.util.Log
import com.aura.app.data.model.CreatorProfile
import com.aura.app.utils.NicheMatcher
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * CreatorRankingRepository
 *
 * Responsible for:
 *   1. Fetching all candidate creator profiles from Firestore (excluding self).
 *   2. Comparing their niche tags against the logged-in creator's tags.
 *   3. Ranking the candidates so that:
 *        - creators with overlapping tags appear first (hasMatch = true)
 *        - among matching creators, more overlaps = higher rank
 *        - among non-matching creators, rank by portfolioCount then lastActiveAt
 *
 * Ranking is computed client-side in Kotlin because it is viewer-specific:
 * creator A's ideal feed order is different from creator B's, so storing a
 * global static order in Firestore would be wrong (and expensive to maintain).
 */
class CreatorRankingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    companion object {
        private const val TAG = "CreatorRanking"
        private const val COLLECTION_CREATOR_PROFILES = "creatorProfiles"
    }

    /**
     * Returns an ordered list of creatorIds for the discovery feed,
     * ranked by niche-tag similarity to the logged-in creator.
     *
     * @param currentUserId   UID of the logged-in creator (excluded from results).
     * @param currentUserTags Raw (non-normalized) niche tags of the logged-in creator.
     * @param maxCreators     Cap on the number of creators returned.
     */
    suspend fun getRankedCreatorIds(
        currentUserId: String,
        currentUserTags: List<String>,
        maxCreators: Int = 20,
    ): List<String> {
        // Normalize the viewer's own tags once — reused for every comparison below.
        val viewerTags = NicheMatcher.normalizeTags(currentUserTags)
        Log.d(TAG, "Viewer tags (normalized): $viewerTags")

        // ── Step 1: Fetch all creator profiles ──────────────────────────────────
        val allProfiles: List<CreatorProfile> = try {
            firestore.collection(COLLECTION_CREATOR_PROFILES)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(CreatorProfile::class.java) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch creator profiles: ${e.message}")
            return emptyList()
        }

        // ── Step 2: Filter: complete profiles only, excluding the viewer ────────
        val completeProfiles = allProfiles.filter {
            it.userId != currentUserId &&
            it.userId.isNotBlank() &&
            it.isProfileComplete
        }
        Log.d(TAG, "Candidate creator count (complete profiles): ${completeProfiles.size}")

        val candidates = if (completeProfiles.isEmpty()) {
            Log.w(TAG, "No complete profiles found, falling back to all creators.")
            allProfiles.filter { it.userId != currentUserId && it.userId.isNotBlank() }
        } else {
            completeProfiles
        }

        if (candidates.isEmpty()) return emptyList()

        // ── Step 3: Score each candidate ────────────────────────────────────────
        data class ScoredCreator(
            val creatorId: String,
            val overlapCount: Int,
            val hasMatch: Boolean,
            val portfolioCount: Int,
            val lastActiveAt: Long,
            val tags: List<String>
        )

        val scored = candidates.map { profile ->
            val candidateTags = NicheMatcher.normalizeTags(profile.tags)
            val overlap = NicheMatcher.overlapCount(viewerTags, candidateTags)
            val lastActive = profile.updatedAt.seconds

            Log.d(
                TAG,
                "Candidate [${profile.userId}]: tags=${profile.tags}, normalized=$candidateTags, overlap=$overlap"
            )

            ScoredCreator(
                creatorId = profile.userId,
                overlapCount = overlap,
                hasMatch = overlap > 0,
                portfolioCount = profile.portfolioCount,
                lastActiveAt = lastActive,
                tags = profile.tags
            )
        }

        // ── Step 4: Sort ────────────────────────────────────────────────────────
        //
        // Priority:
        //   1. hasMatch     → matches come first
        //   2. overlapCount → more matches = higher rank
        //   3. portfolioCount → handle fallback as requested
        //   4. lastActiveAt → tiebreak
        val sorted = scored.sortedWith(
            compareByDescending<ScoredCreator> { it.hasMatch }
                .thenByDescending { it.overlapCount }
                .thenByDescending { it.portfolioCount }
                .thenByDescending { it.lastActiveAt }
        )

        Log.d(TAG, "--- FINAL SORTED ORDER ---")
        sorted.forEachIndexed { index, sc ->
            Log.d(TAG, "#${index + 1}: ${sc.creatorId} | Overlap: ${sc.overlapCount} | Portfolio: ${sc.portfolioCount}")
        }

        return sorted.map { it.creatorId }.take(maxCreators)
    }
}
