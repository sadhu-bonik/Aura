package com.aura.app.ui.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.PortfolioItem
import com.aura.app.data.model.User
import com.aura.app.data.repository.PortfolioRepository
import com.aura.app.data.repository.StorageRepository
import com.aura.app.data.repository.UserRepository
import com.aura.app.utils.SessionManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(
        val user: User,
        val portfolio: List<PortfolioItem> = emptyList()
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

/** One-shot events surfaced to the Fragment via SharedFlow. */
sealed class UploadEvent {
    data object Started : UploadEvent()
    data class Progress(val message: String) : UploadEvent()
    data object Success : UploadEvent()
    data class Failure(val message: String) : UploadEvent()
}

/**
 * ProfileViewModel - Loads the signed-in user's profile and handles portfolio uploads.
 */
class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val portfolioRepository: PortfolioRepository = PortfolioRepository(),
    private val storageRepository: StorageRepository = StorageRepository(),
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _uploadEvent = MutableSharedFlow<UploadEvent>()
    val uploadEvent: SharedFlow<UploadEvent> = _uploadEvent.asSharedFlow()

    /** Guards against double-tap uploads. */
    private var isUploading = false

    companion object {
        const val MAX_PORTFOLIO_ITEMS = 10
        const val MAX_DURATION_SEC = 60L
    }

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _state.value = ProfileUiState.Loading
            val userId = sessionManager.getUserId()
            if (userId == null) {
                _state.value = ProfileUiState.Error("Not signed in")
                return@launch
            }

            val user = userRepository.getUserProfile(userId)
            if (user == null) {
                _state.value = ProfileUiState.Error("Could not load profile")
                return@launch
            }

            // Show the user info immediately, then stream portfolio items
            _state.value = ProfileUiState.Success(user, emptyList())

            portfolioRepository.getCreatorPortfolio(userId).collect { portfolio ->
                _state.value = ProfileUiState.Success(user, portfolio)
            }
        }
    }

    /**
     * Full upload pipeline: validate → upload to Storage → save metadata to Firestore.
     * If the Firestore write fails, the Storage file is rolled back (deleted).
     *
     * @param videoUri      Content URI from the device picker
     * @param mimeType      MIME type reported by ContentResolver (e.g. "video/mp4")
     * @param fileName      Original file name from the device
     * @param durationSec   Duration extracted via MediaMetadataRetriever
     */
    fun uploadPortfolioVideo(
        videoUri: Uri,
        mimeType: String,
        fileName: String,
        durationSec: Long,
    ) {
        if (isUploading) return // prevent double-tap

        viewModelScope.launch {
            isUploading = true
            _uploadEvent.emit(UploadEvent.Started)

            try {
                // --- Validate auth ---
                val userId = sessionManager.getUserId()
                if (userId == null) {
                    _uploadEvent.emit(UploadEvent.Failure("Not signed in"))
                    return@launch
                }

                // --- Validate MIME ---
                if (!mimeType.startsWith("video/")) {
                    _uploadEvent.emit(UploadEvent.Failure("Only video files are allowed"))
                    return@launch
                }

                // --- Validate duration ---
                if (durationSec > MAX_DURATION_SEC) {
                    _uploadEvent.emit(
                        UploadEvent.Failure("Video is too long (${durationSec}s). Max is ${MAX_DURATION_SEC}s.")
                    )
                    return@launch
                }

                // --- Validate portfolio size ---
                _uploadEvent.emit(UploadEvent.Progress("Checking portfolio limit…"))
                val currentCount = portfolioRepository.getPortfolioCount(userId)
                if (currentCount >= MAX_PORTFOLIO_ITEMS) {
                    _uploadEvent.emit(
                        UploadEvent.Failure("Portfolio is full ($MAX_PORTFOLIO_ITEMS videos max). Remove a video first.")
                    )
                    return@launch
                }

                // --- Upload to Firebase Storage ---
                _uploadEvent.emit(UploadEvent.Progress("Uploading video…"))
                val itemId = portfolioRepository.generateItemId()
                val extension = extensionFromMime(mimeType)

                val uploadResult = storageRepository.uploadPortfolioVideo(
                    userId = userId,
                    itemId = itemId,
                    uri = videoUri,
                    extension = extension,
                )

                // --- Save metadata to Firestore ---
                _uploadEvent.emit(UploadEvent.Progress("Saving portfolio data…"))
                val portfolioItem = PortfolioItem(
                    itemId = itemId,
                    creatorId = userId,
                    title = fileName,
                    mediaUrl = uploadResult.downloadUrl,
                    mediaType = "video",
                    storagePath = uploadResult.storagePath,
                    mimeType = mimeType,
                    originalFileName = fileName,
                    public = true,
                    createdAt = Timestamp.now(),
                )

                val saveResult = portfolioRepository.savePortfolioItem(portfolioItem)
                if (saveResult.isFailure) {
                    // Rollback: delete the orphaned Storage file
                    try {
                        storageRepository.deleteFile(uploadResult.storagePath)
                    } catch (_: Exception) {
                        // Best-effort rollback; log if we had analytics
                    }
                    _uploadEvent.emit(
                        UploadEvent.Failure("Failed to save portfolio data. Upload rolled back.")
                    )
                    return@launch
                }

                // The Firestore snapshot listener in loadProfile() will auto-refresh the list
                _uploadEvent.emit(UploadEvent.Success)

            } catch (e: Exception) {
                _uploadEvent.emit(UploadEvent.Failure(e.message ?: "Upload failed"))
            } finally {
                isUploading = false
            }
        }
    }

    private fun extensionFromMime(mimeType: String): String = when (mimeType) {
        "video/mp4" -> "mp4"
        "video/3gpp" -> "3gp"
        "video/webm" -> "webm"
        "video/x-matroska" -> "mkv"
        "video/quicktime" -> "mov"
        else -> "mp4"
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileViewModel(
                userRepository = UserRepository(),
                portfolioRepository = PortfolioRepository(),
                storageRepository = StorageRepository(),
                sessionManager = SessionManager(context)
            ) as T
    }
}
