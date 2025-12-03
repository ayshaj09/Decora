package com.example.decora

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page8Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page8)

        // REAL logged-in user ID
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
        val scrollView = findViewById<ScrollView>(R.id.mainScroll)

        val pinId = intent.getIntExtra("pinId", -1)
        if (pinId == -1) return
        commentIcon.setOnClickListener {
            commentSection.visibility = View.VISIBLE
            scrollView.post {
                scrollView.smoothScrollTo(0, commentSection.top)
            }
        }

        // Load LIKE STATUS when opening page
        loadLikes(pinId, userId, heartIcon, likeCount)
        loadComments(pinId, commentsContainer)

        // Fetch pin details
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

                    if (imageBase64.isNotEmpty()) {
                        val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        mainImage.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    }

                    if (pfpBase64.isNotEmpty()) {
                        val clean = if (pfpBase64.contains(",")) {
                            pfpBase64.split(",")[1]
                        } else pfpBase64

                        val bytes = Base64.decode(clean, Base64.DEFAULT)
                        profilePic.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
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

                            // reload comments
                            loadComments(pinId, commentsContainer)
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // ❤️ LIKE / UNLIKE BUTTON
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
                            // Refresh like icon + count
                            loadLikes(pinId, userId, heartIcon, likeCount)
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
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

                if (!obj.getBoolean("success")) return@launch

                val commentsArray = obj.getJSONArray("comments")

                withContext(Dispatchers.Main) {
                    commentsContainer.removeAllViews()

                    for (i in 0 until commentsArray.length()) {
                        val c = commentsArray.getJSONObject(i)

                        val username = c.getString("username")
                        val commentText = c.getString("comment_text")

                        val tv = TextView(this@Page8Activity)
                        tv.text = "$username: $commentText"
                        tv.setTextColor(resources.getColor(R.color.white))
                        tv.textSize = 14f
                        tv.setPadding(5, 8, 5, 8)

                        commentsContainer.addView(tv)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Load like count + liked status
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

                        if (liked)
                            heartIcon.setImageResource(R.drawable.heart_filled)
                        else
                            heartIcon.setImageResource(R.drawable.img_2)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
