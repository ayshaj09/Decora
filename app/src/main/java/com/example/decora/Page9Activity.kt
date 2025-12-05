package com.example.decora

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

class Page9Activity : AppCompatActivity() {

    private lateinit var rvBoards: RecyclerView
    private var pinIdToSave: Int = -1
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page9)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pinIdToSave = intent.getIntExtra("pin_id_to_save", -1)
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

        if (pinIdToSave == -1) {
            Toast.makeText(this, "Error: No Pin selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.back).setOnClickListener { finish() }
        rvBoards = findViewById(R.id.rvBoardsList)
        rvBoards.layoutManager = LinearLayoutManager(this)

        loadBoards()
    }

    private fun loadBoards() {
        val urlString = Config.BASE_URL + "get_board.php" // Use standard get_boards

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("user_id", currentUserId)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)

                if (json.optBoolean("success")) {
                    val jsonArray = json.getJSONArray("boards")
                    val boardsList = ArrayList<Board>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        // We only need ID and Title for this list
                        boardsList.add(Board(
                            obj.getInt("id"),
                            obj.getString("title"),
                            "",
                            emptyList()
                        ))
                    }

                    withContext(Dispatchers.Main) {
                        rvBoards.adapter = SimpleBoardAdapter(boardsList)
                        if (boardsList.isEmpty()) {
                            Toast.makeText(this@Page9Activity, "No boards found. Create one first!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun savePinToBoard(boardId: Int) {
        val urlString = Config.BASE_URL + "save_pin.php"
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val jsonParam = JSONObject()
                jsonParam.put("board_id", boardId)
                jsonParam.put("pin_id", pinIdToSave)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                val json = JSONObject(response)

                withContext(Dispatchers.Main) {
                    val msg = json.optString("message")
                    Toast.makeText(this@Page9Activity, msg, Toast.LENGTH_SHORT).show()
                    if (json.optBoolean("success")) {
                        finish() // Close page after saving
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- SIMPLE ADAPTER (Text Only) ---
    inner class SimpleBoardAdapter(private val boards: List<Board>) :
        RecyclerView.Adapter<SimpleBoardAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            // Bind to the TextView in our new simple layout
            val title: TextView = itemView.findViewById(R.id.boardName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Inflate item_board_simple.xml
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_board_simple, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val board = boards[position]
            holder.title.text = board.title

            holder.itemView.setOnClickListener {
                savePinToBoard(board.id)
            }
        }

        override fun getItemCount() = boards.size
    }
}