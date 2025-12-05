package com.example.decora

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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

        // --- Profile Loading ---
        // 1. Load Main Profile Picture (Big one)
        val pfp = findViewById<CircleImageView>(R.id.pfp)
        loadCurrentUserProfile(pfp)

        // 2. Load Footer Profile Picture (Small one)
        val pfp2 = findViewById<CircleImageView>(R.id.prof)
        loadCurrentUserProfile(pfp2)

        // --- Board Loading ---
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

        rvBoards = findViewById(R.id.rvBoards)
        rvBoards.layoutManager = GridLayoutManager(this, 2)
    }

    override fun onResume() {
        super.onResume()
        loadBoards()
        // Reload profile in case user came back from "Edit Profile" page
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

                        // Parse Preview Images Array
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
}