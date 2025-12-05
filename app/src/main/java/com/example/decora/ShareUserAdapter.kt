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

class ShareUserAdapter(
    private val context: Context,
    private val users: List<UserChat>,
    private val onUserClick: (UserChat) -> Unit
) : RecyclerView.Adapter<ShareUserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val imgProfile: CircleImageView = itemView.findViewById(R.id.imgProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // Inflate the new SIMPLE layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_share_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.tvUsername.text = user.username

        // PFP Logic (Base64)
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

        // Click Logic
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount(): Int = users.size
}