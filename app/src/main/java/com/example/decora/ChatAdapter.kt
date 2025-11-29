package com.example.decora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Updated constructor to accept a click listener
class ChatAdapter(
    private val currentUserId: Int,
    private val onMessageLongClick: (Message) -> Unit // Callback function
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

        if (message.senderId == currentUserId) {
            // SENT MESSAGE
            holder.tvSent.text = message.messageText
            holder.tvSent.visibility = View.VISIBLE
            holder.tvReceived.visibility = View.GONE

            // Add Long Click Listener ONLY for Sent messages
            holder.tvSent.setOnLongClickListener {
                onMessageLongClick(message)
                true // Consumes the long click
            }
        } else {
            // RECEIVED MESSAGE
            holder.tvReceived.text = message.messageText
            holder.tvReceived.visibility = View.VISIBLE
            holder.tvSent.visibility = View.GONE

            // Prevent long clicking received messages (optional)
            holder.tvReceived.setOnLongClickListener(null)
        }
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSent: TextView = itemView.findViewById(R.id.tvSent)
        val tvReceived: TextView = itemView.findViewById(R.id.tvReceived)
    }
}