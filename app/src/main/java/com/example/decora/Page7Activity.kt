package com.example.decora

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.decora.com.example.decora.Config
import com.google.firebase.messaging.FirebaseMessaging
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page7Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page7)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            val post = findViewById<ImageView>(R.id.post)

            post.setOnClickListener {
                openGallery()
            }

            insets
        }

        val srch = findViewById<ImageView>(R.id.search)
        srch.setOnClickListener {
            val intent = Intent(this, Page10Activity::class.java)
            startActivity(intent)
            finish()
        }

        val msg = findViewById<ImageView>(R.id.dm)
        msg.setOnClickListener {
            val intent = Intent(this, Page18Activity::class.java)
            startActivity(intent)
            finish()
        }

        val pfp = findViewById<CircleImageView>(R.id.prof)

        loadCurrentUserProfile(pfp)
        val serviceIntent = Intent(this, NotificationService::class.java)
        startService(serviceIntent)
        // --- NEW: Update FCM Token ---
        updateFcmToken()
        // -----------------------------

        pfp.setOnClickListener {
            val intent = Intent(this, Page13Activity::class.java)
            startActivity(intent)
            finish()
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 200)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data

            val intent = Intent(this, Page21Activity::class.java)
            intent.putExtra("selectedImage", imageUri.toString())
            startActivity(intent)
        }
    }

    private fun updateFcmToken() {
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

        if (currentUserId == -1) return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            uploadTokenToServer(currentUserId, token)
        }
    }

    private fun uploadTokenToServer(userId: Int, token: String) {
        val urlString = Config.BASE_URL + "update_fcm_token.php"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("user_id", userId)
                jsonParam.put("fcm_token", token)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                // Check response (Optional, fire and forget)
                val code = conn.responseCode
                Log.d("FCM", "Token Update Code: $code")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadCurrentUserProfile(targetImageView: ImageView) {
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1
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
                os.close()
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    val user = json.getJSONObject("user")
                    val pfpString = if (user.isNull("profile_picture")) "" else user.optString("profile_picture")
                    if (pfpString.isNotEmpty()) {
                        try {
                            val cleanBase64 = if (pfpString.contains(",")) pfpString.split(",")[1] else pfpString
                            val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            withContext(Dispatchers.Main) { if (bitmap != null) targetImageView.setImageBitmap(bitmap) }
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {}
        }
    }
}