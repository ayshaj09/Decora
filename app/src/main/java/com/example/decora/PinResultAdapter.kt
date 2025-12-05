package com.example.decora

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PinResultAdapter(private val pinList: List<Pin>) :
    RecyclerView.Adapter<PinResultAdapter.PinViewHolder>() {

    class PinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.pinImage)
        val title: TextView = itemView.findViewById(R.id.pinTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pin_result, parent, false)
        return PinViewHolder(view)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        val pin = pinList[position]

        holder.title.text = pin.title

        // Decode Image
        if (pin.image.isNotEmpty()) {
            try {
                // Handle base64 strings that might have the data prefix
                val cleanBase64 = if (pin.image.contains(","))
                    pin.image.split(",")[1] else pin.image

                val decodedString = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                if (bitmap != null) {
                    holder.image.setImageBitmap(bitmap)
                } else {
                    holder.image.setImageResource(R.drawable.img1) // Fallback if decoding fails
                }
            } catch (e: Exception) {
                holder.image.setImageResource(R.drawable.img1) // Fallback on error
            }
        } else {
            holder.image.setImageResource(R.drawable.img1) // Fallback if empty
        }

        // --- NEW CLICK LOGIC ---
        holder.itemView.setOnClickListener {
            // 1. Create intent to go to your partner's Detail Page (Page8Activity)
            val intent = Intent(holder.itemView.context, Page8Activity::class.java)

            // 2. Pass the ID. Your partner's code specifically looks for "pinId"
            intent.putExtra("pinId", pin.id)

            // 3. Optional: Hide the delete button since we are searching, not managing our own pins
            intent.putExtra("opened_from_pins_page", false)

            // 4. Start the activity
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return pinList.size
    }
}