package com.example.decora

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SharePinActivity : AppCompatActivity() {

    private lateinit var rvUsers: RecyclerView
    private var myUserId: Int = -1

    // --- FIX: Declare these variables so they can be used in onCreate ---
    private var pinTitle: String = ""
    private var pinId: Int = -1
    private var pinImage: String = ""
    private var pinDesc: String = ""
    // -------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_pin)

        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val idString = sharedPrefs.getString("user_id", "-1")
        myUserId = idString?.toIntOrNull() ?: -1

        // 1. Get Pin Data passed from Page 8
        pinTitle = intent.getStringExtra("pinTitle") ?: "this pin"
        pinId = intent.getIntExtra("pinId", -1)

        // These lines will now work because variables are declared above
        pinImage = intent.getStringExtra("pinImage") ?: ""
        pinDesc = intent.getStringExtra("pinDesc") ?: ""

        findViewById<ImageView>(R.id.backBtn).setOnClickListener { finish() }

        rvUsers = findViewById(R.id.rvShareUsers)
        rvUsers.layoutManager = LinearLayoutManager(this)

        loadUsersToShare()
    }

    private fun loadUsersToShare() {
        val urlString = Config.BASE_URL + "fetch_chat_users.php"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("user_id", myUserId)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonParam.toString())
                writer.flush()
                writer.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)

                if (json.optBoolean("success")) {
                    val usersArray = json.getJSONArray("users")
                    val userList = ArrayList<UserChat>()

                    for (i in 0 until usersArray.length()) {
                        val obj = usersArray.getJSONObject(i)
                        userList.add(
                            UserChat(
                                obj.getInt("id"),
                                obj.getString("full_name"),
                                obj.optString("profile_picture", null),
                                "",
                                ""
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        rvUsers.adapter = ShareUserAdapter(this@SharePinActivity, userList) { user ->
                            sharePinToUser(user)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sharePinToUser(user: UserChat) {
        val urlString = Config.BASE_URL + "send_message.php"

        // 2. Create a Structured JSON Message for the ChatAdapter
        val messagePayload = JSONObject()
        messagePayload.put("type", "share_pin")
        messagePayload.put("title", pinTitle)
        messagePayload.put("image", pinImage)
        messagePayload.put("id", pinId)
        messagePayload.put("caption", pinDesc)

        val messageText = messagePayload.toString()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("sender_id", myUserId)
                jsonParam.put("receiver_id", user.id)
                jsonParam.put("message", messageText) // Send JSON string

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonParam.toString())
                writer.flush()
                writer.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)

                withContext(Dispatchers.Main) {
                    if (json.optBoolean("success")) {
                        // Send Notification Signal
                        sendNotificationSignal(user.id.toString(), "Shared a Pin: $pinTitle")
                        Toast.makeText(this@SharePinActivity, "Sent to ${user.username}!", Toast.LENGTH_SHORT).show()
                        finish() // Close page
                    } else {
                        Toast.makeText(this@SharePinActivity, "Failed to send", Toast.LENGTH_SHORT).show()
                        withContext(Dispatchers.Main) {
                            if (json.optBoolean("success")) {
                                sendNotificationSignal(user.id.toString(), "Shared a Pin: $pinTitle")
                                Toast.makeText(this@SharePinActivity, "Sent to ${user.username}!", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                // --- FIX: Show the actual error from PHP ---
                                val errorMsg = json.optString("message", "Unknown Error")
                                Toast.makeText(this@SharePinActivity, "Failed: $errorMsg", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendNotificationSignal(receiverId: String, messageText: String) {
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val myName = sharedPrefs.getString("full_name", "Someone") ?: "New Message"

        val notificationData = HashMap<String, String>()
        notificationData["title"] = myName
        notificationData["body"] = messageText
        notificationData["senderId"] = myUserId.toString()
        notificationData["type"] = "chat"

        FirebaseDatabase.getInstance()
            .getReference("notifications")
            .child(receiverId)
            .push()
            .setValue(notificationData)
    }
}