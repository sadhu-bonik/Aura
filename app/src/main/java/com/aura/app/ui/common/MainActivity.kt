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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StubSession.init(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Keep the host edge-to-edge on the bottom so bottom-nav screens can anchor to the
            // physical screen edge and handle navigation-bar insets themselves.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
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
