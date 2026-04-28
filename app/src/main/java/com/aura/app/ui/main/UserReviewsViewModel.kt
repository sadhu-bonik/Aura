package com.aura.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Review
import com.aura.app.data.repository.ReviewRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ReviewStats(
    val averageRating: Double = 0.0,
    val totalReviews: Long = 0L,
)

class UserReviewsViewModel(application: Application) : AndroidViewModel(application) {

    private val reviewRepo = ReviewRepository(application)

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _stats = MutableStateFlow(ReviewStats())
    val stats: StateFlow<ReviewStats> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentJob: Job? = null
    private var boundUserId: String? = null

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadReviewsForUser(userId: String) {
        if (userId == boundUserId) return
        boundUserId = userId

        currentJob?.cancel()

        _isLoading.value = true
        currentJob = viewModelScope.launch {
            reviewRepo.streamReviewsForUser(userId)
                .catch { e ->
                    android.util.Log.e("UserReviewsVM", "Failed to load reviews", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { reviewList ->
                    _reviews.value = reviewList
                    _isLoading.value = false

                    if (reviewList.isEmpty()) {
                        _stats.value = ReviewStats()
                    } else {
                        val avg = reviewList.map { it.rating }.average()
                        _stats.value = ReviewStats(
                            averageRating = avg,
                            totalReviews = reviewList.size.toLong(),
                        )
                    }
                }
        }
    }
}
