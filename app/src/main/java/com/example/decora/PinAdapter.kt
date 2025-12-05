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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.Target

// Constructor accepts the list AND the optional delete action
class PinAdapter(
    private val pins: List<Pin>,
    private val onLongClick: ((Pin) -> Unit)? = null
) : RecyclerView.Adapter<PinAdapter.PinViewHolder>() {

    class PinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.pinImage)
        val title: TextView = itemView.findViewById(R.id.pinTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pin, parent, false)
        return PinViewHolder(view)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        val pin = pins[position]

        holder.title.text = pin.title

        // --- PURE BASE64 LOADING LOGIC ---
        try {
            // 1. Clean the string (remove header if exists)
            val cleanBase64 = if (pin.image.contains(",")) {
                pin.image.split(",")[1]
            } else {
                pin.image
            }

            // 2. Decode to Bytes
            val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

            // 3. Load into Glide
            Glide.with(holder.itemView.context)
                .asBitmap()
                .load(imageBytes)
                .placeholder(R.drawable.rectangle11)
                // This makes the image size itself correctly in Staggered Grid
                .override(Target.SIZE_ORIGINAL)
                .transform(RoundedCorners(20))
                .into(holder.img)

        } catch (e: Exception) {
            // If decoding fails, show grey box
            holder.img.setImageResource(R.drawable.rectangle11)
        }
        // ---------------------------------

        // Normal Click: View Details
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Page8Activity::class.java)

            intent.putExtra("pinId", pin.id)
            intent.putExtra("pinTitle", pin.title)
            intent.putExtra("pinImage", pin.image) // Sending Base64 string
            intent.putExtra("pinUser", pin.username)
            intent.putExtra("userPfp", pin.userPfp)

            context.startActivity(intent)
        }

        // Long Click: Custom Action (Delete) or Default (Save)
        holder.itemView.setOnLongClickListener {
            if (onLongClick != null) {
                // Page 14 Logic (Delete)
                onLongClick.invoke(pin)
            } else {
                // Page 7 Logic (Save to Board)
                val context = holder.itemView.context
                val intent = Intent(context, Page9Activity::class.java)
                intent.putExtra("pin_id_to_save", pin.id)
                context.startActivity(intent)
            }
            true
        }
    }

    override fun getItemCount() = pins.size
}