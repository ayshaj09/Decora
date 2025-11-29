package com.example.decora

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Page18Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page18)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dm=findViewById<LinearLayout>(R.id.dm)
        dm.setOnClickListener {
            val intent= Intent(this, Page19Activity::class.java)
            startActivity(intent)
        }

        val msg=findViewById<TextView>(R.id.messages)
        msg.setOnClickListener {
            val intent= Intent(this, MessageActivity::class.java)
            startActivity(intent)
        }
    }
}