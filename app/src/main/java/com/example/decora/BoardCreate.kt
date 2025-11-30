package com.example.decora

import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.decora.com.example.decora.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class BoardCreate: AppCompatActivity() {

    private lateinit var etBoardName: EditText
    private var currentUserId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_board_create)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        currentUserId = sharedPrefs.getString("user_id", "-1")?.toIntOrNull() ?: -1

        etBoardName = findViewById(R.id.etBoardName)
        val btnDone = findViewById<TextView>(R.id.done)
        val btnBack = findViewById<ImageView>(R.id.back)

        btnBack.setOnClickListener { finish() }

        btnDone.setOnClickListener {
            val title = etBoardName.text.toString().trim()
            if (title.isNotEmpty()) {
                createBoard(title)
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createBoard(title: String) {
        val urlString = Config.BASE_URL + "create_board.php"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("user_id", currentUserId)
                jsonParam.put("title", title)

                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                val code = conn.responseCode
                if (code == 200) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BoardCreate, "Board Created!", Toast.LENGTH_SHORT).show()
                        finish() // Go back to Page 13
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BoardCreate, "Error: $code", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BoardCreate, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}