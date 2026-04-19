package com.aura.app.ui.auth.brand

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.repository.BrandRegistrationRepository
import kotlinx.coroutines.launch

/**
 * BrandRegistrationViewModel
 *
 * Steps 1–4: purely in-memory draft — no network calls, instant navigation.
 * Step 5:    calls completeRegistration() which does the single Firebase transaction
 *            (Auth + two Firestore writes) and emits registrationComplete on success.
 */
class BrandRegistrationViewModel(
    private val repo: BrandRegistrationRepository = BrandRegistrationRepository()
) : ViewModel() {

    // ── In-memory draft (no Firebase until Step 5) ────────────────────────────

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

    // Step 3
    var legalName: String = ""
    var repName: String = ""
    var companyEmail: String = ""
    var linkedinUrl: String = ""
    var twitterHandle: String = ""

    // Step 4
    var industryTags: List<String> = emptyList()
    var city: String = ""
    var state: String = ""
    var country: String = ""

    // Step 5
    var campaignName: String = ""
    var campaignBrief: String = ""

    // ── UI State ──────────────────────────────────────────────────────────────

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Emits true when the Firebase Auth + Firestore write succeeds on Step 5. */
    private val _registrationComplete = MutableLiveData(false)
    val registrationComplete: LiveData<Boolean> = _registrationComplete

    // ── Single Firebase call — triggered only on Step 5 ──────────────────────

    fun completeRegistration() {
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
                legalName = legalName,
                repName = repName,
                companyEmail = companyEmail,
                linkedinUrl = linkedinUrl,
                twitterHandle = twitterHandle,
                industryTags = industryTags,
                city = city,
                state = state,
                country = country,
                campaignName = campaignName,
                campaignBrief = campaignBrief
            )

            _isLoading.value = false
            result.fold(
                onSuccess = { _registrationComplete.value = true },
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
