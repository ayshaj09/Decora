package com.example.decora

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
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
import com.google.firebase.database.FirebaseDatabase
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
import java.util.ArrayList

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
    private lateinit var dbHelper: DatabaseHelper // Local Database

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

        dbHelper = DatabaseHelper(this)

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

        adapter = ChatAdapter(currentUserId) { message ->
            handleMessageLongClick(message)
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        rvChat.layoutManager = layoutManager
        rvChat.adapter = adapter

        // 1. Load from Local Cache FIRST (Instant Offline Access)
        loadCachedMessages()

        // 2. Load from Server if Online
        if (NetworkUtils.isNetworkAvailable(this)) {
            loadPartnerProfile()
            startMessagePolling()
        } else {
            Toast.makeText(this, "Loading offline messages.", Toast.LENGTH_SHORT).show()
        }

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                handleSendMessage(msg)
                etMessage.setText("")
            }
        }
    }

    private fun loadCachedMessages() {
        // Fetch from SQLite
        val localMsgs = dbHelper.getMessages(currentUserId, chatPartnerId)

        currentMessages.clear()
        currentMessages.addAll(localMsgs)

        adapter.setMessages(currentMessages)
        if (currentMessages.isNotEmpty()) {
            rvChat.scrollToPosition(currentMessages.size - 1)
        }
    }

    private fun handleSendMessage(text: String) {
        if (NetworkUtils.isNetworkAvailable(this)) {
            // Online: Send directly
            sendMessage(text)
        } else {
            // Offline: Queue it
            queueMessageOffline(text)
        }
    }

    private fun queueMessageOffline(text: String) {
        // 1. Show in UI Immediately (Optimistic UI)
        val tempMsg = Message(
            id = 0, // 0 ID means not yet synced
            senderId = currentUserId,
            receiverId = chatPartnerId,
            messageText = text,
            createdAt = "Sending..."
        )

        currentMessages.add(tempMsg)
        adapter.setMessages(currentMessages)
        rvChat.scrollToPosition(currentMessages.size - 1)

        // 2. Save to Local Database
        dbHelper.saveMessage(currentUserId, chatPartnerId, text, "Pending", 0)

        // 3. Add to Upload Queue
        val json = JSONObject()
        json.put("sender_id", currentUserId)
        json.put("receiver_id", chatPartnerId)
        json.put("message", text)
        // "user_id" is needed for the queue table logic
        json.put("user_id", currentUserId)

        OfflineQueueManager.addToQueue(this, "send_message", json.toString())

        Toast.makeText(this, "Offline: Message queued to send later", Toast.LENGTH_SHORT).show()
    }

    // --- EXISTING LOGIC (Updated for DB) ---

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
                            val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
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
        // Ensure you use the correct file name (get_messages.php)
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

                // 1. Clear old cache for this conversation to avoid duplicates
                dbHelper.clearMessagesForChat(currentUserId, chatPartnerId)

                for(i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)

                    val mId = obj.getInt("id")
                    val mSender = obj.getInt("sender_id")
                    val mReceiver = obj.getInt("receiver_id")
                    val mText = obj.getString("message_text")
                    val mTime = obj.getString("created_at")

                    newMsgs.add(Message(mId, mSender, mReceiver, mText, mTime))

                    // 2. Save to Local DB
                    dbHelper.saveMessage(mSender, mReceiver, mText, mTime, mId)
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

        FirebaseDatabase.getInstance()
            .getReference("notifications")
            .child(receiverId)
            .push()
            .setValue(notificationData)
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
                    val realId = json.optInt("id", 0)

                    withContext(Dispatchers.Main) {
                        // Show message instantly
                        val instantMsg = Message(
                            id = realId,
                            senderId = currentUserId,
                            receiverId = chatPartnerId,
                            messageText = text,
                            createdAt = "Just now"
                        )
                        currentMessages.add(instantMsg)
                        adapter.setMessages(currentMessages)
                        rvChat.scrollToPosition(currentMessages.size - 1)

                        // Save to Local DB (as Synced)
                        dbHelper.saveMessage(currentUserId, chatPartnerId, text, "Just now", realId)
                    }

                    sendNotificationSignal(chatPartnerId.toString(), text)
                    // Background refresh
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

    // --- LONG CLICK / EDIT LOGIC ---
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
                if (newText.isNotEmpty()) performEdit(message.id, newText)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Delete Message?")
            .setPositiveButton("Delete") { _, _ -> performDelete(message.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performEdit(msgId: Int, newText: String) {
        if (msgId == 0) {
            Toast.makeText(this, "Wait for message to sync...", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@Page19Activity, "Updated", Toast.LENGTH_SHORT).show()
                        fetchMessages()
                    } else {
                        Toast.makeText(this@Page19Activity, json.optString("message"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun performDelete(msgId: Int) {
        if (msgId == 0) {
            Toast.makeText(this, "Wait for message to sync...", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@Page19Activity, "Deleted", Toast.LENGTH_SHORT).show()
                        fetchMessages()
                    } else {
                        Toast.makeText(this@Page19Activity, json.optString("message"), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}