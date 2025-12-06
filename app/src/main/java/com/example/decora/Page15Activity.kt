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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page15Activity : AppCompatActivity() {
    private lateinit var rvBoards: RecyclerView
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page15)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupLogout()
        // --- Navigation Logic ---
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

        val home = findViewById<ImageView>(R.id.home)
        home.setOnClickListener {
            val intent = Intent(this, Page7Activity::class.java)
            startActivity(intent)
            finish()
        }

        val created = findViewById<TextView>(R.id.created)
        created.setOnClickListener {
            val intent = Intent(this, Page16Activity::class.java)
            startActivity(intent)
            finish()
        }

        val edited = findViewById<TextView>(R.id.edit)
        edited.setOnClickListener {
            val intent = Intent(this, Page17Activity::class.java)
            startActivity(intent)
        }

        // --- NEW: Followers/Following Click Listeners ---
        val statsContainer = findViewById<LinearLayout>(R.id.fllow)

        // We use loops or index because they don't have IDs in XML
        if (statsContainer != null && statsContainer.childCount >= 2) {

            // 1. Followers Click (First text)
            statsContainer.getChildAt(0).setOnClickListener {
                val intent = Intent(this, FollowListActivity::class.java)
                intent.putExtra("TYPE", "followers")
                startActivity(intent)
            }

            // 2. Following Click (Second text)
            statsContainer.getChildAt(1).setOnClickListener {
                val intent = Intent(this, FollowListActivity::class.java)
                intent.putExtra("TYPE", "following")
                startActivity(intent)
            }
        }

        // --- Profile Loading ---
        val pfp = findViewById<CircleImageView>(R.id.pfp)
        val pfp2 = findViewById<CircleImageView>(R.id.prof)

        // --- Board Loading ---
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

        // Load data
        loadCurrentUserProfile(pfp)
        loadCurrentUserProfile(pfp2) // Load footer image
        loadFollowStats() // Load stats numbers

        rvBoards = findViewById(R.id.rvBoards)
        rvBoards.layoutManager = GridLayoutManager(this, 2)
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
    override fun onResume() {
        super.onResume()
        loadBoards()
        loadFollowStats() // Refresh counts when returning to page
        val pfp = findViewById<CircleImageView>(R.id.pfp)
        loadCurrentUserProfile(pfp)
    }

    private fun loadBoards() {
        val urlString = Config.BASE_URL + "get_board.php"
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
                        rvBoards.adapter = BoardAdapter(this@Page15Activity, boardsList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadCurrentUserProfile(targetImageView: ImageView) {
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

                    val fullName = user.optString("full_name")
                    val pfpString = if (user.isNull("profile_picture")) "" else user.optString("profile_picture")

                    withContext(Dispatchers.Main) {
                        val nameTv = findViewById<TextView>(R.id.username)
                        if (nameTv != null) nameTv.text = fullName

                        val handleContainer = findViewById<LinearLayout>(R.id.name)
                        if (handleContainer != null) {
                            val handleTv = handleContainer.getChildAt(1) as TextView
                            handleTv.text = "@${fullName.replace(" ", "").lowercase()}"
                        }

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