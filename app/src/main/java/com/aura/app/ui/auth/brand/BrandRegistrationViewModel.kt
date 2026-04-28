package com.aura.app.ui.auth.brand

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.app.data.model.Campaign
import com.aura.app.data.repository.BrandRegistrationRepository
import com.aura.app.data.repository.CampaignRepository
import com.aura.app.utils.BudgetRanges
import com.aura.app.utils.SessionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
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
    private val repo: BrandRegistrationRepository = BrandRegistrationRepository(),
    private val campaignRepository: CampaignRepository = CampaignRepository(FirebaseFirestore.getInstance())
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
    var targetAudience: List<String> = emptyList()
    var website: String = ""
    var city: String = ""
    var state: String = ""
    var country: String = ""

    // Step 5
    var campaignName: String = ""
    var campaignBrief: String = ""
    var campaignGoals: List<String> = emptyList()
    var campaignBudgetRange: String = ""
    var campaignTimeline: Timestamp? = null
    var campaignDeliverables: List<String> = emptyList()

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
                targetAudience = targetAudience,
                website = website,
                city = city,
                state = state,
                country = country,
                campaignName = campaignName,
                campaignBrief = campaignBrief
            )

            _isLoading.value = false
            result.fold(
                onSuccess = { uid ->
                    val campaignResult = createInitialCampaignIfPresent(uid)
                    if (campaignResult.isFailure) {
                        _error.value = campaignResult.exceptionOrNull()?.message
                            ?: "Registration saved, but campaign creation failed."
                        return@fold
                    }
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

    private suspend fun createInitialCampaignIfPresent(brandId: String): Result<Unit> {
        if (campaignName.isBlank() && campaignBrief.isBlank()) return Result.success(Unit)
        if (campaignName.isBlank() || campaignBrief.isBlank()) {
            return Result.failure(Exception("Add both campaign title and description."))
        }

        val budget = BudgetRanges.toMinMaxCents(campaignBudgetRange)
        val result = campaignRepository.createCampaign(
            Campaign(
                brandId = brandId,
                title = campaignName,
                description = campaignBrief,
                goals = campaignGoals,
                budgetRange = campaignBudgetRange,
                budgetMin = budget.first,
                budgetMax = budget.second,
                timeline = campaignTimeline,
                deliverables = campaignDeliverables
            )
        )
        return result.map { Unit }
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BrandRegistrationViewModel() as T
    }
}
