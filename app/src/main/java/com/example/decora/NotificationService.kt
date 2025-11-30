package com.example.decora

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class NotificationService : Service() {

    private var databaseRef: DatabaseReference? = null
    private var listener: ChildEventListener? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Get Logged In User ID from SharedPreferences
        val sharedPrefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val myUserId = sharedPrefs.getString("user_id", "-1")

        if (myUserId != null && myUserId != "-1") {
            startListening(myUserId)
        } else {
            stopSelf()
        }

        // START_STICKY ensures the service restarts if Android kills it to save memory
        return START_STICKY
    }

    private fun startListening(userId: String) {
        // Prevent duplicate listeners
        if (databaseRef != null) return

        // Listen to: notifications -> [MY_USER_ID]
        databaseRef = FirebaseDatabase.getInstance().getReference("notifications").child(userId)

        listener = databaseRef?.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!snapshot.exists()) return

                // 2. Read the Signal
                val title = snapshot.child("title").getValue(String::class.java) ?: "New Message"
                val body = snapshot.child("body").getValue(String::class.java) ?: "You have a new message"
                val senderId = snapshot.child("senderId").getValue(String::class.java) ?: ""

                // 3. Show Notification
                NotificationHelper.showNotification(
                    applicationContext,
                    title,
                    body,
                    senderId
                )

                // 4. Delete the signal so it doesn't ring again later
                snapshot.ref.removeValue()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listener to prevent memory leaks
        if (databaseRef != null && listener != null) {
            databaseRef!!.removeEventListener(listener!!)
        }
    }
}