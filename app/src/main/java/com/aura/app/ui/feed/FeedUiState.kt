package com.aura.app.ui.feed

import com.aura.app.data.model.PortfolioItem

sealed class FeedUiState {
    data object Loading : FeedUiState()
    data object Empty : FeedUiState()
    data class Content(val items: List<PortfolioItem>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}
