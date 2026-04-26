package com.aura.app.ui.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.CreatorFeedEntry
import com.aura.app.data.repository.CreatorRankingRepository
import com.aura.app.data.repository.PortfolioRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.SessionManager
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VideoFeedViewModel(
    private val portfolioRepository: PortfolioRepository,
    val userRepository: UserRepository,
    private val rankingRepository: CreatorRankingRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state

    companion object {
        /** Max number of creator pages in the initial feed load */
        const val INITIAL_CREATOR_COUNT = 4
    }

    init {
        loadCreatorFeed()
    }

    /**
     * Loads the creator discovery feed with ranking.
     *
     * Workflow:
     * 1. Get current logged-in creator profile.
     * 2. Compute similarity ranking (niche tags) against other creators.
     * 3. Fetch portfolio items for the ranked IDs in order.
     * 4. Pre-resolve identity metadata (name/avatar).
     */
    fun loadCreatorFeed() {
        viewModelScope.launch {
            _state.value = FeedUiState.Loading

            val currentUserId = sessionManager.getUserId()
            if (currentUserId == null) {
                _state.value = FeedUiState.Error("Not signed in")
                return@launch
            }

            try {
                // Step 1: Resolve viewer tags — use industryTags for brands, niche tags for creators
                val userProfile = userRepository.getUserProfile(currentUserId)
                val viewerTags: List<String> = if (userProfile?.role == "brand") {
                    userRepository.getBrandProfile(currentUserId)?.industryTags ?: emptyList()
                } else {
                    userRepository.getCreatorProfile(currentUserId)?.tags ?: emptyList()
                }

                // Step 2: Get ranked creator IDs based on similarity
                val rankedIds = rankingRepository.getRankedCreatorIds(
                    currentUserId = currentUserId,
                    currentUserTags = viewerTags,
                    maxCreators = INITIAL_CREATOR_COUNT
                )

                // Step 3: Fetch feed content for these specific creators
                val entries = portfolioRepository.getDiscoveryFeed(
                    excludeUserId = currentUserId,
                    maxCreators = INITIAL_CREATOR_COUNT,
                    rankedCreatorIds = rankedIds
                )

                // Step 4: Pre-resolve creator name + avatar concurrently
                val resolved = entries.map { entry ->
                    async {
                        val user = runCatching { userRepository.getUserLite(entry.creatorId) }.getOrNull()
                        entry.copy(
                            creatorName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Unknown Creator",
                            creatorProfileImageUrl = user?.profileImageUrl ?: ""
                        )
                    }
                }.awaitAll()

                _state.value = if (resolved.isEmpty()) {
                    FeedUiState.Empty
                } else {
                    FeedUiState.Content(resolved)
                }
            } catch (e: Exception) {
                _state.value = FeedUiState.Error(e.message ?: "Failed to load feed")
            }
        }
    }

    /**
     * Loads additional creators into the feed.
     * Merges new entries with existing ones, deduplicating by creatorId.
     */
    fun loadMoreFeed() {
        val current = (_state.value as? FeedUiState.Content)?.entries ?: emptyList()
        val existingCreatorIds = current.map { it.creatorId }.toSet()

        viewModelScope.launch {
            val currentUserId = sessionManager.getUserId() ?: return@launch
            try {
                val moreEntries = portfolioRepository.getDiscoveryFeed(
                    excludeUserId = currentUserId,
                    maxCreators = INITIAL_CREATOR_COUNT,
                )

                val newEntries = moreEntries.filter { it.creatorId !in existingCreatorIds }
                if (newEntries.isEmpty()) return@launch

                val merged = (current + newEntries)
                    .sortedByDescending { it.items.first().createdAt?.seconds ?: 0L }
                _state.value = FeedUiState.Content(merged)
            } catch (_: Exception) {
                // Silently fail on pagination; the current feed is still showing
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VideoFeedViewModel(
                portfolioRepository = PortfolioRepository(),
                userRepository = UserRepository(),
                rankingRepository = CreatorRankingRepository(),
                sessionManager = SessionManager(context),
            ) as T
    }
}
