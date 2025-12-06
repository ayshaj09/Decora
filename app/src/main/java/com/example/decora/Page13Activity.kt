package com.example.decora

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
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

class Page13Activity : AppCompatActivity() {

    private lateinit var rvBoards: RecyclerView
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page13)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

        val pfpTop = findViewById<CircleImageView>(R.id.pfp)
        val pfpBottom = findViewById<CircleImageView>(R.id.prof)
        loadCurrentUserProfile(pfpTop)
        loadCurrentUserProfile(pfpBottom)

        rvBoards = findViewById(R.id.rvBoards)
        rvBoards.layoutManager = GridLayoutManager(this, 2)

        val btnAdd = findViewById<ImageView>(R.id.add)

        btnAdd.setOnClickListener {
            val intent = Intent(this, BoardCreate::class.java)
            startActivity(intent)
        }

        pfpTop.setOnClickListener {
            startActivity(Intent(this, Page15Activity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.home).setOnClickListener {
            startActivity(Intent(this, Page7Activity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.search).setOnClickListener {
            startActivity(Intent(this, Page10Activity::class.java))
            finish()
        }
        findViewById<ImageView>(R.id.comment).setOnClickListener {
            startActivity(Intent(this, Page18Activity::class.java))
            finish()
        }

        val pins = findViewById<TextView>(R.id.pins)
        pins.setOnClickListener {
            startActivity(Intent(this, PinsPage::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // --- OFFLINE CHECK ---
        if (!NetworkUtils.isNetworkAvailable(this)) {
            val db = DatabaseHelper(this)
            val cachedBoards = db.getBoards()
            if (cachedBoards.isNotEmpty()) {
                rvBoards.adapter = BoardAdapter(this@Page13Activity, cachedBoards)
                Toast.makeText(this, "Loaded offline boards", Toast.LENGTH_SHORT).show()
            }
        } else {
            loadBoards()
        }
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
                        rvBoards.adapter = BoardAdapter(this@Page13Activity, boardsList)
                        // SAVE TO OFFLINE DB
                        DatabaseHelper(this@Page13Activity).saveBoards(boardsList)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadCurrentUserProfile(targetImageView: ImageView) {
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
                            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            withContext(Dispatchers.Main) { if (bitmap != null) targetImageView.setImageBitmap(bitmap) }
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {}
        }
    }
}