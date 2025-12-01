package com.example.decora

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page5Activity : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page5)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 1. Bind Views
        val emailField = findViewById<EditText>(R.id.text3)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val loginButton = findViewById<RelativeLayout>(R.id.picture6)
        val togglePasswordBtn = findViewById<ImageButton>(R.id.togglePassword)
        val signupLink = findViewById<TextView>(R.id.signup)

        // Back button logic
        val mainLayout = findViewById<RelativeLayout>(R.id.main)
        val backButton = mainLayout.findViewWithTag<ImageButton>("back_arrow")
        backButton?.setOnClickListener { finish() }

        // 2. Handle Password Visibility Toggle
        togglePasswordBtn.setOnClickListener {
            if (isPasswordVisible) {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordBtn.setImageResource(R.drawable.eyeoff)
            } else {
                passwordField.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            isPasswordVisible = !isPasswordVisible
            passwordField.setSelection(passwordField.text.length)
        }

        // 3. Handle Login Click
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                performLogin(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Handle Sign Up Link
        signupLink.setOnClickListener {
            val intent = Intent(this, Page6Activity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin(emailInput: String, passInput: String) {
        val loginUrl = Config.URL_LOGIN

        lifecycleScope.launch(Dispatchers.IO) {
            var responseString = ""
            try {
                val url = URL(loginUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                conn.connectTimeout = 5000 // 5 seconds timeout
                conn.readTimeout = 5000

                // Create JSON Payload
                val jsonParam = JSONObject()
                jsonParam.put("email", emailInput)
                jsonParam.put("password", passInput)

                // Send Data
                val os = OutputStreamWriter(conn.outputStream)
                os.write(jsonParam.toString())
                os.flush()
                os.close()

                // Check HTTP Response Code
                val responseCode = conn.responseCode
                val inputStream: InputStream = if (responseCode in 200..299) {
                    conn.inputStream
                } else {
                    conn.errorStream
                }

                // Read Response
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                responseString = response.toString().trim()
                Log.d("LOGIN_DEBUG", "Code: $responseCode | Response: $responseString")

                // If empty response
                if (responseString.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Page5Activity, "Server returned empty response (Code $responseCode)", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                // Try to parse JSON
                // --- FIX: Handle both Object ({}) and Array ([]) responses ---
                val jsonResponse = if (responseString.startsWith("[")) {
                    val jsonArray = JSONArray(responseString)
                    if (jsonArray.length() > 0) jsonArray.getJSONObject(0) else JSONObject()
                } else if (responseString.startsWith("{")) {
                    JSONObject(responseString)
                } else {
                    // Response is not JSON (likely HTML error from PHP)
                    throw Exception("Invalid JSON: $responseString")
                }
                // -------------------------------------------------------------

                val success = jsonResponse.optBoolean("success")

                withContext(Dispatchers.Main) {
                    if (success) {
                        val userObj = jsonResponse.optJSONObject("user")
                        val fullName = userObj?.optString("full_name") ?: "User"
                        val userId = userObj?.optString("id") ?: ""

                        val sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isLoggedIn", true)
                        editor.putString("user_id", userId)
                        editor.putString("full_name", fullName)
                        editor.putString("email", emailInput)
                        editor.apply()

                        Toast.makeText(this@Page5Activity, "Welcome back, $fullName!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@Page5Activity, Page7Activity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Get message, default to "Unknown Error" if missing
                        var message = jsonResponse.optString("message")
                        if (message.isEmpty()) message = "Unknown Error (No message from server)"

                        Toast.makeText(this@Page5Activity, "Login Failed: $message", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    // Show the raw error or response string to help debug
                    val errorMsg = if (responseString.isNotEmpty()) "Server Error: $responseString" else "Connection Error: ${e.message}"
                    // Truncate if too long for Toast
                    val displayMsg = if (errorMsg.length > 100) errorMsg.substring(0, 100) + "..." else errorMsg
                    Toast.makeText(this@Page5Activity, displayMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}