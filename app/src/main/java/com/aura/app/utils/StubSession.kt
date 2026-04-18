package com.aura.app.utils

import android.content.Context

/**
 * Holds the active dummy user for testing. Call init() once from MainActivity.
 * When real auth lands, replace the internals with FirebaseAuth — call sites stay identical.
 */
object StubSession {
    private const val PREFS = "aura_stub_session"
    private const val KEY_ROLE = "role"

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun role(): String =
        prefs().getString(KEY_ROLE, Constants.ROLE_CREATOR) ?: Constants.ROLE_CREATOR

    fun userId(): String =
        if (role() == Constants.ROLE_CREATOR) StubData.CREATOR_ID else StubData.BRAND_ID

    fun displayName(): String = StubData.users[userId()]?.displayName ?: ""

    fun photoUrl(): String = StubData.users[userId()]?.profileImageUrl ?: ""

    fun switchRole() {
        val next = if (role() == Constants.ROLE_CREATOR) Constants.ROLE_BRAND else Constants.ROLE_CREATOR
        prefs().edit().putString(KEY_ROLE, next).apply()
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
