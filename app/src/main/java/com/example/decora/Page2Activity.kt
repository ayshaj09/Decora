package com.example.decora

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Page2Activity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val login=findViewById<RelativeLayout>(R.id.pictures5)
        val signup=findViewById<RelativeLayout>(R.id.picture4)
        val forgotPass=findViewById<TextView>(R.id.forgotPass)

        forgotPass.setOnClickListener{
            val intent= Intent(this, Page3Activity::class.java)
            startActivity(intent)

        }

        login.setOnClickListener{
            val intent= Intent(this, Page5Activity::class.java)
            startActivity(intent)

        }

        signup.setOnClickListener{
            val intent= Intent(this, Page6Activity::class.java)
            startActivity(intent)

        }

    }
}