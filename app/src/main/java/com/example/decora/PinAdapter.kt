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

        val bytes = Base64.decode(pin.image, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        holder.img.setImageBitmap(bitmap)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Page8Activity::class.java)

            intent.putExtra("pinTitle", pin.title)
            intent.putExtra("pinImageBase64", pin.image)
            intent.putExtra("username", pin.username)
            intent.putExtra("userPfpBase64", pin.userPfp)

            context.startActivity(intent)
        }
    }

    override fun getItemCount() = pins.size
}
