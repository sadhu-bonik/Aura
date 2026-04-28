package com.aura.app.ui.common

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.aura.app.R
import com.aura.app.utils.SessionManager
import com.aura.app.utils.StubSession
import com.google.firebase.auth.FirebaseAuth
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTIF_ID = "notifId"
        const val EXTRA_DEAL_ID = "dealId"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        StubSession.init(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment)) { v, insets ->
            val insetsType = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            val windowInsets = insets.getInsets(insetsType)
            v.setPadding(windowInsets.left, windowInsets.top, windowInsets.right, windowInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        if (savedInstanceState == null) {
            restoreAuthenticatedSession()
        }
    }

    private fun restoreAuthenticatedSession() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return
        SessionManager(this).saveUserId(firebaseUser.uid)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHost.navController
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.welcomeFragment, inclusive = true)
            .build()
        navController.navigate(R.id.homeContainerFragment, null, navOptions)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val focusedView = currentFocus
            if (focusedView is EditText) {
                val focusedBounds = Rect()
                focusedView.getGlobalVisibleRect(focusedBounds)
                if (!focusedBounds.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    focusedView.clearFocus()
                    hideKeyboard(focusedView)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun hideKeyboard(view: android.view.View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
