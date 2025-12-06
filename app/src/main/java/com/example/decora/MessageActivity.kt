package com.example.decora

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
    private lateinit var dbHelper: DatabaseHelper // Local DB Helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.messages_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Database Helper
        dbHelper = DatabaseHelper(this)

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

        // 1. Load Cached Users (Instant Offline Access)
        loadCachedUsers()

        // 2. Check Network and Sync
        if (isNetworkAvailable()) {
            loadUsersFromServer()
        } else {
            Toast.makeText(this, "Loading offline chats.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun loadCachedUsers() {
        try {
            // Fetch users stored in SQLite
            val localUsers = dbHelper.getChatUsers()
            if (localUsers.isNotEmpty()) {
                updateAdapter(localUsers)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadUsersFromServer() {
        // Use the filename from your working code
        val urlString = Config.BASE_URL + "fetch_chat_users.php"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
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

                if (conn.responseCode != 200) {
                    withContext(Dispatchers.Main) {
                        // Only show error if we have no cached data to show
                        if (rvUsers.adapter == null) {
                            Toast.makeText(this@MessageActivity, "Server Error: ${conn.responseCode}", Toast.LENGTH_LONG).show()
                        }
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
                        val pic = if (obj.isNull("profile_picture")) null else obj.getString("profile_picture")
                        val msg = if (obj.isNull("last_message")) null else obj.getString("last_message")
                        val time = if (obj.isNull("last_time")) null else obj.getString("last_time")

                        userList.add(UserChat(id, name, pic, msg, time))
                    }

                    // 3. Save to Local DB (Update Cache)
                    dbHelper.saveChatUsers(userList)

                    withContext(Dispatchers.Main) {
                        updateAdapter(userList)

                        if (userList.isEmpty()) {
                            Toast.makeText(this@MessageActivity, "No chats found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val errorMsg = jsonResponse.optString("message", "Unknown error")
                    withContext(Dispatchers.Main) {
                        if (rvUsers.adapter == null) {
                            Toast.makeText(this@MessageActivity, "Failed: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (rvUsers.adapter == null) {
                        Toast.makeText(this@MessageActivity, "Connection Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateAdapter(userList: List<UserChat>) {
        rvUsers.adapter = UsersAdapter(this@MessageActivity, userList) { selectedUser ->
            // Open Chat (Page 19)
            // Page 19 already handles offline message queuing!
            val intent = Intent(this@MessageActivity, Page19Activity::class.java)
            intent.putExtra("partner_id", selectedUser.id)
            intent.putExtra("partner_name", selectedUser.username)
            startActivity(intent)
        }
    }
}