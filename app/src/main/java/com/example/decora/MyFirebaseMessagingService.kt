package com.example.decora

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // We will handle saving the token in Page7Activity to ensure we have the UserID
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: "New Message"
        val body = remoteMessage.notification?.body ?: "You have a new message"

        // If data payload contains sender info, extract it
        val senderId = remoteMessage.data["sender_id"]

        showNotification(title, body, senderId)
    }

    private fun showNotification(title: String, body: String, senderId: String?) {
        val channelId = "chat_notifications"
        val notificationId = System.currentTimeMillis().toInt()

        // Create Intent to open Page19 (Chat) or Page18 (Inbox)
        val intent = if (senderId != null) {
            Intent(this, Page19Activity::class.java).apply {
                putExtra("partner_id", senderId.toInt())
                // We might not have the name here, so Page19 handles the loading
            }
        } else {
            Intent(this, Page18Activity::class.java)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo2) // Make sure this icon exists
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(notificationId, builder.build())
    }
}