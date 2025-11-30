package com.example.decora

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Page8Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page8)

        val mainImage = findViewById<ImageView>(R.id.firstimage)
        val titleText = findViewById<TextView>(R.id.titleText)
        val usernameText = findViewById<TextView>(R.id.username)
        val profilePic =
            findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profilePic)

        val title = intent.getStringExtra("pinTitle")
        val imageBase64 = intent.getStringExtra("pinImageBase64")
        val username = intent.getStringExtra("username")
        val pfpBase64 = intent.getStringExtra("userPfpBase64")

        titleText.text = title ?: ""

        usernameText.text = username ?: ""


        if (!imageBase64.isNullOrEmpty()) {
            val bytes = android.util.Base64.decode(imageBase64, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            mainImage.setImageBitmap(bitmap)
        }
        if (!pfpBase64.isNullOrEmpty()) {
            val bytes = android.util.Base64.decode(pfpBase64, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            profilePic.setImageBitmap(bitmap)
        }
    }
}
