package com.example.decora

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class Page11Activity : AppCompatActivity() {

    // List to store all titles fetched from the database
    private val allTitles = ArrayList<String>()

    // Adapter to connect the list to the RecyclerView
    private lateinit var adapter: PinSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page11)

        // 1. Handle Window Insets (Your existing code)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 2. Setup RecyclerView
        // Make sure your XML has <androidx.recyclerview.widget.RecyclerView android:id="@+id/pinsRecyclerView" ... />
        val recyclerView = findViewById<RecyclerView>(R.id.pinsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with empty list first
        adapter = PinSearchAdapter(ArrayList())
        recyclerView.adapter = adapter

        // 3. Fetch Real Data from Database
        fetchPinTitles()

        // 4. Setup Search Listener (Filter as you type)
        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase(Locale.getDefault())
                filterTitles(query)
            }
        })

        // 5. Navigation Logic
        val userSearch = findViewById<TextView>(R.id.userSearch)
        userSearch.setOnClickListener {
            val intent = Intent(this, userSearchActivity::class.java)
            startActivity(intent)
            // finish() // Optional: Keep this if you want to close Page11 when going to User Search
        }

        val cancelBtn = findViewById<TextView>(R.id.cancel)
        cancelBtn.setOnClickListener {
            finish() // Close activity
        }
    }

    private fun fetchPinTitles() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure Config.URL_GET_PIN_TITLES is set in Config.kt
                val url = URL(Config.URL_GET_PIN_TITLES)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    if (json.optBoolean("success")) {
                        val jsonArray = json.getJSONArray("titles")

                        allTitles.clear()
                        for (i in 0 until jsonArray.length()) {
                            allTitles.add(jsonArray.getString(i))
                        }

                        // Update UI on Main Thread
                        withContext(Dispatchers.Main) {
                            adapter.updateList(allTitles)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Optional: Toast.makeText(this@Page11Activity, "Error loading pins", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterTitles(query: String) {
        val filteredList = ArrayList<String>()
        for (title in allTitles) {
            // Check if title starts with the query (e.g., "sof" matches "Sofa")
            if (title.lowercase(Locale.getDefault()).startsWith(query)) {
                filteredList.add(title)
            }
        }
        // Update the adapter with the filtered list
        adapter.updateList(filteredList)
    }
}