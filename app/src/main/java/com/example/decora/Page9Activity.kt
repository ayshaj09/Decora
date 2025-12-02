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

        // Fix System Bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Get Data
        pinIdToSave = intent.getIntExtra("pin_id_to_save", -1)
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

        if (pinIdToSave == -1) {
            Toast.makeText(this, "Error: No Pin selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val back=findViewById<ImageView>(R.id.back)
        back.setOnClickListener {
            finish()
        }
        // 2. Setup UI
        findViewById<ImageView>(R.id.back).setOnClickListener { finish() }
        rvBoards = findViewById(R.id.rvBoardsList)
        rvBoards.layoutManager = LinearLayoutManager(this)

        // 3. Load Boards
        loadBoards()
    }

    private fun loadBoards() {
        val urlString = Config.BASE_URL + "get_board.php"
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

                        // Parse Preview Images Array
                        val previewList = ArrayList<String>()
                        val imagesArray = obj.optJSONArray("preview_images")
                        if (imagesArray != null) {
                            for (j in 0 until imagesArray.length()) {
                                previewList.add(imagesArray.getString(j))
                            }
                        }

                        val count = obj.optInt("pin_count", 0)

                        boardsList.add(Board(
                            obj.getInt("id"),
                            obj.getString("title"),
                            "$count pins",
                            previewList // Pass the list of images
                        ))
                    }

                    withContext(Dispatchers.Main) {
                        rvBoards.adapter = BoardAdapter(this@Page9Activity, boardsList)
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
                        finish() // Close page on success
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Inner Adapter Class for Board Selection List
    inner class SimpleBoardAdapter(private val boards: List<Board>) :
        RecyclerView.Adapter<SimpleBoardAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Using a simple built-in Android layout item for the list row
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)

            // Customize text color since the background is dark
            val textView = view.findViewById<TextView>(android.R.id.text1)
            textView.setTextColor(resources.getColor(R.color.white, null))
            textView.textSize = 18f

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