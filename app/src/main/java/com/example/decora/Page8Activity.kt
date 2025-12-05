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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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

    // Store data to pass to Share Screen
    private var currentPinImage: String = ""
    private var currentPinDesc: String = "" // NEW: Store Description

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page8)

        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

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
        val scrollView = findViewById<NestedScrollView>(R.id.mainScroll)
        val back = findViewById<ImageView>(R.id.backBtn)
        val saveBtn = findViewById<TextView>(R.id.saveButton)
        val sendBtn = findViewById<ImageView>(R.id.sendTo)

        morePinsRecycler = findViewById(R.id.morePinsRecycler)
        morePinsRecycler.layoutManager = GridLayoutManager(this, 2)
        morePinsRecycler.isNestedScrollingEnabled = false

        val pinId = intent.getIntExtra("pinId", -1)
        if (pinId == -1) {
            finish()
            return
        }
// ---------------- OFFLINE MODE ACTIVATION ----------------
        if (!NetworkUtils.isNetworkAvailable(this)) {

            // Load main pin from local database
            loadPinOffline(pinId)

            // Load suggestions from local database
            loadMorePinsOffline(pinId)

            Toast.makeText(this, "Loaded offline data", Toast.LENGTH_SHORT).show()

            return  // STOP here so it does NOT attempt online fetch
        }
// -----------------------------------------------------------

        // --- NEW: Grab Description passed from previous page ---
        currentPinDesc = intent.getStringExtra("pinDesc") ?: ""

        back.setOnClickListener { finish() }

        saveBtn?.setOnClickListener {
            val intent = Intent(this, Page9Activity::class.java)
            intent.putExtra("pin_id_to_save", pinId)
            startActivity(intent)
        }

        // --- UPDATED: Pass Description to Share Page ---
        sendBtn?.setOnClickListener {
            val intent = Intent(this, SharePinActivity::class.java)
            intent.putExtra("pinId", pinId)
            intent.putExtra("pinTitle", titleText.text.toString())
            intent.putExtra("pinImage", currentPinImage)
            intent.putExtra("pinDesc", currentPinDesc) // Pass Caption
            startActivity(intent)
        }

        commentIcon.setOnClickListener {
            commentSection.visibility = View.VISIBLE
            scrollView.post {
                scrollView.smoothScrollTo(0, commentSection.top)
            }
        }

        loadLikes(pinId, userId, heartIcon, likeCount)
        loadComments(pinId, commentsContainer)
        fetchMorePins(pinId)

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

                // Save for sharing
                currentPinImage = imageBase64
                // Update description if server has newer version
                currentPinDesc = pin.optString("description", currentPinDesc)

                withContext(Dispatchers.Main) {
                    titleText.text = title
                    usernameText.text = username

                    if (imageBase64.isNotEmpty()) {
                        try {
                            val cleanBase64 = if (imageBase64.contains(",")) {
                                imageBase64.split(",")[1]
                            } else {
                                imageBase64
                            }
                            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            mainImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            mainImage.setImageResource(R.drawable.rectangle11)
                        }
                    } else {
                        mainImage.setImageResource(R.drawable.rectangle11)
                    }

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

        postCommentBtn.setOnClickListener {
            val comment = commentInput.text.toString().trim()
            if (comment.isEmpty()) return@setOnClickListener

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(Config.BASE_URL + "add_comment.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")

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

        heartIcon.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = URL(Config.BASE_URL + "like_pin.php")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")

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
                        morePinsRecycler.adapter = PinAdapter(limitedPins)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadComments(pinId: Int, commentsContainer: LinearLayout) {
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
    private fun loadPinOffline(pinId: Int) {
        val db = DatabaseHelper(this)
        val pins = db.getPins()

        val pin = pins.find { it.id == pinId }
        if (pin == null) {
            Toast.makeText(this, "Pin not available offline", Toast.LENGTH_SHORT).show()
            return
        }

        // UI elements
        val mainImage = findViewById<ImageView>(R.id.firstimage)
        val titleText = findViewById<TextView>(R.id.titleText)
        val usernameText = findViewById<TextView>(R.id.username)
        val profilePic = findViewById<CircleImageView>(R.id.profilePic)

        titleText.text = pin.title
        usernameText.text = pin.username

        // Decode main image
        try {
            val clean = if (pin.image.contains(",")) pin.image.split(",")[1] else pin.image
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            mainImage.setImageBitmap(bitmap)
        } catch (e: Exception) {
            mainImage.setImageResource(R.drawable.rectangle11)
        }

        // Decode profile picture
        if (pin.userPfp.isNotEmpty()) {
            try {
                val clean = if (pin.userPfp.contains(",")) pin.userPfp.split(",")[1] else pin.userPfp
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profilePic.setImageBitmap(bitmap)
            } catch (e: Exception) {
                profilePic.setImageResource(R.drawable.defaultpfp)
            }
        }
    }
    private fun loadMorePinsOffline(currentPinId: Int) {
        val db = DatabaseHelper(this)
        val pins = db.getPins().filter { it.id != currentPinId }

        if (pins.isEmpty()) return

        val shuffled = pins.shuffled()
        val limited = if (shuffled.size > 4) shuffled.subList(0, 4) else shuffled

        morePinsRecycler.adapter = PinAdapter(limited.toMutableList())
    }

    private fun loadLikes(pinId: Int, userId: Int, heartIcon: ImageView, likeCount: TextView) {
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