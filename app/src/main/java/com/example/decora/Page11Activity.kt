package com.example.decora

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Page11Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page11)

        val userSearch=findViewById<TextView>(R.id.userSearch)

        userSearch.setOnClickListener{
            val intent= Intent(this, userSearchActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}