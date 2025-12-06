package com.example.decora

import android.content.Intent
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OtherUserProfile : AppCompatActivity() {

    private var targetUserId: String = ""
    private var myUserId: String = ""
    private var isFollowing = false
    private lateinit var rvBoards: RecyclerView

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
        myUserId = prefs.getString("user_id", "") ?: ""

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
        val followBtn = findViewById<TextView>(R.id.edit)

        nameView.text = incomingName
        handleView.text = "@${incomingName.lowercase().replace(" ", "")}"

        if (incomingPfp.isNotEmpty()) {
            val bitmap = decodeBase64(incomingPfp)
            if (bitmap != null) pfpView.setImageBitmap(bitmap)
        }

        // 3. Setup RecyclerView for Boards
        rvBoards = findViewById(R.id.rvBoards)
        rvBoards.layoutManager = GridLayoutManager(this, 2)

        // 4. Load Data
        loadProfileStats() // Stats + Click Listeners
        loadBoards()       // Boards for this user

        // 5. Handle Follow Button
        followBtn.setOnClickListener {
            toggleFollow()
        }

        // Navigation
        findViewById<ImageView>(R.id.home).setOnClickListener {
            startActivity(Intent(this, Page7Activity::class.java))
            finish()
        }

        // Back Button (Added in XML)
        val backBtn = findViewById<ImageView>(R.id.backBtn)
        if(backBtn != null) {
            backBtn.setOnClickListener { finish() }
        }
    }

    private fun loadProfileStats() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlStr = "${Config.URL_GET_PROFILE_STATS}?target_id=$targetUserId&my_id=$myUserId"
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
                            val statsContainer = findViewById<LinearLayout>(R.id.fllow)

                            if (statsContainer != null && statsContainer.childCount >= 2) {
                                val followersTv = statsContainer.getChildAt(0) as TextView
                                val followingTv = statsContainer.getChildAt(1) as TextView

                                followersTv.text = "$followers followers"
                                followingTv.text = "$following following"

                                // ✅ ADD CLICK LISTENERS HERE
                                followersTv.setOnClickListener {
                                    val intent = Intent(this@OtherUserProfile, FollowListActivity::class.java)
                                    // VERY IMPORTANT: Pass the targetUserId (Search User), NOT myUserId
                                    val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
                                    // Save target ID temporarily or just pass it in Intent?
                                    // FollowListActivity normally grabs "user_id" from SharedPrefs.
                                    // We need to modify FollowListActivity slightly OR cheat by passing intent extras.
                                    // Let's rely on modifying FollowListActivity to check Intent Extras first.

                                    // Update: My previous code for FollowListActivity looked at SharedPrefs.
                                    // You should update FollowListActivity to prefer Intent Extra "USER_ID" if present.
                                    intent.putExtra("USER_ID_OVERRIDE", targetUserId)
                                    intent.putExtra("TYPE", "followers")
                                    startActivity(intent)
                                }

                                followingTv.setOnClickListener {
                                    val intent = Intent(this@OtherUserProfile, FollowListActivity::class.java)
                                    intent.putExtra("USER_ID_OVERRIDE", targetUserId)
                                    intent.putExtra("TYPE", "following")
                                    startActivity(intent)
                                }
                            }
                            updateFollowButtonUI()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ✅ NEW: Load Boards for the TARGET user
    private fun loadBoards() {
        val urlString = Config.BASE_URL + "get_board.php"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val jsonParam = JSONObject()
                // Use targetUserId to get THEIR boards
                jsonParam.put("user_id", targetUserId)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)

                if (json.optBoolean("success")) {
                    val jsonArray = json.getJSONArray("boards")
                    val boardsList = ArrayList<Board>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val previewList = ArrayList<String>()
                        val imagesArray = obj.optJSONArray("preview_images")
                        if (imagesArray != null) {
                            for (j in 0 until imagesArray.length()) {
                                previewList.add(imagesArray.getString(j))
                            }
                        }
                        val count = obj.optInt("pin_count", 0)
                        boardsList.add(Board(
                            obj.getInt("id"),
                            obj.getString("title"),
                            "$count pins",
                            previewList
                        ))
                    }

                    withContext(Dispatchers.Main) {
                        // Reuse BoardAdapter
                        rvBoards.adapter = BoardAdapter(this@OtherUserProfile, boardsList)
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
                        val status = json.optString("status")
                        isFollowing = (status == "followed")

                        withContext(Dispatchers.Main) {
                            updateFollowButtonUI()
                            loadProfileStats() // Refresh numbers
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
            followBtn.background.setTint(android.graphics.Color.parseColor("#444444"))
        } else {
            followBtn.text = "Follow"
            followBtn.background.setTint(android.graphics.Color.parseColor("#737576"))
        }
    }

    private fun decodeBase64(input: String): Bitmap? {
        return try {
            val decodedByte = Base64.decode(input, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
        } catch (e: Exception) { null }
    }
}