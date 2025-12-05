package com.example.decora

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

// FIX: Added onMessageLongClick to constructor
class ChatAdapter(
    private val currentUserId: Int,
    private val onMessageLongClick: (Message) -> Unit
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val messages = ArrayList<Message>()

    fun setMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        // Helper to parse if message is JSON pin share
        var isPinShare = false
        var pinTitle = ""
        var pinImage = ""

        try {
            if (message.messageText.trim().startsWith("{")) {
                val json = JSONObject(message.messageText)
                if (json.optString("type") == "share_pin") {
                    isPinShare = true
                    pinTitle = json.optString("title")
                    pinImage = json.optString("image")
                }
            }
        } catch (e: Exception) { isPinShare = false }

        // Logic: Show Sent or Received Layout
        if (message.senderId == currentUserId) {
            // MY MESSAGE (Right)
            holder.layoutSent.visibility = View.VISIBLE
            holder.layoutReceived.visibility = View.GONE

            // Setup Long Click for Edit/Delete (Only on my messages)
            holder.layoutSent.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }

            if (isPinShare) {
                holder.tvSent.visibility = View.GONE
                holder.cardSent.visibility = View.VISIBLE
                holder.titleSentPin.text = pinTitle
                loadImage(pinImage, holder.imgSentPin)
            } else {
                holder.tvSent.visibility = View.VISIBLE
                holder.cardSent.visibility = View.GONE
                holder.tvSent.text = message.messageText
            }
        } else {
            // RECEIVED MESSAGE (Left)
            holder.layoutReceived.visibility = View.VISIBLE
            holder.layoutSent.visibility = View.GONE

            // No long click for received messages usually
            holder.layoutReceived.setOnLongClickListener(null)

            if (isPinShare) {
                holder.tvReceived.visibility = View.GONE
                holder.cardReceived.visibility = View.VISIBLE
                holder.titleReceivedPin.text = pinTitle
                loadImage(pinImage, holder.imgReceivedPin)
            } else {
                holder.tvReceived.visibility = View.VISIBLE
                holder.cardReceived.visibility = View.GONE
                holder.tvReceived.text = message.messageText
            }
        }
    }

    private fun loadImage(base64String: String, imageView: ImageView) {
        if (base64String.isNotEmpty()) {
            try {
                val clean = if (base64String.contains(",")) base64String.split(",")[1] else base64String
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.rectangle11)
            }
        } else {
            imageView.setImageResource(R.drawable.rectangle11)
        }
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Layout Containers
        val layoutReceived: LinearLayout = itemView.findViewById(R.id.layoutReceived)
        val layoutSent: LinearLayout = itemView.findViewById(R.id.layoutSent)

        // Text Views
        val tvSent: TextView = itemView.findViewById(R.id.tvSent)
        val tvReceived: TextView = itemView.findViewById(R.id.tvReceived)

        // Pin Cards
        val cardSent: CardView = itemView.findViewById(R.id.cardSent)
        val cardReceived: CardView = itemView.findViewById(R.id.cardReceived)

        // Pin Contents
        val imgSentPin: ImageView = itemView.findViewById(R.id.imgSentPin)
        val titleSentPin: TextView = itemView.findViewById(R.id.titleSentPin)
        val imgReceivedPin: ImageView = itemView.findViewById(R.id.imgReceivedPin)
        val titleReceivedPin: TextView = itemView.findViewById(R.id.titleReceivedPin)
    }
}