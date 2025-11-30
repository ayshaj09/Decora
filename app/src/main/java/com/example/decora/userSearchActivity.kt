package com.example.decora

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.decora.com.example.decora.Config // Check your package path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class userSearchActivity : AppCompatActivity() {

    private val allUsersList = ArrayList<User>() // Holds database data
    private lateinit var userAdapter: UserAdapter // Connects data to RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_search)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.userRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize Adapter with empty list first
        userAdapter = UserAdapter(ArrayList())
        recyclerView.adapter = userAdapter

        // 2. Navigation
        findViewById<TextView>(R.id.cancel).setOnClickListener { finish() }
        findViewById<TextView>(R.id.pinsTab).setOnClickListener { finish() }

        // 3. Fetch Data
        fetchUsers()

        // 4. Search Filter
        val searchInput = findViewById<EditText>(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase(Locale.getDefault())
                filterUsers(query)
            }
        })
    }

    private fun fetchUsers() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure Config.URL_GET_USERS is correct
                val url = URL(Config.URL_GET_USERS)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")

                    if (success) {
                        val usersArray = json.getJSONArray("users")
                        allUsersList.clear()

                        for (i in 0 until usersArray.length()) {
                            val userObj = usersArray.getJSONObject(i)
                            val id = userObj.optString("id")
                            val name = userObj.optString("full_name")
                            val base64Image = userObj.optString("profile_picture")
                            allUsersList.add(User(id, name, base64Image))
                        }

                        // Update RecyclerView on Main Thread
                        withContext(Dispatchers.Main) {
                            // Show all users initially
                            userAdapter.updateList(allUsersList)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@userSearchActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterUsers(query: String) {
        val filteredList = ArrayList<User>()
        for (user in allUsersList) {
            if (user.name.lowercase(Locale.getDefault()).contains(query)) {
                filteredList.add(user)
            }
        }
        // Tell Adapter to show only filtered results
        userAdapter.updateList(filteredList)
    }
}