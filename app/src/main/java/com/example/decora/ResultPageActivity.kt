package com.example.decora

import android.os.Bundle
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.decora.com.example.decora.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ResultPageActivity : AppCompatActivity() {

    // Use the existing Pin class for this
    private val pinList = ArrayList<Pin>()
    private lateinit var adapter: PinResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ensure this matches your XML file name
        setContentView(R.layout.activity_result_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val searchQuery = intent.getStringExtra("EXTRA_QUERY") ?: ""

        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchInput.setText(searchQuery)

        val recyclerView = findViewById<RecyclerView>(R.id.pinsRecyclerView)

        // Staggered Grid for Pinterest look
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.layoutManager = layoutManager

        adapter = PinResultAdapter(pinList)
        recyclerView.adapter = adapter

        findViewById<TextView>(R.id.cancel).setOnClickListener { finish() }

        fetchPins(searchQuery)
    }

    private fun fetchPins(query: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlStr = "${Config.URL_SEARCH_PINS}?query=$query"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    if (json.optBoolean("success")) {
                        val jsonArray = json.getJSONArray("pins")
                        pinList.clear()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)

                            // 1. Get available data
                            val idString = obj.optString("id")
                            val id = idString.toIntOrNull() ?: 0 // Safely convert to Int
                            val title = obj.optString("title")
                            val img = obj.optString("image")

                            // 2. Create existing Pin object
                            // We pass "" for username and userPfp since the search API doesn't send them yet
                            pinList.add(Pin(id, title, img, "", ""))
                        }

                        withContext(Dispatchers.Main) {
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResultPageActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }//phas gya
        }
    }
}