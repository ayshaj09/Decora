package com.example.decora

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.decora.com.example.decora.Config
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class Page19Activity : AppCompatActivity() {

    private lateinit var adapter: ChatAdapter
    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var imgPfp: CircleImageView
    private lateinit var tvName: TextView

    private var currentUserId: Int = -1
    private var chatPartnerId: Int = -1
    private var chatPartnerName: String = ""

    // Store messages locally so we can update them instantly
    private val currentMessages = ArrayList<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_page19)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chatPartnerId = intent.getIntExtra("partner_id", -1)
        chatPartnerName = intent.getStringExtra("partner_name") ?: "User"
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

        if (chatPartnerId == -1 || currentUserId == -1) {
            Toast.makeText(this, "Error: User ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        imgPfp = findViewById(R.id.pfp)
        tvName = findViewById(R.id.name)
        val btnBack = findViewById<ImageView>(R.id.back)
        rvChat = findViewById(R.id.rvChat)
        etMessage = findViewById(R.id.messageInput)
        btnSend = findViewById(R.id.send)

        tvName.text = chatPartnerName
        btnBack.setOnClickListener { finish() }

        // Updated Adapter Init with Long Click Handler
        adapter = ChatAdapter(currentUserId) { message ->
            handleMessageLongClick(message)
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvChat.layoutManager = layoutManager
        rvChat.adapter = adapter

        loadPartnerProfile()

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
                etMessage.setText("")
            }
        }

        startMessagePolling()
    }

    // --- LONG CLICK & EDIT LOGIC ---

    private fun handleMessageLongClick(message: Message) {
        if (message.senderId != currentUserId) return

        showEditDeleteDialog(message)
    }

    private fun showEditDeleteDialog(message: Message) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Message Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditInput(message)
                    1 -> confirmDelete(message)
                }
            }
            .show()
    }

    private fun showEditInput(message: Message) {
        val input = EditText(this)
        input.setText(message.messageText)

        AlertDialog.Builder(this)
            .setTitle("Edit Message")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    performEdit(message.id, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Delete Message?")
            .setPositiveButton("Delete") { _, _ ->
                performDelete(message.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performEdit(msgId: Int, newText: String) {
        // If ID is 0, we can't edit on server yet
        if (msgId == 0) {
            Toast.makeText(this, "Wait a moment for message to sync...", Toast.LENGTH_SHORT).show()
            return
        }

        val urlString = Config.BASE_URL + "edit_message.php"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("message_id", msgId)
                jsonParam.put("new_text", newText)
                jsonParam.put("user_id", currentUserId)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)

                withContext(Dispatchers.Main) {
                    if (json.optBoolean("success")) {
                        Toast.makeText(this@Page19Activity, "Message Updated", Toast.LENGTH_SHORT).show()
                        fetchMessages() // Refresh list
                    } else {
                        Toast.makeText(this@Page19Activity, json.optString("message"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun performDelete(msgId: Int) {
        if (msgId == 0) {
            Toast.makeText(this, "Wait a moment for message to sync...", Toast.LENGTH_SHORT).show()
            return
        }

        val urlString = Config.BASE_URL + "delete_message.php"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("message_id", msgId)
                jsonParam.put("user_id", currentUserId)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)

                withContext(Dispatchers.Main) {
                    if (json.optBoolean("success")) {
                        Toast.makeText(this@Page19Activity, "Message Deleted", Toast.LENGTH_SHORT).show()
                        fetchMessages() // Refresh list
                    } else {
                        Toast.makeText(this@Page19Activity, json.optString("message"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- STANDARD LOGIC ---

    private fun loadPartnerProfile() {
        val urlString = Config.BASE_URL + "get_user_profile.php"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("user_id", chatPartnerId)
                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    val user = json.getJSONObject("user")
                    val name = user.optString("full_name")
                    val pfpString = if (user.isNull("profile_picture")) "" else user.optString("profile_picture")
                    withContext(Dispatchers.Main) { tvName.text = name }
                    if (pfpString.isNotEmpty()) {
                        try {
                            val cleanBase64 = if (pfpString.contains(",")) pfpString.split(",")[1] else pfpString
                            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            withContext(Dispatchers.Main) { if (bitmap != null) imgPfp.setImageBitmap(bitmap) }
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun startMessagePolling() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                fetchMessages()
                delay(2000)
            }
        }
    }

    private suspend fun fetchMessages() {
        val urlString = Config.BASE_URL + "recieve_message.php"
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            val jsonParam = JSONObject()
            jsonParam.put("user1_id", currentUserId)
            jsonParam.put("user2_id", chatPartnerId)
            val os = OutputStreamWriter(conn.outputStream)
            os.write(jsonParam.toString())
            os.flush()
            os.close()

            if (conn.responseCode != 200) return

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()

            val jsonResponse = JSONObject(response)
            if (jsonResponse.optBoolean("success")) {
                val jsonArray = jsonResponse.getJSONArray("messages")
                val newMsgs = ArrayList<Message>()

                for(i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    newMsgs.add(Message(
                        obj.getInt("id"),
                        obj.getInt("sender_id"),
                        obj.getInt("receiver_id"),
                        obj.getString("message_text"),
                        obj.getString("created_at")
                    ))
                }

                withContext(Dispatchers.Main) {
                    val isAtBottom = !rvChat.canScrollVertically(1)
                    currentMessages.clear()
                    currentMessages.addAll(newMsgs)
                    adapter.setMessages(currentMessages)

                    if (newMsgs.isNotEmpty() && isAtBottom) {
                        rvChat.scrollToPosition(newMsgs.size - 1)
                    }
                }
            }
        } catch (e: Exception) { }
    }
    private fun sendNotificationSignal(receiverId: String, messageText: String) {
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val myName = sharedPrefs.getString("full_name", "Someone") ?: "New Message"

        val notificationData = HashMap<String, String>()
        notificationData["title"] = myName
        notificationData["body"] = messageText
        notificationData["senderId"] = currentUserId.toString()
        notificationData["type"] = "chat"

        // Write to Firebase Realtime Database
        com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("notifications")
            .child(receiverId)
            .push()
            .setValue(notificationData)
            .addOnSuccessListener {
                // SUCCESS: You will see this in Logcat
                android.util.Log.d("NOTIF_DEBUG", "Signal sent successfully to user $receiverId")

                // Optional: Toast for testing (remove later)
                // Toast.makeText(this, "Notification Signal Sent!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // FAILURE: Check Logcat for permission errors
                android.util.Log.e("NOTIF_DEBUG", "Failed to send signal: ${e.message}")
                Toast.makeText(this, "Notif Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun sendMessage(text: String) {
        val urlString = Config.BASE_URL + "send_message.php"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("sender_id", currentUserId)
                jsonParam.put("receiver_id", chatPartnerId)
                jsonParam.put("message", text)
                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (json.optBoolean("success")) {
                    // FIX: Get the Real ID from the server
                    val realId = json.optInt("id", 0) // Defaults to 0 if not found

                    withContext(Dispatchers.Main) {
                        val instantMsg = Message(
                            id = realId, // Use Real ID here!
                            senderId = currentUserId,
                            receiverId = chatPartnerId,
                            messageText = text,
                            createdAt = "Just now"
                        )
                        currentMessages.add(instantMsg)
                        adapter.setMessages(currentMessages)
                        rvChat.scrollToPosition(currentMessages.size - 1)
                    }
                    sendNotificationSignal(chatPartnerId.toString(), text)
                    // Background refresh to confirm order
                    fetchMessages()
                } else {
                    val error = json.optString("message", "Unknown error")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Page19Activity, "Send Failed: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Page19Activity, "Net Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}