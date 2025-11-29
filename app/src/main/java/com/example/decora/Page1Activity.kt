package com.example.decora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Page1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page1)

        // Fix for system bars (Keep this as you requested)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginAndRedirect()
        }, 5000)
    }

    private fun checkLoginAndRedirect() {
        // 1. Check local storage for login flag
        val sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            // 2. User is already logged in -> Go to Page 7
            val intent = Intent(this, Page7Activity::class.java)
            startActivity(intent)
        } else {
            // 3. Not logged in -> Go to Page 2 (Login)
            val intent = Intent(this, Page2Activity::class.java)
            startActivity(intent)
        }

        finish() // Close splash screen so back button doesn't return here
    }
}