package com.example.decora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SectionsAdapter(private val sections: List<SectionModel>) :
    RecyclerView.Adapter<SectionsAdapter.SectionViewHolder>() {

    inner class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.sectionTitle)
        val imagesRecycler: RecyclerView = view.findViewById(R.id.imagesRecycler)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]

        // Set title from DB
        holder.title.text = section.title

        // Horizontal RecyclerView
        holder.imagesRecycler.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)

        holder.imagesRecycler.adapter = ImagesAdapter(section.images)
    }

    override fun getItemCount(): Int = sections.size
}
