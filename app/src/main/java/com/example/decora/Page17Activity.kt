package com.example.decora

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page17Activity : AppCompatActivity() {

    private var selectedBitmap: Bitmap? = null
    private var currentUserId: Int = -1

    // Image Picker Launcher
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val pfp = findViewById<CircleImageView>(R.id.pfp)
            pfp.setImageURI(uri) // Update UI immediately

            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page17)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1


        val pfp = findViewById<CircleImageView>(R.id.pfp)
        val editBtn = findViewById<TextView>(R.id.edit)
        val doneBtn = findViewById<TextView>(R.id.done)
        val backBtn = findViewById<ImageView>(R.id.back)


        val nameInput = findViewById<LinearLayout>(R.id.name).getChildAt(1) as EditText
        val usernameInput = findViewById<LinearLayout>(R.id.username).getChildAt(1) as EditText
        val emailInput = findViewById<LinearLayout>(R.id.email).getChildAt(1) as EditText
        val phoneInput = findViewById<LinearLayout>(R.id.phno).getChildAt(1) as EditText


        loadCurrentUserProfile(pfp, nameInput, usernameInput, emailInput, phoneInput)


        editBtn.setOnClickListener {
            pickImage.launch("image/*")
        }


        doneBtn.setOnClickListener {
            val newName = nameInput.text.toString().trim()
            val newPhone = phoneInput.text.toString().trim() // <--- GET PHONE INPUT

            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convert Image if selected
            val imageString = if (selectedBitmap != null) bitmapToBase64(selectedBitmap!!) else ""


            updateProfile(newName, newPhone, imageString)
        }

        backBtn.setOnClickListener { finish() }
    }


    private fun updateProfile(name: String, phone: String, imageBase64: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.URL_UPDATE_PROFILE)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("user_id", currentUserId)
                jsonParam.put("full_name", name)
                jsonParam.put("phone", phone) // <--- SEND PHONE TO SERVER

                // Only send image if user changed it
                if (imageBase64.isNotEmpty()) {
                    jsonParam.put("profile_picture", imageBase64)
                }

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")
                    val message = json.optString("message")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Page17Activity, message, Toast.LENGTH_SHORT).show()
                        if (success) {
                            finish()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Page17Activity, "Error saving profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadCurrentUserProfile(targetImageView: ImageView, nameEt: EditText, userEt: EditText, emailEt: EditText, phoneEt: EditText) {
        if (currentUserId == -1) return

        val urlString = Config.BASE_URL + "get_user_profile.php"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("user_id", currentUserId)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()

                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    val user = json.getJSONObject("user")

                    val name = user.optString("full_name")
                    val email = user.optString("email")
                    val phone = user.optString("phone")
                    val pfpString = user.optString("profile_picture")

                    withContext(Dispatchers.Main) {
                        nameEt.setText(name)
                        emailEt.setText(email)
                        phoneEt.setText(phone)
                        // Make fake username from name
                        userEt.setText(name.lowercase().replace(" ", ""))

                        if (pfpString.isNotEmpty()) {
                            try {
                                val cleanBase64 = if (pfpString.contains(",")) pfpString.split(",")[1] else pfpString
                                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (bitmap != null) targetImageView.setImageBitmap(bitmap)
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
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
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }
}