package com.aura.app.ui.common

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.aura.app.R
import com.aura.app.utils.StubSession

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StubSession.init(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
    }
}
