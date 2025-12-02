package com.example.decora

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop

class BoardAdapter(private val context: Context, private val boards: List<Board>) :
    RecyclerView.Adapter<BoardAdapter.BoardViewHolder>() {

    class BoardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.boardTitle)
        val count: TextView = itemView.findViewById(R.id.pinCount)
        val ivLarge: ImageView = itemView.findViewById(R.id.ivLarge)
        val ivSmallTop: ImageView = itemView.findViewById(R.id.ivSmallTop)
        val ivSmallBottom: ImageView = itemView.findViewById(R.id.ivSmallBottom)
        val container: View = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_board, parent, false)
        return BoardViewHolder(view)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val board = boards[position]

        holder.title.text = board.title
        holder.count.text = board.pinCount

        // --- PREVIEW IMAGE LOGIC ---
        val previews = board.previewImages

        // Helper function to load image or show placeholder
        fun loadImage(url: String?, target: ImageView) {
            if (!url.isNullOrEmpty()) {
                val fullUrl = Config.BASE_URL + "uploads/" + url
                Glide.with(context)
                    .load(fullUrl)
                    .transform(CenterCrop())
                    .placeholder(R.drawable.greybox) // Grey box
                    .error(R.drawable.greybox)
                    .into(target)
            } else {
                // If no image, ensure it shows the grey box
                target.setImageResource(R.drawable.greybox)
            }
        }

        // Load 1st Image (Large)
        val img1 = if (previews.isNotEmpty()) previews[0] else null
        loadImage(img1, holder.ivLarge)

        // Load 2nd Image (Top Right)
        val img2 = if (previews.size > 1) previews[1] else null
        loadImage(img2, holder.ivSmallTop)

        // Load 3rd Image (Bottom Right)
        val img3 = if (previews.size > 2) previews[2] else null
        loadImage(img3, holder.ivSmallBottom)
        // ---------------------------

        holder.container.setOnClickListener {
            val intent = Intent(context, Page14Activity::class.java)
            intent.putExtra("board_id", board.id)
            intent.putExtra("board_name", board.title)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = boards.size
}