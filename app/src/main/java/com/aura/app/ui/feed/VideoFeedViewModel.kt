package com.aura.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.repository.PortfolioRepository
import com.aura.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class VideoFeedViewModel(
    private val portfolioRepository: PortfolioRepository,
    val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state

    init {
        viewModelScope.launch {
            portfolioRepository.streamPublicVideos()
                .catch { t -> _state.value = FeedUiState.Error(t.message ?: "Unknown error") }
                .collect { items ->
                    _state.value = if (items.isEmpty()) FeedUiState.Empty
                    else FeedUiState.Content(items)
                }
        }
    }

    class Factory(
        private val portfolioRepository: PortfolioRepository = PortfolioRepository(),
        private val userRepository: UserRepository = UserRepository(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VideoFeedViewModel(portfolioRepository, userRepository) as T
    }
}
