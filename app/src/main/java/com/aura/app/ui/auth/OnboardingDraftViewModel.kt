package com.aura.app.ui.auth

import androidx.lifecycle.ViewModel
import com.aura.app.ui.auth.brand.BrandRegistrationViewModel

class OnboardingDraftViewModel : ViewModel() {
    var email: String = ""
    var password: String = ""
    var firstName: String = ""
    var lastName: String = ""
    var phone: String = ""
    var securityQuestion: String = ""
    var securityAnswer: String = ""
    var role: String = ""

    val fullName: String
        get() = listOf(firstName, lastName)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")

    fun reset() {
        email = ""
        password = ""
        firstName = ""
        lastName = ""
        phone = ""
        securityQuestion = ""
        securityAnswer = ""
        role = ""
    }

    fun applyToCreator(registrationViewModel: RegistrationViewModel) {
        registrationViewModel.email = email
        registrationViewModel.password = password
        registrationViewModel.fullName = fullName
        registrationViewModel.phone = phone
        registrationViewModel.securityQuestion = securityQuestion
        registrationViewModel.securityAnswer = securityAnswer
        registrationViewModel.setUserRole("creator")
    }

    fun applyToBrand(brandRegistrationViewModel: BrandRegistrationViewModel) {
        brandRegistrationViewModel.email = email
        brandRegistrationViewModel.password = password
        brandRegistrationViewModel.phone = phone
        brandRegistrationViewModel.securityQuestion = securityQuestion
        brandRegistrationViewModel.securityAnswer = securityAnswer
    }
}
