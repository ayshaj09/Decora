package com.example.decora

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class UsersAdapter(private val context: Context, private val users: List<UserChat>) :
    RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {
//id dekhni
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val imgProfile: CircleImageView = itemView.findViewById(R.id.imgProfile)
        // Ensure your XML (message_item) has an ID for the root layout (e.g., @+id/dm)
        val container: View = itemView.findViewById(R.id.dm)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.message_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        // 1. Set Username
        holder.tvUsername.text = user.username


        holder.imgProfile.setImageResource(R.drawable.defaultpfp)

        if (!user.profilePic.isNullOrEmpty()) {
            try {

                val base64String = if (user.profilePic.contains(",")) {
                    user.profilePic.split(",")[1]
                } else {
                    user.profilePic
                }

                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                if (bitmap != null) {
                    holder.imgProfile.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                // If decoding fails, the default image is already set
                e.printStackTrace()
            }
        }


        if (!user.lastMessage.isNullOrEmpty()) {
            holder.tvLastMessage.text = user.lastMessage

        } else {
            holder.tvLastMessage.text = "Tap to chat"
            holder.tvTime.text = ""
        }


        holder.container.setOnClickListener {
            val intent = Intent(context, Page19Activity::class.java)


            intent.putExtra("partner_id", user.id)
            intent.putExtra("partner_name", user.username)
            intent.putExtra("partner_pfp", user.profilePic)

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }
}