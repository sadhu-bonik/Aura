package com.aura.app.ui.feed

import com.aura.app.data.model.CreatorFeedEntry

sealed class FeedUiState {
    data object Loading : FeedUiState()
    data object Empty : FeedUiState()
    data class Content(val entries: List<CreatorFeedEntry>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}
