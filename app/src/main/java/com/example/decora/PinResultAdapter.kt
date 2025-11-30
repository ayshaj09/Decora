package com.example.decora

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 1. We use the existing "Pin" class here
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
        // (We access 'pin.image' because your existing class calls it 'image', not 'imageBase64')
        if (pin.image.isNotEmpty()) {
            try {
                val cleanBase64 = if (pin.image.contains(","))
                    pin.image.split(",")[1] else pin.image

                val decodedString = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                holder.image.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.image.setImageResource(R.drawable.img1) // Fallback
            }
        }
    }

    override fun getItemCount(): Int {
        return pinList.size
    }
}