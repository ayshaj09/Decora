package com.example.decora

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class Page6Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page6)


        val emailInput = findViewById<EditText>(R.id.email)
        val nameInput = findViewById<EditText>(R.id.yourname)
        val phoneInput = findViewById<EditText>(R.id.phoneNumber)
        val passInput = findViewById<EditText>(R.id.password)
        val confirmPassInput = findViewById<EditText>(R.id.confirmPassword)
        val createAccountBtn = findViewById<TextView>(R.id.createAccount)
        val backBtn = findViewById<ImageView>(R.id.back)
        val doneBtn = findViewById<TextView>(R.id.done)


        backBtn.setOnClickListener { finish() }
        doneBtn.setOnClickListener { finish() }


        createAccountBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val name = nameInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val pass = passInput.text.toString().trim()
            val confirmPass = confirmPassInput.text.toString().trim()


            if (email.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            registerUser(name, email, phone, pass)
        }
    }

    private fun registerUser(name: String, email: String, phone: String, pass: String) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
//updated
                val url = URL(Config.URL_SIGNUP)

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true


                val jsonParam = JSONObject()
                jsonParam.put("full_name", name)
                jsonParam.put("email", email)
                jsonParam.put("phone", phone)
                jsonParam.put("password", pass)


                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonParam.toString())
                writer.flush()


                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseText)

                    val success = jsonResponse.optBoolean("success")
                    val message = jsonResponse.optString("message")


                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Page6Activity, message, Toast.LENGTH_SHORT).show()
                        if (success) {
                            finish()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@Page6Activity, "Server Error: $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Page6Activity, "Connection Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}