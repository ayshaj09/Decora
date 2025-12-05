package com.example.decora

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SyncWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {

    override fun doWork(): Result {
        Log.d("SYNC_WORKER", "Worker started")

        val pendingActions = OfflineQueueManager.getPending(applicationContext)
        Log.d("SYNC_WORKER", "Pending actions: ${pendingActions.size}")

        var allSuccessful = true

        for (action in pendingActions) {
            when (action.actionType) {
                "upload_pin" -> {
                    Log.d("SYNC_WORKER", "Uploading pending pin ID=${action.id}")
                    val success = uploadPin(action)

                    if (success) {
                        Log.d("SYNC_WORKER", "Upload success for ID=${action.id}")
                        OfflineQueueManager.markCompleted(applicationContext, action.id)
                    } else {
                        Log.d("SYNC_WORKER", "Upload FAILED for ID=${action.id}")
                        OfflineQueueManager.markFailed(applicationContext, action.id)
                        allSuccessful = false
                    }
                }
            }
        }

        return if (allSuccessful) {
            Log.d("SYNC_WORKER", "All uploads successful. Worker finished.")
            Result.success()
        } else {
            Log.d("SYNC_WORKER", "Some uploads failed → retrying later.")
            Result.retry()
        }
    }

    private fun uploadPin(action: OfflineQueueManager.QueuedAction): Boolean {
        return try {
            val data = JSONObject(action.actionData)

            val url = URL(Config.BASE_URL + "upload_pin.php")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(data.toString())
            writer.flush()
            writer.close()

            val response = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(response)

            obj.optBoolean("success", false)

        } catch (e: Exception) {
            Log.e("SYNC_WORKER", "Error uploading pin ID=${action.id}: ${e.message}")
            false
        }
    }
}
