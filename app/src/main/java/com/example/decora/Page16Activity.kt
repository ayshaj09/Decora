package com.example.decora

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page16Activity : AppCompatActivity() {
    private lateinit var pinsRecycler: RecyclerView
    private val pinsList = ArrayList<Pin>()
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page16)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupLogout()
        // --- Navigation ---
        val srch = findViewById<ImageView>(R.id.search)
        srch.setOnClickListener {
            val intent = Intent(this, Page10Activity::class.java)
            startActivity(intent)
            finish()
        }

        val msg = findViewById<ImageView>(R.id.comment)
        msg.setOnClickListener {
            val intent = Intent(this, Page18Activity::class.java)
            startActivity(intent)
            finish()
        }

        val home = findViewById<ImageView>(R.id.home)
        home.setOnClickListener {
            val intent = Intent(this, Page7Activity::class.java)
            startActivity(intent)
            finish()
        }

        val saved = findViewById<TextView>(R.id.svd)
        saved.setOnClickListener {
            val intent = Intent(this, Page15Activity::class.java)
            startActivity(intent)
            finish()
        }

        val edited = findViewById<TextView>(R.id.edit)
        edited.setOnClickListener {
            val intent = Intent(this, Page17Activity::class.java)
            startActivity(intent)
        }

        // --- Profile Loading ---
        val pfp = findViewById<CircleImageView>(R.id.prof)
        loadCurrentUserProfile(pfp)

        val pfp2 = findViewById<CircleImageView>(R.id.pfp)
        loadCurrentUserProfile(pfp2)

        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1
        loadFollowStats()
        // --- Pins Loading ---
        pinsRecycler = findViewById(R.id.pinsRecycler)
        pinsRecycler.layoutManager = GridLayoutManager(this, 2)

        fetchPins()
    }
    private fun setupLogout() {
        val logoutBtn = findViewById<ImageView>(R.id.logout) // The options icon

        logoutBtn.setOnClickListener {
            // Show Confirmation Popup
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout") { _, _ ->
                    performLogout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    override fun onResume() {
        super.onResume()
        loadFollowStats() // Refresh counts when returning to page
        val pfp = findViewById<CircleImageView>(R.id.pfp)
        loadCurrentUserProfile(pfp)
    }
    private fun performLogout() {
        // 1. Clear SharedPreferences (Remove user data)
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.clear() // Deletes user_id, isLoggedIn, etc.
        editor.apply()

        // 2. Stop Notification Service (Optional but good practice)
        stopService(Intent(this, NotificationService::class.java))

        // 3. Navigate to Login Page (Page 2)
        val intent = Intent(this, Page2Activity::class.java)
        // Clear back stack so user can't press "Back" to return to profile
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    private fun fetchPins() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.BASE_URL + "get_pins.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                val currentUserId = sharedPrefs.getString("user_id", "-1")

                val requestJson = JSONObject()
                requestJson.put("user_id", currentUserId)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(requestJson.toString())
                writer.flush()
                writer.close()

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                if (json.getBoolean("success")) {
                    val pinsArray: JSONArray = json.getJSONArray("pins")
                    pinsList.clear()

                    for (i in 0 until pinsArray.length()) {
                        val obj = pinsArray.getJSONObject(i)

                        pinsList.add(
                            Pin(
                                id = obj.getInt("id"),
                                title = obj.getString("title"),
                                image = obj.getString("image"),
                                username = obj.optString("username", "Unknown"),
                                userPfp = obj.optString("user_pfp", "")
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        pinsRecycler.adapter = PinAdapter(pinsList)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ✅ UPDATED FUNCTION: Loads Name AND Image
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
                reader.close()

                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    val user = json.getJSONObject("user")

                    // 1. Get the Name
                    val fullName = user.optString("full_name")
                    val pfpString = if (user.isNull("profile_picture")) "" else user.optString("profile_picture")

                    withContext(Dispatchers.Main) {
                        // 2. Update the Name TextView
                        val nameTv = findViewById<TextView>(R.id.username)
                        if (nameTv != null) {
                            nameTv.text = fullName
                        }

                        // 3. Update the "Handle" (the small text below name)
                        // XML Structure: LinearLayout (id=name) -> [ImageView, TextView]
                        val handleContainer = findViewById<LinearLayout>(R.id.name)
                        if (handleContainer != null) {
                            // The text view is at index 1
                            val handleTv = handleContainer.getChildAt(1) as TextView
                            handleTv.text = "@${fullName.replace(" ", "").lowercase()}"
                        }

                        // 4. Update Image
                        if (pfpString.isNotEmpty()) {
                            try {
                                val cleanBase64 = if (pfpString.contains(",")) pfpString.split(",")[1] else pfpString
                                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                                if (bitmap != null) targetImageView.setImageBitmap(bitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun loadFollowStats() {
        if (currentUserId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Config.URL_GET_PROFILE_STATS = .../get_profile_stats.php
                val urlStr = "${Config.URL_GET_PROFILE_STATS}?target_id=$currentUserId&my_id=$currentUserId"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    if (json.optBoolean("success")) {
                        val followers = json.optString("followers")
                        val following = json.optString("following")

                        withContext(Dispatchers.Main) {
                            val statsContainer = findViewById<LinearLayout>(R.id.fllow)

                            if (statsContainer != null && statsContainer.childCount >= 2) {
                                val followersTv = statsContainer.getChildAt(0) as TextView
                                val followingTv = statsContainer.getChildAt(1) as TextView

                                followersTv.text = "$followers followers"
                                followingTv.text = "$following following"
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}