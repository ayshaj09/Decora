package com.example.decora

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

class Page10Activity : AppCompatActivity() {

    private val sections = ArrayList<SectionModel>()
    private lateinit var sectionsAdapter: SectionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page10)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ---------------- FOOTER NAV ----------------
        val home = findViewById<ImageView>(R.id.home)
        home.setOnClickListener {
            startActivity(Intent(this, Page7Activity::class.java))
            finish()
        }

        val msg = findViewById<ImageView>(R.id.dm)
        msg.setOnClickListener {
            startActivity(Intent(this, Page18Activity::class.java))
            finish()
        }

        val pfp = findViewById<CircleImageView>(R.id.prof)
        loadCurrentUserProfile(pfp)
        pfp.setOnClickListener {
            startActivity(Intent(this, Page13Activity::class.java))
            finish()
        }

        val search = findViewById<RelativeLayout>(R.id.srch)
        search.setOnClickListener {
            startActivity(Intent(this, Page11Activity::class.java))
        }

        // ---------------- MAIN RECYCLER ----------------
        val recyclerView = findViewById<RecyclerView>(R.id.pinsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        sectionsAdapter = SectionsAdapter(sections)
        recyclerView.adapter = sectionsAdapter

        // Fetch dynamic sections (titles + images)
        fetchTitles()

    }
    private fun fetchTitles() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.URL_GET_PIN_TITLES)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    if (json.optBoolean("success")) {
                        val titlesArray = json.getJSONArray("titles")

                        sections.clear()
                        for (i in 0 until titlesArray.length()) {
                            val title = titlesArray.getString(i)
                            sections.add(SectionModel(title))
                        }

                        withContext(Dispatchers.Main) {
                            sectionsAdapter.notifyDataSetChanged()
                        }


                        for (section in sections) {
                            fetchImagesForTitle(section)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun fetchImagesForTitle(section: SectionModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ✅ SAME API & FORMAT AS ResultPageActivity
                val urlStr = "${Config.URL_SEARCH_PINS}?query=${section.title}"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    if (json.optBoolean("success")) {
                        val pinsArray = json.getJSONArray("pins")

                        section.images.clear()

                        for (i in 0 until pinsArray.length()) {
                            val obj = pinsArray.getJSONObject(i)
                            val image = obj.optString("image")

                            if (image.isNotEmpty()) {
                                section.images.add(image)
                            }
                        }

                        // ✅ Refresh UI for this section
                        withContext(Dispatchers.Main) {
                            sectionsAdapter.notifyDataSetChanged()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ---------------- PROFILE IMAGE ----------------
    private fun loadCurrentUserProfile(targetImageView: ImageView) {
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1
        if (currentUserId == -1) return

        val urlString = Config.BASE_URL + "get_user_profile.php"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conn = URL(urlString).openConnection() as HttpURLConnection
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
                    val pfpString = if (user.isNull("profile_picture")) "" else user.optString("profile_picture")

                    if (pfpString.isNotEmpty()) {
                        val clean = if (pfpString.contains(",")) pfpString.split(",")[1] else pfpString
                        val bytes = Base64.decode(clean, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                        withContext(Dispatchers.Main) {
                            targetImageView.setImageBitmap(bitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
