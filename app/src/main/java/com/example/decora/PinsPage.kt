package com.example.decora

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class PinsPage : AppCompatActivity() {

    private lateinit var pinsRecycler: RecyclerView
    private val pinsList = ArrayList<Pin>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pins_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val pfp2 = findViewById<CircleImageView>(R.id.prof)
        loadCurrentUserProfile(pfp2)

        val pfp = findViewById<CircleImageView>(R.id.pfp)
        loadCurrentUserProfile(pfp)

        pfp.setOnClickListener {
            startActivity(Intent(this, Page15Activity::class.java))
        }

        findViewById<ImageView>(R.id.search).setOnClickListener {
            startActivity(Intent(this, Page10Activity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.comment).setOnClickListener {
            startActivity(Intent(this, Page18Activity::class.java))
            finish()
        }

        findViewById<ImageView>(R.id.home).setOnClickListener {
            startActivity(Intent(this, Page7Activity::class.java))
            finish()
        }
        val board=findViewById<TextView>(R.id.boards)
        board.setOnClickListener {
            startActivity(Intent(this, Page13Activity::class.java))
            finish()
        }

        pinsRecycler = findViewById(R.id.pinsRecycler)
        pinsRecycler.layoutManager = GridLayoutManager(this, 2)


        fetchPins()
    }

    private fun fetchPins() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.BASE_URL + "get_pins.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                // Send empty JSON so POST works
                val writer = OutputStreamWriter(conn.outputStream)
                val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                val currentUserId = sharedPrefs.getString("user_id", "-1")

                val jsonObject = JSONObject()
                jsonObject.put("user_id", currentUserId)

                writer.write(jsonObject.toString())

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
                    val pfpString =
                        if (user.isNull("profile_picture")) "" else user.optString("profile_picture")

                    if (pfpString.isNotEmpty()) {
                        try {
                            val cleanBase64 =
                                if (pfpString.contains(",")) pfpString.split(",")[1] else pfpString
                            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            val bitmap =
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                            withContext(Dispatchers.Main) {
                                targetImageView.setImageBitmap(bitmap)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}