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


        if (!user.profilePic.isNullOrEmpty()) {
            try {
                // 1. Clean the string if it has the "data:image..." prefix
                val base64String = if (user.profilePic.contains(",")) {
                    user.profilePic.split(",")[1]
                } else {
                    user.profilePic
                }

                // 2. Decode String to Bytes
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)

                // 3. Convert Bytes to Bitmap
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                // 4. Set Image
                if (bitmap != null) {
                    holder.imgProfile.setImageBitmap(bitmap)
                } else {
                    holder.imgProfile.setImageResource(R.drawable.defaultpfp)
                }
            } catch (e: Exception) {

                holder.imgProfile.setImageResource(R.drawable.defaultpfp)
            }
        } else {

            holder.imgProfile.setImageResource(R.drawable.defaultpfp)
        }



        if (!user.lastMessage.isNullOrEmpty()) {
            holder.tvLastMessage.text = user.lastMessage
            holder.tvTime.text = "Now"
        } else {
            holder.tvLastMessage.text = "Tap to chat"
            holder.tvTime.text = ""
        }


        holder.container.setOnClickListener {

            val intent = Intent(context, Page19Activity::class.java)
            intent.putExtra("partner_id", user.id)
            intent.putExtra("partner_name", user.username)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }
}