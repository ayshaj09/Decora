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


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false)
        return UserViewHolder(itemView)
    }


    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]

        holder.tvName.text = currentUser.name

        // Reset image
        holder.imgProfile.setImageResource(R.drawable.defaultpfp)

        // Set Image
        if (currentUser.pfp.isNotEmpty()) {
            val bitmap = decodeBase64(currentUser.pfp)
            if (bitmap != null) {
                holder.imgProfile.setImageBitmap(bitmap)
            }
        }


        holder.itemView.setOnClickListener {

            val intent = android.content.Intent(holder.itemView.context, otherUserProfile::class.java)

            intent.putExtra("EXTRA_ID", currentUser.id)
            intent.putExtra("EXTRA_NAME", currentUser.name)
            intent.putExtra("EXTRA_PFP", currentUser.pfp)


            holder.itemView.context.startActivity(intent)
        }
    }


    override fun getItemCount(): Int {
        return userList.size
    }


    fun updateList(newList: List<User>) {
        userList = ArrayList(newList)
        notifyDataSetChanged()
    }


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