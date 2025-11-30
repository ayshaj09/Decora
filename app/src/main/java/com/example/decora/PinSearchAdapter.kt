package com.example.decora

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PinSearchAdapter(private var titles: List<String>) :
    RecyclerView.Adapter<PinSearchAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvSuggestionTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val title = titles[position]
        holder.tvTitle.text = title

        // ✅ UPDATED: Click logic to open the Result Page
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context

            // 1. Create the Intent
            val intent = Intent(context, ResultPageActivity::class.java)

            // 2. Pass the title (e.g., "Sofa") so the next page knows what to fetch
            intent.putExtra("EXTRA_QUERY", title)

            // 3. Start the activity
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return titles.size
    }

    // Function to update data when filtering
    fun updateList(newTitles: List<String>) {
        titles = newTitles
        notifyDataSetChanged()
    }
}