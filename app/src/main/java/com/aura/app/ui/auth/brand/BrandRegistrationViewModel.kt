package com.aura.app.ui.auth.brand

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.repository.BrandRegistrationRepository
import com.aura.app.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * BrandRegistrationViewModel
 *
 * Steps 1–4: purely in-memory draft — no network calls, instant navigation.
 * Step 4 final: calls completeRegistration(context) which fires:
 *   1. Firebase Auth (create user)
 *   2. Storage  → upload logo + verification doc (if provided)
 *   3. Firestore users/{uid}         → user document
 *   4. Firestore brandProfiles/{uid} → full brand profile with URLs
 *   5. SessionManager.saveUserId()   → persists auth session locally
 */
class BrandRegistrationViewModel(
    private val repo: BrandRegistrationRepository = BrandRegistrationRepository()
) : ViewModel() {

    // ── In-memory draft ───────────────────────────────────────────────────────

    // Step 1
    var brandName: String = ""
    var email: String = ""
    var password: String = ""
    var phone: String = ""
    var securityQuestion: String = ""
    var securityAnswer: String = ""

    // Step 2
    var motto: String = ""
    var bio: String = ""
    var logoUri: Uri? = null

    // Step 3
    var legalName: String = ""
    var repName: String = ""
    var companyEmail: String = ""
    var linkedinUrl: String = ""
    var twitterHandle: String = ""
    var verificationFileUri: Uri? = null
    var verificationFileName: String = ""
    var verificationFileMimeType: String = ""

    // Step 4
    var industryTags: List<String> = emptyList()
    var city: String = ""
    var state: String = ""
    var country: String = ""

    // Step 5 (optional — kept for future campaign creation)
    var campaignName: String = ""
    var campaignBrief: String = ""

    // ── UI State ──────────────────────────────────────────────────────────────

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _registrationComplete = MutableLiveData(false)
    val registrationComplete: LiveData<Boolean> = _registrationComplete

    // ── Single Firebase call — triggered at Step 4 ───────────────────────────

    fun completeRegistration(context: android.content.Context) {
        if (_isLoading.value == true) return  // double-tap guard
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val result = repo.registerBrand(
                brandName = brandName,
                email = email,
                password = password,
                phone = phone,
                securityQuestion = securityQuestion,
                securityAnswer = securityAnswer,
                motto = motto,
                bio = bio,
                logoUri = logoUri,
                legalName = legalName,
                repName = repName,
                companyEmail = companyEmail,
                linkedinUrl = linkedinUrl,
                twitterHandle = twitterHandle,
                verificationFileUri = verificationFileUri,
                verificationFileName = verificationFileName,
                verificationFileMimeType = verificationFileMimeType,
                industryTags = industryTags,
                city = city,
                state = state,
                country = country,
                campaignName = campaignName,
                campaignBrief = campaignBrief
            )

            _isLoading.value = false
            result.fold(
                onSuccess = { uid ->
                    // Persist session so feed/profile screens recognise the signed-in user
                    SessionManager(context).saveUserId(uid)
                    _registrationComplete.value = true
                },
                onFailure = { e -> _error.value = e.message ?: "Registration failed." }
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun clearError() { _error.value = null }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BrandRegistrationViewModel() as T
    }
}
