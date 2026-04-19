package com.aura.app.ui.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.CreatorFeedEntry
import com.aura.app.data.repository.PortfolioRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VideoFeedViewModel(
    private val portfolioRepository: PortfolioRepository,
    val userRepository: UserRepository,
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
     * Loads the creator discovery feed.
     *
     * Two-step Firestore flow:
     * 1. Query `users` where role == "creator", exclude current user
     * 2. For each creator, query their public video `portfolioItems`
     *
     * Result: List<CreatorFeedEntry> where each entry = one vertical page
     * containing that creator's videos for horizontal scrolling.
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
                val entries = portfolioRepository.getDiscoveryFeed(
                    excludeUserId = currentUserId,
                    maxCreators = INITIAL_CREATOR_COUNT,
                )

                _state.value = if (entries.isEmpty()) {
                    FeedUiState.Empty
                } else {
                    FeedUiState.Content(entries)
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
                sessionManager = SessionManager(context),
            ) as T
    }
}
