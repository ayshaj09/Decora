package com.example.decora

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView // Changed import
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager // Changed import
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

class Page8Activity : AppCompatActivity() {

    private lateinit var morePinsRecycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page8)

        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

        // UI Elements
        val heartIcon = findViewById<ImageView>(R.id.heartIcon)
        val likeCount = findViewById<TextView>(R.id.likeCount)
        val mainImage = findViewById<ImageView>(R.id.firstimage)
        val titleText = findViewById<TextView>(R.id.titleText)
        val usernameText = findViewById<TextView>(R.id.username)
        val profilePic = findViewById<CircleImageView>(R.id.profilePic)
        val commentsContainer = findViewById<LinearLayout>(R.id.commentsContainer)
        val commentInput = findViewById<TextView>(R.id.commentInput)
        val postCommentBtn = findViewById<TextView>(R.id.postCommentBtn)
        val commentIcon = findViewById<ImageView>(R.id.commentIcon)
        val commentSection = findViewById<LinearLayout>(R.id.commentSection)

        // Ensure XML uses NestedScrollView for id "mainScroll"
        val scrollView = findViewById<NestedScrollView>(R.id.mainScroll)

        val back = findViewById<ImageView>(R.id.backBtn)
        val saveBtn = findViewById<TextView>(R.id.saveButton)
        val deleteBtn = findViewById<TextView>(R.id.deleteButton)
        val openedFromPins = intent.getBooleanExtra("opened_from_pins_page", false)

        deleteBtn.visibility = if (openedFromPins) View.VISIBLE else View.GONE

        // --- LAYOUT FIX: Use GridLayoutManager ---
        morePinsRecycler = findViewById(R.id.morePinsRecycler)
        morePinsRecycler.layoutManager = GridLayoutManager(this, 2)
        morePinsRecycler.isNestedScrollingEnabled = false // Let main scrollview handle scrolling
        // -----------------------------------------

        val pinId = intent.getIntExtra("pinId", -1)
        if (pinId == -1) {
            finish()
            return
        }

        back.setOnClickListener { finish() }

        saveBtn?.setOnClickListener {
            val intent = Intent(this, Page9Activity::class.java)
            intent.putExtra("pin_id_to_save", pinId)
            startActivity(intent)
        }
        deleteBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(Config.BASE_URL + "delete_pin.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")

                    val json = JSONObject()
                    json.put("pin_id", pinId)

                    val writer = OutputStreamWriter(conn.outputStream)
                    writer.write(json.toString())
                    writer.flush()
                    writer.close()

                    val response = conn.inputStream.bufferedReader().readText()
                    val obj = JSONObject(response)

                    if (obj.getBoolean("success")) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@Page8Activity, "Pin deleted", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish() // Close Page8 and return to PinsPage
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        commentIcon.setOnClickListener {
            commentSection.visibility = View.VISIBLE
            scrollView.post {
                scrollView.smoothScrollTo(0, commentSection.top)
            }
        }

        loadLikes(pinId, userId, heartIcon, likeCount)
        loadComments(pinId, commentsContainer)

        // Fetch Suggestions
        fetchMorePins(pinId)

        // Fetch Main Pin Details
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.BASE_URL + "get_single_pin.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

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

                withContext(Dispatchers.Main) {
                    titleText.text = title
                    usernameText.text = username

                    // Main Image Base64
                    if (imageBase64.isNotEmpty()) {
                        try {
                            val cleanBase64 = if (imageBase64.contains(",")) imageBase64.split(",")[1] else imageBase64
                            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            mainImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            mainImage.setImageResource(R.drawable.rectangle11)
                        }
                    } else {
                        mainImage.setImageResource(R.drawable.rectangle11)
                    }

                    // Profile Pic Base64
                    if (pfpBase64.isNotEmpty()) {
                        try {
                            val cleanPfp = if (pfpBase64.contains(",")) pfpBase64.split(",")[1] else pfpBase64
                            val bytes = Base64.decode(cleanPfp, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            if (bitmap != null) profilePic.setImageBitmap(bitmap)
                            else profilePic.setImageResource(R.drawable.defaultpfp)
                        } catch (e: Exception) {
                            profilePic.setImageResource(R.drawable.defaultpfp)
                        }
                    } else {
                        profilePic.setImageResource(R.drawable.defaultpfp)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        // Comment & Like Listeners...
        postCommentBtn.setOnClickListener {
            val comment = commentInput.text.toString().trim()
            if (comment.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val url = URL(Config.BASE_URL + "add_comment.php")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.doOutput = true
                        val json = JSONObject()
                        json.put("pin_id", pinId)
                        json.put("user_id", userId)
                        json.put("comment", comment)
                        val writer = OutputStreamWriter(conn.outputStream)
                        writer.write(json.toString())
                        writer.flush()
                        writer.close()
                        val response = conn.inputStream.bufferedReader().readText()
                        val obj = JSONObject(response)
                        if (obj.getBoolean("success")) {
                            withContext(Dispatchers.Main) {
                                commentInput.text = ""
                                loadComments(pinId, commentsContainer)
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }

        heartIcon.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(Config.BASE_URL + "like_pin.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    val json = JSONObject()
                    json.put("pin_id", pinId)
                    json.put("user_id", userId)
                    val writer = OutputStreamWriter(conn.outputStream)
                    writer.write(json.toString())
                    writer.flush()
                    writer.close()
                    val response = conn.inputStream.bufferedReader().readText()
                    val obj = JSONObject(response)
                    if (obj.getBoolean("success")) {
                        withContext(Dispatchers.Main) {
                            loadLikes(pinId, userId, heartIcon, likeCount)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    private fun fetchMorePins(currentPinId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.BASE_URL + "get_all_pins.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write("{}")
                writer.flush()
                writer.close()

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                if (json.getBoolean("success")) {
                    val pinsArray = json.getJSONArray("pins")
                    val allPins = ArrayList<Pin>()

                    for (i in 0 until pinsArray.length()) {
                        val obj = pinsArray.getJSONObject(i)
                        val id = obj.getInt("id")
                        if (id == currentPinId) continue

                        allPins.add(
                            Pin(
                                id = id,
                                title = obj.getString("title"),
                                image = obj.getString("image"),
                                username = obj.optString("username", "User"),
                                userPfp = obj.optString("user_pfp", "")
                            )
                        )
                    }

                    Collections.shuffle(allPins)
                    val limitedPins = if (allPins.size > 4) allPins.subList(0, 4) else allPins

                    withContext(Dispatchers.Main) {
                        // Toast to confirm data arrived
                        // Toast.makeText(this@Page8Activity, "Loaded ${limitedPins.size} suggestions", Toast.LENGTH_SHORT).show()
                        morePinsRecycler.adapter = PinAdapter(limitedPins)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Existing loadComments and loadLikes functions remain unchanged...
    private fun loadComments(pinId: Int, commentsContainer: LinearLayout) {
        // ... (Keep existing code)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.BASE_URL + "get_comments.php?pin_id=$pinId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                val obj = JSONObject(response)
                if (obj.getBoolean("success")) {
                    val commentsArray = obj.getJSONArray("comments")
                    withContext(Dispatchers.Main) {
                        commentsContainer.removeAllViews()
                        for (i in 0 until commentsArray.length()) {
                            val c = commentsArray.getJSONObject(i)
                            val username = c.getString("username")
                            val commentText = c.getString("comment_text")
                            val tv = TextView(this@Page8Activity)
                            tv.text = "$username: $commentText"
                            tv.setTextColor(resources.getColor(R.color.white, null))
                            tv.textSize = 14f
                            tv.setPadding(5, 8, 5, 8)
                            commentsContainer.addView(tv)
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadLikes(pinId: Int, userId: Int, heartIcon: ImageView, likeCount: TextView) {
        // ... (Keep existing code)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.BASE_URL + "get_likes.php?pin_id=$pinId&user_id=$userId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                val response = conn.inputStream.bufferedReader().readText()
                val obj = JSONObject(response)
                if (obj.getBoolean("success")) {
                    val likes = obj.getInt("likes")
                    val liked = obj.getBoolean("liked")
                    withContext(Dispatchers.Main) {
                        likeCount.text = likes.toString()
                        if (liked) heartIcon.setImageResource(R.drawable.heart_filled)
                        else heartIcon.setImageResource(R.drawable.img_2)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}