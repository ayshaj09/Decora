package com.example.decora

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page6Activity : AppCompatActivity() {


    private var selectedBitmap: Bitmap? = null


    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {

            findViewById<CircleImageView>(R.id.pfp).setImageURI(uri)

            try {

                selectedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page6)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val emailInput = findViewById<EditText>(R.id.email)
        val nameInput = findViewById<EditText>(R.id.yourname)
        val phoneInput = findViewById<EditText>(R.id.phoneNumber)
        val passInput = findViewById<EditText>(R.id.password)
        val confirmPassInput = findViewById<EditText>(R.id.confirmPassword)
        val createAccountBtn = findViewById<TextView>(R.id.createAccount)
        val backBtn = findViewById<ImageView>(R.id.back)
        val doneBtn = findViewById<TextView>(R.id.done)


        val editProfileBtn = findViewById<TextView>(R.id.edit)

        // Navigation
        backBtn.setOnClickListener { finish() }
        doneBtn.setOnClickListener { finish() }

        // 2. Open Gallery when clicked
        editProfileBtn.setOnClickListener {
            pickImage.launch("image/*")
        }


        createAccountBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val pass = passInput.text.toString().trim()
            val confirmPass = confirmPassInput.text.toString().trim()

            // Validation
            if (email.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val imageString = if (selectedBitmap != null) bitmapToBase64(selectedBitmap!!) else ""

            registerUser(name, email, phone, pass, imageString)
        }
    }


    private fun bitmapToBase64(bitmap: Bitmap): String {

        val resizedBitmap = getResizedBitmap(bitmap, 500)

        val byteArrayOutputStream = ByteArrayOutputStream()

        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream)

        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            // Landscape
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            // Portrait
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun registerUser(name: String, email: String, phone: String, pass: String, imageBase64: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Using Config file for URL
                val url = URL(Config.URL_SIGNUP)

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("full_name", name)
                jsonParam.put("email", email)
                jsonParam.put("phone", phone)
                jsonParam.put("password", pass)
                // 4. Send the RESIZED image string
                jsonParam.put("profile_picture", imageBase64)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonParam.toString())
                writer.flush()

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseText)

                    val success = jsonResponse.optBoolean("success")
                    val message = jsonResponse.optString("message")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Page6Activity, message, Toast.LENGTH_SHORT).show()
                        if (success) {
                            val intent= Intent(this@Page6Activity, Page7Activity::class.java)
                            startActivity(intent)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Page6Activity, "Server Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Page6Activity, "Connection Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}