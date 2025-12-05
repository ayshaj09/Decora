package com.example.decora

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "pins_db", null, 3) {
    override fun onCreate(db: SQLiteDatabase) {
        // Existing table
        db.execSQL(
            "CREATE TABLE pins (" +
                    "id INTEGER PRIMARY KEY, " +
                    "title TEXT, " +
                    "image TEXT, " +
                    "username TEXT, " +
                    "userPfp TEXT)"
        )

        // NEW offline_queue table
        db.execSQL(
            """
        CREATE TABLE offline_queue (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER,
            action_type TEXT,
            action_data TEXT,
            status TEXT DEFAULT 'pending',
            retry_count INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
        """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS pins")
        db.execSQL("DROP TABLE IF EXISTS offline_queue")
        onCreate(db)
    }

    // SAVE LIST OF PINS
    fun savePins(pinList: List<Pin>) {
        val db = writableDatabase
        db.execSQL("DELETE FROM pins")

        for (pin in pinList) {
            val cv = ContentValues()
            cv.put("id", pin.id)
            cv.put("title", pin.title)
            cv.put("image", pin.image)
            cv.put("username", pin.username)
            cv.put("userPfp", pin.userPfp)
            db.insert("pins", null, cv)
        }

        db.close()
    }

    // LOAD CACHED PINS
    fun getPins(): List<Pin> {
        val pinList = ArrayList<Pin>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM pins", null)

        if (cursor.moveToFirst()) {
            do {
                pinList.add(
                    Pin(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        image = cursor.getString(cursor.getColumnIndexOrThrow("image")),
                        username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        userPfp = cursor.getString(cursor.getColumnIndexOrThrow("userPfp"))
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return pinList
    }
}
