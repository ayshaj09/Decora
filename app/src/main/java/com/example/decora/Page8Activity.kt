package com.example.decora

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page8Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page8)

        val mainImage = findViewById<ImageView>(R.id.firstimage)
        val titleText = findViewById<TextView>(R.id.titleText)
        val usernameText = findViewById<TextView>(R.id.username)
        val profilePic = findViewById<CircleImageView>(R.id.profilePic)

        val pinId = intent.getIntExtra("pinId", -1)
        if (pinId == -1) return

        // Fetch the full pin details from backend
        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val url = URL(Config.BASE_URL + "get_single_pin.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                // Send { "pin_id" : X }
                val json = JSONObject()
                json.put("pin_id", pinId)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(json.toString())
                writer.flush()
                writer.close()

                val response = conn.inputStream.bufferedReader().readText()
                val obj = JSONObject(response)

                if (!obj.getBoolean("success")) return@launch

                val pin = obj.getJSONObject("pin")

                val title = pin.getString("title")
                val username = pin.getString("username")
                val imageBase64 = pin.getString("image")
                val pfpBase64 = pin.optString("user_pfp", "")

                // Update UI
                withContext(Dispatchers.Main) {
                    titleText.text = title
                    usernameText.text = username

                    // Decode main pin image
                    if (imageBase64.isNotEmpty()) {
                        val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        mainImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    }

                    // Decode user profile picture
                    if (pfpBase64.isNotEmpty()) {
                        val clean = if (pfpBase64.contains(",")) {
                            pfpBase64.split(",")[1]
                        } else pfpBase64

                        val bytes = Base64.decode(clean, Base64.DEFAULT)
                        profilePic.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
