package com.example.decora

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

    // Search Input Field
    private lateinit var searchInput: EditText

    // 1. Create the Launcher for Voice Search Result
    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data
            // Get the list of spoken text results
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

            // The first result is usually the most accurate
            val spokenText = results?.get(0) ?: ""

            if (spokenText.isNotEmpty()) {
                // Set text to search bar (this will trigger the TextWatcher filter automatically!)
                searchInput.setText(spokenText)

                // Move cursor to the end of the text
                searchInput.setSelection(spokenText.length)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page11)

        // 2. Handle Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 3. Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.pinsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with empty list first
        adapter = PinSearchAdapter(ArrayList())
        recyclerView.adapter = adapter

        // 4. Fetch Real Data from Database
        fetchPinTitles()

        // 5. Setup Search Listener & Mic
        searchInput = findViewById(R.id.searchInput)
        val micIcon = findViewById<ImageView>(R.id.micIcon)

        // Text Watcher (Filters as you type)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase(Locale.getDefault())
                filterTitles(query)
            }
        })

        // Mic Click Listener (Launches Voice Search)
        micIcon.setOnClickListener {
            startVoiceRecognition()
        }

        // 6. Navigation Logic
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

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        // Use free form language model (better for general search terms)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)

        // Prompt text shown to user
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search pins...")

        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice search not supported on this device", Toast.LENGTH_SHORT).show()
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