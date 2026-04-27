package com.aura.app.utils

import android.util.Patterns

/**
 * Shared validation helpers used by registration, login, edit profile,
 * account settings and forgot password.
 *
 * Password rule mirrors the registration screens:
 * minimum 8 characters, at least 1 uppercase letter, at least 1 number.
 */
object ValidationUtils {

    fun isNonBlank(value: String?): Boolean = !value.isNullOrBlank()

    fun isValidEmail(email: String?): Boolean =
        !email.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /** Accepts E.164-ish or US-formatted numbers. 7–15 digits after stripping separators. */
    fun isValidPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return true
        val digits = phone.filter { it.isDigit() }
        return digits.length in 7..15
    }

    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        return Patterns.WEB_URL.matcher(url).matches()
    }

    fun isValidPassword(password: String?): Boolean {
        if (password.isNullOrBlank()) return false
        if (password.length < 8) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isDigit() }) return false
        return true
    }

    /** Returns null if password is valid, otherwise a user-facing reason. */
    fun passwordError(password: String?): String? = when {
        password.isNullOrBlank() -> "Password required"
        password.length < 8 -> "Min 8 characters"
        !password.any { it.isUpperCase() } -> "Need 1 uppercase letter"
        !password.any { it.isDigit() } -> "Need 1 number"
        else -> null
    }

    fun doPasswordsMatch(password: String?, confirm: String?): Boolean =
        !password.isNullOrEmpty() && password == confirm
}
