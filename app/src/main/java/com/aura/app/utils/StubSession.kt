package com.aura.app.utils

import android.content.Context

/**
 * Holds the active dummy user for testing. Call init() once from MainActivity.
 * When real auth lands, replace internals with FirebaseAuth — call sites stay identical.
 */
object StubSession {
    private const val PREFS = "aura_stub_session"
    private const val KEY_USER_ID = "userId"

    private val userCycle = listOf(
        StubData.CREATOR_ID,
        StubData.BRAND_ID_NOVA,
        StubData.BRAND_ID_APEX,
    )

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun userId(): String =
        prefs().getString(KEY_USER_ID, StubData.CREATOR_ID) ?: StubData.CREATOR_ID

    fun role(): String = when (userId()) {
        StubData.CREATOR_ID -> Constants.ROLE_CREATOR
        else -> Constants.ROLE_BRAND
    }

    fun displayName(): String = StubData.users[userId()]?.displayName ?: ""

    fun photoUrl(): String = StubData.users[userId()]?.profileImageUrl ?: ""

    fun switchToNext() {
        val current = userId()
        val nextIndex = (userCycle.indexOf(current) + 1) % userCycle.size
        prefs().edit().putString(KEY_USER_ID, userCycle[nextIndex]).apply()
    }

    fun nextUserDisplayName(): String {
        val current = userId()
        val nextIndex = (userCycle.indexOf(current) + 1) % userCycle.size
        return StubData.users[userCycle[nextIndex]]?.displayName ?: ""
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
