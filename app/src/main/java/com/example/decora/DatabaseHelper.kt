package com.example.decora

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "pins_db", null, 6) { // Updated Version to 6

    override fun onCreate(db: SQLiteDatabase) {

        // 1. Pins Table
        db.execSQL("CREATE TABLE pins (id INTEGER PRIMARY KEY, title TEXT, image TEXT, username TEXT, userPfp TEXT)")

        // 2. Offline Queue
        db.execSQL("""
            CREATE TABLE offline_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                action_type TEXT,
                action_data TEXT,
                status TEXT DEFAULT 'pending',
                retry_count INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        // 3. Boards & Relations
        db.execSQL("CREATE TABLE boards (id INTEGER PRIMARY KEY, title TEXT, pin_count TEXT, preview_images TEXT)")
        db.execSQL("CREATE TABLE board_pins (board_id INTEGER, pin_id INTEGER, PRIMARY KEY (board_id, pin_id))")

        // 4. Local Messages (Single Chat History)
        db.execSQL("""
            CREATE TABLE local_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                server_id INTEGER,
                sender_id INTEGER,
                receiver_id INTEGER,
                message_text TEXT,
                created_at TEXT
            )
        """)

        // 5. NEW: Local Chat Users (Inbox List)
        db.execSQL("""
            CREATE TABLE local_chat_users (
                id INTEGER PRIMARY KEY, 
                username TEXT, 
                profile_pic TEXT, 
                last_message TEXT, 
                last_time TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS pins")
        db.execSQL("DROP TABLE IF EXISTS offline_queue")
        db.execSQL("DROP TABLE IF EXISTS boards")
        db.execSQL("DROP TABLE IF EXISTS board_pins")
        db.execSQL("DROP TABLE IF EXISTS local_messages")
        db.execSQL("DROP TABLE IF EXISTS local_chat_users") // Drop new table
        onCreate(db)
    }

    // --- INBOX FUNCTIONS (NEW) ---

    fun saveChatUsers(users: List<UserChat>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Clear old cache to ensure fresh list
            db.execSQL("DELETE FROM local_chat_users")

            for (user in users) {
                val cv = ContentValues().apply {
                    put("id", user.id)
                    put("username", user.username)
                    put("profile_pic", user.profilePic)
                    put("last_message", user.lastMessage)
                    put("last_time", user.timestamp)
                }
                db.insert("local_chat_users", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getChatUsers(): ArrayList<UserChat> {
        val list = ArrayList<UserChat>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM local_chat_users", null)

        if (cursor.moveToFirst()) {
            do {
                list.add(UserChat(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    profilePic = cursor.getString(cursor.getColumnIndexOrThrow("profile_pic")),
                    lastMessage = cursor.getString(cursor.getColumnIndexOrThrow("last_message")),
                    timestamp = cursor.getString(cursor.getColumnIndexOrThrow("last_time"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    // --- EXISTING FUNCTIONS ---
    // (Keeping your existing pin, board, and message functions here for completeness)

    fun savePins(pinList: List<Pin>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (pin in pinList) {
                val cv = ContentValues()
                cv.put("id", pin.id)
                cv.put("title", pin.title)
                cv.put("image", pin.image)
                cv.put("username", pin.username)
                cv.put("userPfp", pin.userPfp)
                db.insertWithOnConflict("pins", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getPins(): List<Pin> {
        val pinList = ArrayList<Pin>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM pins", null)
        if (cursor.moveToFirst()) {
            do {
                pinList.add(Pin(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    image = cursor.getString(cursor.getColumnIndexOrThrow("image")),
                    username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    userPfp = cursor.getString(cursor.getColumnIndexOrThrow("userPfp"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return pinList
    }

    // Board Functions...
    fun saveBoards(boards: List<Board>) { /* ... */ }
    fun getBoards(): ArrayList<Board> { return ArrayList() }
    fun saveBoardPins(boardId: Int, pinList: List<Pin>) { /* ... */ }
    fun getBoardPins(boardId: Int): ArrayList<Pin> { return ArrayList() }

    // Message Functions
    fun saveMessage(senderId: Int, receiverId: Int, message: String, timestamp: String, serverId: Int = 0) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("server_id", serverId)
            put("sender_id", senderId)
            put("receiver_id", receiverId)
            put("message_text", message)
            put("created_at", timestamp)
        }
        db.insert("local_messages", null, values)
        db.close()
    }

    fun getMessages(u1: Int, u2: Int): ArrayList<Message> {
        val list = ArrayList<Message>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM local_messages WHERE (sender_id=$u1 AND receiver_id=$u2) OR (sender_id=$u2 AND receiver_id=$u1) ORDER BY id ASC",
            null
        )
        if (cursor.moveToFirst()) {
            do {
                list.add(Message(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("server_id")),
                    senderId = cursor.getInt(cursor.getColumnIndexOrThrow("sender_id")),
                    receiverId = cursor.getInt(cursor.getColumnIndexOrThrow("receiver_id")),
                    messageText = cursor.getString(cursor.getColumnIndexOrThrow("message_text")),
                    createdAt = cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun clearMessagesForChat(u1: Int, u2: Int) {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM local_messages WHERE (sender_id=$u1 AND receiver_id=$u2) OR (sender_id=$u2 AND receiver_id=$u1)")
        db.close()
    }
}