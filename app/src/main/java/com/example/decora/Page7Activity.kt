package com.example.decora

import android.content.Intent
import android.media.Image
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.hdodenhof.circleimageview.CircleImageView

class Page7Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page7)
        val srch=findViewById<ImageView>(R.id.search)
        srch.setOnClickListener {
            val intent= Intent(this, Page10Activity::class.java)
            startActivity(intent)
            finish()
        }

        val msg=findViewById<ImageView>(R.id.dm)
        msg.setOnClickListener {
            val intent= Intent(this, Page18Activity::class.java)
            startActivity(intent)
            finish()
        }

        val pfp=findViewById<CircleImageView>(R.id.prof)
        pfp.setOnClickListener {
            val intent= Intent(this, Page15Activity::class.java)
            startActivity(intent)
            finish()
        }
    }
}