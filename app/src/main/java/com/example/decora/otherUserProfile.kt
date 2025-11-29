package com.example.decora

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.hdodenhof.circleimageview.CircleImageView

class otherUserProfile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_other_user_profile)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val incomingName = intent.getStringExtra("EXTRA_NAME") ?: "User"
        val incomingPfp = intent.getStringExtra("EXTRA_PFP") ?: ""


        val nameView = findViewById<TextView>(R.id.username)
        val handleView = findViewById<TextView>(R.id.userHandle) // Ensure you added this ID to XML
        val pfpView = findViewById<CircleImageView>(R.id.pfp)


        val homeBtn = findViewById<ImageView>(R.id.home)



        nameView.text = incomingName


        handleView.text = "@${incomingName.lowercase().replace(" ", "")}"


        if (incomingPfp.isNotEmpty()) {
            val bitmap = decodeBase64(incomingPfp)
            if (bitmap != null) {
                pfpView.setImageBitmap(bitmap)
            }
        }


        homeBtn.setOnClickListener {

            finish()
        }
    }


    private fun decodeBase64(input: String): Bitmap? {
        return try {
            val decodedByte = Base64.decode(input, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}