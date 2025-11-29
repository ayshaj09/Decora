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

class Page4Activity : AppCompatActivity() {

    var phoneNumber: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page4)


        phoneNumber = intent.getStringExtra("PHONE_NUMBER")

        val otpInput = findViewById<EditText>(R.id.otpInput)
        val verifyBtn = findViewById<RelativeLayout>(R.id.verifyOtp)

        verifyBtn.setOnClickListener {
            val code = otpInput.text.toString().trim()
            if (code.isNotEmpty() && phoneNumber != null) {
                verifyOtp(phoneNumber!!, code)
            } else {
                Toast.makeText(this, "Enter the code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verifyOtp(phone: String, code: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(Config.URL_VERIFY_OTP)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("phone", phone)
                jsonParam.put("otp_code", code)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonParam.toString())
                writer.flush()

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val success = json.optBoolean("success")
                    val message = json.optString("message")

                    withContext(Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(this@Page4Activity, "Login Success!", Toast.LENGTH_SHORT).show()

                            // 2. Navigate to Home Page (e.g., Page12)
                            val intent = Intent(this@Page4Activity, Page7Activity::class.java)
                            // Clear history so they can't go back to OTP screen
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@Page4Activity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}