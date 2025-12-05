package com.example.decora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MessageActivity : AppCompatActivity() {

    private lateinit var rvUsers: RecyclerView
    private var myUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.messages_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Get Logged In User ID
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val idString = sharedPrefs.getString("user_id", "-1")
        myUserId = idString?.toIntOrNull() ?: -1

        if (myUserId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rvUsers = findViewById(R.id.rvUsers)
        rvUsers.layoutManager = LinearLayoutManager(this)

        // 2. Load Users
        loadUsers()
    }

    private fun loadUsers() {
        val urlString = Config.BASE_URL + "fetch_chat_users.php"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ... existing connection setup ...
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("user_id", myUserId)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                // Check for Server Errors (500, 404, etc)
                val responseCode = conn.responseCode
                if (responseCode != 200) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MessageActivity, "Server Error: $responseCode", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonResponse = JSONObject(response)

                if (jsonResponse.optBoolean("success")) {
                    val usersArray = jsonResponse.getJSONArray("users")
                    val userList = ArrayList<UserChat>()

                    for (i in 0 until usersArray.length()) {
                        val obj = usersArray.getJSONObject(i)

                        val id = obj.getInt("id")
                        val name = obj.getString("full_name")

                        // Handle nulls safely
                        val pic = if (obj.isNull("profile_picture")) null else obj.getString("profile_picture")
                        val msg = if (obj.isNull("last_message")) null else obj.getString("last_message")
                        val time = if (obj.isNull("last_time")) null else obj.getString("last_time")

                        userList.add(UserChat(id, name, pic, msg, time))
                    }

                    withContext(Dispatchers.Main) {
                        if (userList.isEmpty()) {
                            Toast.makeText(this@MessageActivity, "No other users found", Toast.LENGTH_SHORT).show()
                        }
                        rvUsers.adapter = UsersAdapter(this@MessageActivity, userList) { selectedUser ->
                            // This code runs when an item is clicked: Open Chat (Page 19)
                            val intent = Intent(this@MessageActivity, Page19Activity::class.java)
                            intent.putExtra("partner_id", selectedUser.id)
                            intent.putExtra("partner_name", selectedUser.username)
                            startActivity(intent)
                        }
                    }
                } else {
                    // NEW: Show error message if success is false
                    val errorMsg = jsonResponse.optString("message", "Unknown error")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MessageActivity, "Failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MessageActivity, "App Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
