package com.example.decora

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.decora.com.example.decora.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page3Activity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page3)

        val phoneInput = findViewById<EditText>(R.id.mobileNumberInput)
        val sendBtn = findViewById<RelativeLayout>(R.id.continueButton)

        sendBtn.setOnClickListener {
            val phone = phoneInput.text.toString().trim()
            if (phone.isNotEmpty()) {
                sendOtpAndMove(phone)
            } else {
                Toast.makeText(this, "Please enter phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendOtpAndMove(phone: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(Config.URL_SEND_OTP)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("phone", phone)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonParam.toString())
                writer.flush()

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")
                    val message = json.optString("message") // "OTP Sent! Code: 1234"

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Page3Activity, message, Toast.LENGTH_LONG).show()

                        if (success) {
                            // MOVE TO PAGE 4
                            val intent = Intent(this@Page3Activity, Page4Activity::class.java)
                            // IMPORTANT: Pass the phone number to the next screen!
                            intent.putExtra("PHONE_NUMBER", phone)
                            startActivity(intent)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Page3Activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}