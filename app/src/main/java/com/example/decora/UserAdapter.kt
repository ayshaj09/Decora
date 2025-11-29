package com.example.decora

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class UserAdapter(private var userList: ArrayList<User>) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // 1. Create the View (Inflate user_item.xml)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UserViewHolder(itemView)
    }

    // 2. Bind Data (Put Name and Image into the View)
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]

        holder.tvName.text = currentUser.name

        // Reset image to default first
        holder.imgProfile.setImageResource(R.drawable.pfp2)

        // Decode Base64 Image if it exists
        if (currentUser.pfp.isNotEmpty()) {
            val bitmap = decodeBase64(currentUser.pfp)
            if (bitmap != null) {
                holder.imgProfile.setImageBitmap(bitmap)
            }
        }
    }

    // 3. Count how many items to display
    override fun getItemCount(): Int {
        return userList.size
    }

    // 4. Helper to Update List (For Search Filter)
    fun updateList(newList: List<User>) {
        userList = ArrayList(newList)
        notifyDataSetChanged() // Refreshes the RecyclerView
    }

    // 5. Base64 Decoder
    private fun decodeBase64(input: String): Bitmap? {
        return try {
            val decodedByte = Base64.decode(input, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.size)
        } catch (e: Exception) {
            null
        }
    }

    // 6. ViewHolder Class (Finds IDs)
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val imgProfile: CircleImageView = itemView.findViewById(R.id.imgProfile)
    }
}