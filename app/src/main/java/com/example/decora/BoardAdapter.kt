package com.example.decora

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BoardAdapter(private val context: Context, private val boards: List<Board>) :
    RecyclerView.Adapter<BoardAdapter.BoardViewHolder>() {

    class BoardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.boardTitle)
        val count: TextView = itemView.findViewById(R.id.pinCount)
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

        holder.container.setOnClickListener {
            val intent = Intent(context, Page14Activity::class.java)
            intent.putExtra("board_id", board.id)
            intent.putExtra("board_name", board.title)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = boards.size
}