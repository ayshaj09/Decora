package com.example.decora

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page21Activity : AppCompatActivity() {

    private var encodedImage = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page21)

        val pinImage = findViewById<ImageView>(R.id.pinImage)
        val nextButton = findViewById<Button>(R.id.nextButton)
        val titleInput = findViewById<EditText>(R.id.pinTitleInput)
        val descInput = findViewById<EditText>(R.id.pinDescInput)
        val linkInput = findViewById<EditText>(R.id.pinLink)


        val sharedPrefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val currentUserId = sharedPrefs.getString("user_id", "0")!!.toInt()


        val imageUriString = intent.getStringExtra("selectedImage")

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            pinImage.setImageURI(imageUri)

            // Encode image
            encodedImage = encodeImageToBase64(imageUri)
        }

        nextButton.setOnClickListener {

            val title = titleInput.text.toString().trim()
            val desc = descInput.text.toString().trim()
            val link = linkInput.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (encodedImage.isEmpty()) {
                Toast.makeText(this, "Image not loaded!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            uploadPin(encodedImage, title, desc, link, currentUserId)
        }
    }

    private fun encodeImageToBase64(uri: Uri): String {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()

        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun uploadPin(imageBase64: String, title: String, desc: String, link: String, userId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.BASE_URL + "upload_pin.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val json = JSONObject()
                json.put("title", title)
                json.put("description", desc)
                json.put("link", link)
                json.put("image", imageBase64)
                json.put("user_id", userId)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(json.toString())
                writer.flush()
                writer.close()

                val response = conn.inputStream.bufferedReader().readText()
                val obj = JSONObject(response)

                withContext(Dispatchers.Main) {
                    if (obj.optBoolean("success")) {
                        Toast.makeText(this@Page21Activity, "Pin Uploaded!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@Page21Activity, obj.optString("message"), Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Page21Activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}