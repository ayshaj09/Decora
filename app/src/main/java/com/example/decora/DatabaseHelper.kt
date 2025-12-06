package com.example.decora

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues


class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "pins_db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            "CREATE TABLE pins (" +
                    "id INTEGER PRIMARY KEY, " +
                    "title TEXT, " +
                    "image TEXT, " +
                    "username TEXT, " +
                    "userPfp TEXT)"
        )


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


        db.execSQL(
            """
            CREATE TABLE boards (
                id INTEGER PRIMARY KEY, 
                title TEXT, 
                pin_count TEXT, 
                preview_images TEXT
            )
            """
        )


        db.execSQL(
            """
            CREATE TABLE board_pins (
                board_id INTEGER,
                pin_id INTEGER,
                PRIMARY KEY (board_id, pin_id)
            )
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS pins")
        db.execSQL("DROP TABLE IF EXISTS offline_queue")
        db.execSQL("DROP TABLE IF EXISTS boards")
        db.execSQL("DROP TABLE IF EXISTS board_pins")
        onCreate(db)
    }


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


    fun saveBoards(boards: List<Board>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM boards")
            for (board in boards) {
                val values = ContentValues()
                values.put("id", board.id)
                values.put("title", board.title)
                values.put("pin_count", board.pinCount)
                val imagesString = board.previewImages.joinToString(",")
                values.put("preview_images", imagesString)
                db.insert("boards", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getBoards(): ArrayList<Board> {
        val boardsList = ArrayList<Board>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM boards", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
                val count = cursor.getString(cursor.getColumnIndexOrThrow("pin_count"))
                val imagesStr = cursor.getString(cursor.getColumnIndexOrThrow("preview_images"))
                val imageList = if (imagesStr.isNotEmpty()) imagesStr.split(",").toList() else emptyList()
                boardsList.add(Board(id, title, count, imageList))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return boardsList
    }


    fun saveBoardPins(boardId: Int, pinList: List<Pin>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 1. Save pin details
            for (pin in pinList) {
                val cv = ContentValues()
                cv.put("id", pin.id)
                cv.put("title", pin.title)
                cv.put("image", pin.image)
                cv.put("username", pin.username)
                cv.put("userPfp", pin.userPfp)
                db.insertWithOnConflict("pins", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }


            db.execSQL("DELETE FROM board_pins WHERE board_id = ?", arrayOf(boardId))

            // 3. Save new links
            for (pin in pinList) {
                val cvLink = ContentValues()
                cvLink.put("board_id", boardId)
                cvLink.put("pin_id", pin.id)
                db.insert("board_pins", null, cvLink)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun getBoardPins(boardId: Int): ArrayList<Pin> {
        val pinList = ArrayList<Pin>()
        val db = readableDatabase

        val query = """
            SELECT p.* FROM pins p 
            INNER JOIN board_pins bp ON p.id = bp.pin_id 
            WHERE bp.board_id = ?
        """
        val cursor = db.rawQuery(query, arrayOf(boardId.toString()))

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