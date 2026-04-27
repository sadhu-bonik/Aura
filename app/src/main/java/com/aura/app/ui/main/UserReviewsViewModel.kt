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
import kotlinx.coroutines.launch

class UserReviewsViewModel(application: Application) : AndroidViewModel(application) {

    private val reviewRepo = ReviewRepository(application)

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentJob: Job? = null
    private var boundUserId: String? = null

    fun loadReviewsForUser(userId: String) {
        if (userId == boundUserId) return
        boundUserId = userId

        currentJob?.cancel()
        
        _isLoading.value = true
        currentJob = viewModelScope.launch {
            reviewRepo.streamReviewsForUser(userId).collect { reviewList ->
                _reviews.value = reviewList
                _isLoading.value = false
            }
        }
    }
}
