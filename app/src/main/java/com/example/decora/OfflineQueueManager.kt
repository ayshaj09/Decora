package com.example.decora

import android.content.ContentValues
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONObject

object OfflineQueueManager {

    fun addToQueue(context: Context, actionType: String, actionData: String)
    {
        val db = DatabaseHelper(context).writableDatabase

        val values = ContentValues().apply {
            put("user_id", JSONObject(actionData).getInt("user_id"))
            put("action_type", actionType)
            put("action_data", actionData)
            put("status", "pending")
            put("retry_count", 0)
        }

        db.insert("offline_queue", null, values)

        triggerSync(context)
    }

    fun getPending(context: Context): List<QueuedAction> {
        val db = DatabaseHelper(context).readableDatabase
        val list = mutableListOf<QueuedAction>()

        val cursor = db.rawQuery(
            "SELECT * FROM offline_queue WHERE status='pending' ORDER BY created_at ASC",
            null
        )

        while (cursor.moveToNext()) {
            list.add(
                QueuedAction(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                    actionType = cursor.getString(cursor.getColumnIndexOrThrow("action_type")),
                    actionData = cursor.getString(cursor.getColumnIndexOrThrow("action_data")),
                    retryCount = cursor.getInt(cursor.getColumnIndexOrThrow("retry_count"))
                )
            )
        }

        cursor.close()
        return list
    }

    fun markCompleted(context: Context, id: Int) {
        val db = DatabaseHelper(context).writableDatabase
        db.execSQL("UPDATE offline_queue SET status='completed' WHERE id=$id")
    }

    fun markFailed(context: Context, id: Int) {
        val db = DatabaseHelper(context).writableDatabase
        db.execSQL("UPDATE offline_queue SET retry_count = retry_count + 1 WHERE id=$id")
    }

    fun triggerSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag("offline_sync")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("OfflineSyncWork", ExistingWorkPolicy.REPLACE, work)
    }

    data class QueuedAction(
        val id: Int,
        val userId: Int,
        val actionType: String,
        val actionData: String,
        val retryCount: Int
    )
}
