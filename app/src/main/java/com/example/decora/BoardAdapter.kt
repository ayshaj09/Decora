package com.example.decora

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions

class BoardAdapter(private val context: Context, private val boards: List<Board>) :
    RecyclerView.Adapter<BoardAdapter.BoardViewHolder>() {

    private val requestOptions = RequestOptions()
        .placeholder(R.drawable.rectangle11)
        .error(R.drawable.rectangle11)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .transform(CenterCrop())

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

        val previews = board.previewImages


        fun loadImage(dataString: String?, target: ImageView) {
            target.setImageResource(R.drawable.rectangle11) // Set placeholder initially

            if (dataString.isNullOrEmpty()) {
                Log.d("BoardAdapterDebug", "Image data is null or empty for a slot.")
                return
            }


            val isBase64Data = dataString.startsWith("/9j/") || dataString.length > 500

            if (isBase64Data) {
                // Case 1: Base64 Data (Decode directly)
                Log.d("BoardAdapterDebug", "Loading as Base64 data (length: ${dataString.length}).")
                try {
                    // Ensure the string is clean (removes data:image/jpeg;base64, prefix if present)
                    val cleanBase64 = if (dataString.contains(",")) dataString.split(",")[1] else dataString

                    val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)

                    // Load byte array directly using Glide
                    Glide.with(context)
                        .asBitmap()
                        .load(bytes)
                        .apply(requestOptions)
                        .into(target)

                } catch (e: Exception) {
                    Log.e("BoardAdapterDebug", "Error decoding Base64: ${e.message}")
                    target.setImageResource(R.drawable.rectangle11) // Show placeholder on error
                }

            } else {
                // Case 2: Filename URL (Load from server path)
                // We revert to the simple /uploads/ path as /static/uploads/ may also be wrong.
                val fullUrl = Config.BASE_URL.trimEnd('/') + "/uploads/" + dataString
                Log.d("BoardAdapterDebug", "Loading as URL from: $fullUrl")

                Glide.with(context)
                    .load(fullUrl)
                    .apply(requestOptions)
                    .into(target)
            }
        }

        // 1. Load 1st Image (Large)
        val img1 = if (previews.isNotEmpty()) previews[0] else null
        loadImage(img1, holder.ivLarge)

        // 2. Load 2nd Image (Top Right)
        val img2 = if (previews.size > 1) previews[1] else null
        loadImage(img2, holder.ivSmallTop)

        // 3. Load 3rd Image (Bottom Right)
        val img3 = if (previews.size > 2) previews[2] else null
        loadImage(img3, holder.ivSmallBottom)

        holder.container.setOnClickListener {
            val intent = Intent(context, Page14Activity::class.java)
            intent.putExtra("board_id", board.id)
            intent.putExtra("board_name", board.title)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = boards.size
}