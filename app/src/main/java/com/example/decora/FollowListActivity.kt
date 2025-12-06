package com.example.decora

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FollowListActivity : AppCompatActivity() {

    private lateinit var adapter: UserAdapter
    private val userList = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_list)


        val type = intent.getStringExtra("TYPE") ?: "followers"
        val titleText = if (type == "followers") "Followers" else "Following"

        // 2. Setup UI
        val titleView = findViewById<TextView>(R.id.pageTitle)
        titleView.text = titleText

        val backBtn = findViewById<ImageView>(R.id.backBtn)
        backBtn.setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.userRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)


        adapter = UserAdapter(userList)
        recyclerView.adapter = adapter


        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPrefs.getString("user_id", "-1") ?: "-1"


        val userIdToFetch = intent.getStringExtra("USER_ID_OVERRIDE") ?: loggedInUserId

        if (userIdToFetch == "-1" || userIdToFetch.isEmpty()) {
            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        fetchConnections(userIdToFetch, type)
    }

    private fun fetchConnections(userId: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Config.URL_GET_CONNECTIONS = .../get_connections.php
                val urlStr = "${Config.URL_GET_CONNECTIONS}?user_id=$userId&type=$type"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    if (json.optBoolean("success")) {
                        val usersArray = json.getJSONArray("users")
                        userList.clear()

                        for (i in 0 until usersArray.length()) {
                            val obj = usersArray.getJSONObject(i)

                            // Extract User Data
                            val id = obj.optString("id")
                            val name = obj.optString("full_name")
                            val pfp = obj.optString("profile_picture")

                            userList.add(User(id, name, pfp))
                        }

                        withContext(Dispatchers.Main) {
                            adapter.notifyDataSetChanged()


                            if (userList.isEmpty()) {
                                Toast.makeText(this@FollowListActivity, "No users found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FollowListActivity, "Error loading list", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}