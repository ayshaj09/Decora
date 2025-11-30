package com.example.decora

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.decora.com.example.decora.Config
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OtherUserProfile : AppCompatActivity() {

    private var targetUserId: String = ""
    private var myUserId: String = ""
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_other_user_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Get IDs
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        myUserId = prefs.getString("user_id", "") ?: "" // Make sure you saved this during Login!

        targetUserId = intent.getStringExtra("EXTRA_ID") ?: ""
        val incomingName = intent.getStringExtra("EXTRA_NAME") ?: "User"
        val incomingPfp = intent.getStringExtra("EXTRA_PFP") ?: ""

        if (targetUserId.isEmpty()) {
            Toast.makeText(this, "Error: User ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Setup UI
        val nameView = findViewById<TextView>(R.id.username)
        val handleView = findViewById<TextView>(R.id.userHandle)
        val pfpView = findViewById<CircleImageView>(R.id.pfp)
        val followBtn = findViewById<TextView>(R.id.edit) // "Follow" button

        nameView.text = incomingName
        handleView.text = "@${incomingName.lowercase().replace(" ", "")}"

        if (incomingPfp.isNotEmpty()) {
            val bitmap = decodeBase64(incomingPfp)
            if (bitmap != null) pfpView.setImageBitmap(bitmap)
        }

        // 3. Load Stats (Followers count & status)
        loadProfileStats()

        // 4. Handle Follow/Unfollow Click
        followBtn.setOnClickListener {
            toggleFollow()
        }

        // Navigation
        findViewById<ImageView>(R.id.home).setOnClickListener { finish() }
    }

    private fun loadProfileStats() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Config.URL_GET_PROFILE_STATS = "http.../get_profile_stats.php"
                val urlStr = "${Config.BASE_URL}get_profile_stats.php?target_id=$targetUserId&my_id=$myUserId"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    if (json.optBoolean("success")) {
                        val followers = json.optString("followers")
                        val following = json.optString("following")
                        isFollowing = json.optBoolean("is_following")

                        withContext(Dispatchers.Main) {
                            // Update Counts
                            val followersText = findViewById<LinearLayout>(R.id.fllow).getChildAt(0) as TextView
                            val followingText = findViewById<LinearLayout>(R.id.fllow).getChildAt(1) as TextView

                            followersText.text = "$followers followers"
                            followingText.text = "$following following"

                            // Update Button Text
                            updateFollowButtonUI()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun toggleFollow() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Config.URL_FOLLOW_TOGGLE = "http.../follow_toggle.php"
                val url = URL("${Config.BASE_URL}follow_toggle.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("follower_id", myUserId)
                jsonParam.put("following_id", targetUserId)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonParam.toString())
                writer.flush()

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    if (json.optBoolean("success")) {
                        val status = json.optString("status") // "followed" or "unfollowed"
                        isFollowing = (status == "followed")

                        withContext(Dispatchers.Main) {
                            updateFollowButtonUI()
                            // Reload stats to update the follower count number immediately
                            loadProfileStats()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateFollowButtonUI() {
        val followBtn = findViewById<TextView>(R.id.edit)
        if (isFollowing) {
            followBtn.text = "Following"
            followBtn.background.setTint(android.graphics.Color.parseColor("#444444")) // Darker when following
        } else {
            followBtn.text = "Follow"
            followBtn.background.setTint(android.graphics.Color.parseColor("#737576")) // Original color
        }
    }

    private fun decodeBase64(input: String): Bitmap? {
        return try {
            val decodedByte = Base64.decode(input, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
        } catch (e: Exception) { null }
    }
}