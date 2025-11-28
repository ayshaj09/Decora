package com.example.decora

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Page16Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page16)

        val srch=findViewById<ImageView>(R.id.search)
        srch.setOnClickListener {
            val intent= Intent(this, Page10Activity::class.java)
            startActivity(intent)
            finish()

        }

        val msg=findViewById<ImageView>(R.id.comment)
        msg.setOnClickListener {
            val intent= Intent(this, Page18Activity::class.java)
            startActivity(intent)
            finish()

        }
        val home=findViewById<ImageView>(R.id.home)
        home.setOnClickListener {
            val intent= Intent(this, Page7Activity::class.java)
            startActivity(intent)
            finish()
        }

        val saved=findViewById<TextView>(R.id.svd)
        saved.setOnClickListener {
            val intent= Intent(this, Page15Activity::class.java)
            startActivity(intent)
            finish()

        }

        val edited=findViewById<TextView>(R.id.edit)
        edited.setOnClickListener {
            val intent= Intent(this, Page17Activity::class.java)
            startActivity(intent)

        }
    }
}