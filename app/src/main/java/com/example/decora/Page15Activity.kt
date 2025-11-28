package com.example.decora

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.w3c.dom.Text

class Page15Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page15)

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
        val home=findViewById<ImageView>(R.id.home)
        home.setOnClickListener {
            val intent= Intent(this, Page7Activity::class.java)
            startActivity(intent)
            finish()
        }

        val created=findViewById<TextView>(R.id.created)
        created.setOnClickListener {
            val intent= Intent(this, Page16Activity::class.java)
            startActivity(intent)
            finish()

        }

        val edited=findViewById<TextView>(R.id.edit)
        created.setOnClickListener {
            val intent= Intent(this, Page17Activity::class.java)
            startActivity(intent)

        }



    }
}