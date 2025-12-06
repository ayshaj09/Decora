package com.example.decora

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import org.json.JSONObject

// Constructor takes a callback function 'onUserClick'
class UsersAdapter(
    private val context: Context,
    private val users: List<UserChat>,
    private val onUserClick: (UserChat) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val imgProfile: CircleImageView = itemView.findViewById(R.id.imgProfile)
        val container: View = itemView.findViewById(R.id.dm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.tvUsername.text = user.username

        // PFP Logic
        if (!user.profilePic.isNullOrEmpty()) {
            try {
                val cleanBase64 = if (user.profilePic.contains(",")) user.profilePic.split(",")[1] else user.profilePic
                val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) holder.imgProfile.setImageBitmap(bitmap)
                else holder.imgProfile.setImageResource(R.drawable.defaultpfp)
            } catch (e: Exception) {
                holder.imgProfile.setImageResource(R.drawable.defaultpfp)
            }
        } else {
            holder.imgProfile.setImageResource(R.drawable.defaultpfp)
        }

        // --- UPDATED: Last Message Parsing Logic ---
        if (!user.lastMessage.isNullOrEmpty()) {
            var displayText = user.lastMessage

            // Check if it is a JSON object (Shared Pin)
            if (displayText!!.trim().startsWith("{")) {
                try {
                    val json = JSONObject(displayText)
                    if (json.optString("type") == "share_pin") {
                        val pinTitle = json.optString("title", "Pin")
                        displayText = "Shared a Pin: $pinTitle"
                    }
                } catch (e: Exception) {
                    // Not valid JSON, treat as normal text
                }
            }

            holder.tvLastMessage.text = displayText

            // Use timestamp from DB if available, otherwise "Recent"
            holder.tvTime.text = if (user.timestamp != null) "Now" else ""
        } else {
            holder.tvLastMessage.text = "Tap to chat"
            holder.tvTime.text = ""
        }
        // -------------------------------------------

        // Click Logic
        holder.container.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }
}