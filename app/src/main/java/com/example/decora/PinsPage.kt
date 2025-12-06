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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
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
    private lateinit var adapter: PinAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pins_page)

        // 1️⃣ Initialize RecyclerView
        pinsRecycler = findViewById(R.id.pinsRecycler)
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        pinsRecycler.layoutManager = layoutManager
        pinsRecycler.itemAnimator = null

        // 2️⃣ OFFLINE CHECK
        val db = DatabaseHelper(this)
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        // ✅ CHANGED: Now looking for "full_name" key
        val loggedInFullName = sharedPrefs.getString("full_name", "") ?: ""

        if (!NetworkUtils.isNetworkAvailable(this)) {
            // Get ALL pins from DB
            val allCachedPins = db.getPins()

            // ✅ FILTER: Compare Pin username with Logged In Full Name
            val myOfflinePins = allCachedPins.filter {
                it.username.trim().equals(loggedInFullName.trim(), ignoreCase = true)
            }

            if (myOfflinePins.isNotEmpty()) {
                pinsList.clear()
                pinsList.addAll(myOfflinePins)

                adapter = PinAdapter(pinsList.toMutableList())
                pinsRecycler.adapter = adapter

                // Set click listener for offline items
                adapter.setOnItemClickListener { pin ->
                    val intent = Intent(this, Page8Activity::class.java)
                    intent.putExtra("pinId", pin.id)
                    intent.putExtra("opened_from_pins_page", true)
                    startActivityForResult(intent, 100)
                }

                Toast.makeText(this, "Offline: Loaded ${myOfflinePins.size} pins", Toast.LENGTH_SHORT).show()
            } else {
                // Helpful Debug Message
                if (loggedInFullName.isEmpty()) {
                    Toast.makeText(this, "Error: full_name not saved in Login", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "No offline pins found for: $loggedInFullName", Toast.LENGTH_LONG).show()
                }
            }

            setupUI()
            return
        }

        // 3️⃣ ONLINE INITIALIZATION
        adapter = PinAdapter(pinsList.toMutableList())
        pinsRecycler.adapter = adapter

        setupUI()
        fetchPins(loggedInFullName) // Pass the name to fetch function
    }

    private fun setupUI() {
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

        findViewById<TextView>(R.id.boards).setOnClickListener {
            startActivity(Intent(this, Page13Activity::class.java))
            finish()
        }

        if (::adapter.isInitialized) {
            adapter.setOnItemClickListener { pin ->
                val intent = Intent(this, Page8Activity::class.java)
                intent.putExtra("pinId", pin.id)
                intent.putExtra("opened_from_pins_page", true)
                startActivityForResult(intent, 100)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val name = sharedPrefs.getString("full_name", "") ?: ""
            fetchPins(name)
        }
    }

    private fun fetchPins(loggedInFullName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(Config.BASE_URL + "get_pins.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                val currentUserId = sharedPrefs.getString("user_id", "-1")

                val requestJson = JSONObject()
                requestJson.put("user_id", currentUserId)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(requestJson.toString())
                writer.flush()
                writer.close()

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                if (json.getBoolean("success")) {
                    val pinsArray: JSONArray = json.getJSONArray("pins")
                    pinsList.clear()

                    for (i in 0 until pinsArray.length()) {
                        val obj = pinsArray.getJSONObject(i)

                        // ✅ FIX: Ensure the pin object has the loggedInFullName
                        // This ensures the offline filter matches exactly next time
                        var apiName = obj.optString("username", "")
                        if (apiName.isEmpty() || apiName == "Unknown") {
                            apiName = loggedInFullName
                        }

                        pinsList.add(
                            Pin(
                                id = obj.getInt("id"),
                                title = obj.getString("title"),
                                image = obj.getString("image"),
                                username = apiName,
                                userPfp = obj.optString("user_pfp", "")
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        adapter.updateData(pinsList)
                        adapter.setOnItemClickListener { pin ->
                            val intent = Intent(this@PinsPage, Page8Activity::class.java)
                            intent.putExtra("pinId", pin.id)
                            intent.putExtra("opened_from_pins_page", true)
                            startActivityForResult(intent, 100)
                        }
                    }

                    // ✅ SAVE TO DB
                    DatabaseHelper(this@PinsPage).savePins(pinsList)
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
                val json = JSONObject(response)

                if (json.optBoolean("success")) {
                    val user = json.getJSONObject("user")
                    val pfpString = if (user.isNull("profile_picture")) "" else user.optString("profile_picture")

                    if (pfpString.isNotEmpty()) {
                        try {
                            val cleanBase64 = if (pfpString.contains(",")) pfpString.split(",")[1] else pfpString
                            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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