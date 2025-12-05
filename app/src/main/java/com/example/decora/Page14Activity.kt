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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.decora.Config
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

class Page14Activity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvCount: TextView
    private lateinit var rvBoardPins: RecyclerView
    private var boardId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page14)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        boardId = intent.getIntExtra("board_id", -1)
        val tempTitle = intent.getStringExtra("board_name") ?: "Board"

        if (boardId == -1) {
            Toast.makeText(this, "Error: Invalid Board", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvTitle = findViewById(R.id.boardTitle)
        tvCount = findViewById(R.id.boardPinCount)
        rvBoardPins = findViewById(R.id.rvBoardPins)

        tvTitle.text = tempTitle

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        rvBoardPins.layoutManager = layoutManager

        findViewById<ImageView>(R.id.back).setOnClickListener { finish() }
        val pfpNav = findViewById<CircleImageView>(R.id.prof)
        loadCurrentUserProfile(pfpNav)
    }

    override fun onResume() {
        super.onResume()
        if (boardId != -1) {
            fetchBoardDetails()
            fetchBoardPins()
        }
    }

    // --- DELETE LOGIC ---

    private fun showDeleteDialog(pin: Pin) {
        AlertDialog.Builder(this)
            .setTitle("Remove Pin")
            .setMessage("Remove this pin from the board?")
            .setPositiveButton("Remove") { _, _ ->
                removePinFromBoard(pin.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removePinFromBoard(pinId: Int) {
        val urlString = Config.BASE_URL + "remove_board_pin.php"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("board_id", boardId)
                jsonParam.put("pin_id", pinId)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)

                withContext(Dispatchers.Main) {
                    if (json.optBoolean("success")) {
                        Toast.makeText(this@Page14Activity, "Pin Removed", Toast.LENGTH_SHORT).show()
                        // Refresh to update UI
                        fetchBoardDetails()
                        fetchBoardPins()
                    } else {
                        Toast.makeText(this@Page14Activity, "Failed: ${json.optString("message")}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Page14Activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- FETCHING LOGIC ---

    private fun fetchBoardDetails() {
        val urlString = Config.BASE_URL + "get_board_details.php"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("board_id", boardId)
                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)
                withContext(Dispatchers.Main) {
                    if (json.optBoolean("success")) {
                        val title = json.optString("title")
                        val count = json.optInt("pin_count")
                        tvTitle.text = title
                        tvCount.text = "$count pins"
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun fetchBoardPins() {
        val urlString = Config.BASE_URL + "get_board_pins.php"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("board_id", boardId)
                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    val pinsArray = json.getJSONArray("pins")
                    val pinsList = ArrayList<Pin>()
                    for (i in 0 until pinsArray.length()) {
                        val obj = pinsArray.getJSONObject(i)
                        pinsList.add(
                            Pin(
                                id = obj.getInt("id"),
                                title = obj.getString("title"),
                                image = obj.getString("image"),
                                username = obj.optString("username", "User"),
                                userPfp = obj.optString("user_pfp", "")
                            )
                        )
                    }
                    withContext(Dispatchers.Main) {
                        // Pass the delete logic here!
                        rvBoardPins.adapter = PinAdapter(pinsList) { pin ->
                            showDeleteDialog(pin)
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
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
                            withContext(Dispatchers.Main) { if (bitmap != null) targetImageView.setImageBitmap(bitmap) }
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {}
        }
    }
}