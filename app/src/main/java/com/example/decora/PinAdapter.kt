package com.example.decora

import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Base64
import com.bumptech.glide.Glide


class PinAdapter(private val pins: List<Pin>) :
    RecyclerView.Adapter<PinAdapter.PinViewHolder>() {

    class PinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img = itemView.findViewById<ImageView>(R.id.pinImage)
        val title = itemView.findViewById<TextView>(R.id.pinTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pin, parent, false)
        return PinViewHolder(view)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        val pin = pins[position]

        holder.title.text = pin.title
        val base64Image = "data:image/*;base64,${pin.image}"
        Glide.with(holder.itemView.context)
            .load(base64Image)
            .placeholder(R.drawable.placeholder_image)   // Add placeholder
            .skipMemoryCache(false)
            .into(holder.img)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Page8Activity::class.java)

            intent.putExtra("pinTitle", pin.title)
            intent.putExtra("pinId", pin.id)

            intent.putExtra("userPfpBase64", pin.userPfp)

            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Page9Activity::class.java)
            intent.putExtra("pin_id_to_save", pin.id) // Pass the ID we want to save
            context.startActivity(intent)
            true // Return true to indicate the click was handled
        }
    }

    override fun getItemCount() = pins.size
}