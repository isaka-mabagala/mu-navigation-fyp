package com.isaka.munavigation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.isaka.munavigation.databinding.RecyclerviewAutocompleteLocationListBinding
import com.isaka.munavigation.model.Location

class AutocompleteLocationListAdapter(
    private var locations: List<Location>, private val listener: OnItemClickListener
) : RecyclerView.Adapter<AutocompleteLocationListAdapter.AutocompleteLocationListViewHolder>() {
    private lateinit var context: Context

    inner class AutocompleteLocationListViewHolder(private val itemBinding: RecyclerviewAutocompleteLocationListBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

        // binding list data
        fun bindItem(location: Location) {
            itemBinding.tvLocationTitle.text = location.title
            itemBinding.tvLocationDescription.text = location.description
        }

        init {
            // set item on click event
            itemBinding.root.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position: Int = adapterPosition

            if (position != RecyclerView.NO_POSITION) {
                val location = locations[position]
                val id = location.id
                listener.onItemClick(id)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AutocompleteLocationListViewHolder {
        context = parent.context

        return AutocompleteLocationListViewHolder(
            RecyclerviewAutocompleteLocationListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: AutocompleteLocationListViewHolder, position: Int) {
        val location = locations[position]
        holder.bindItem(location)
    }

    override fun getItemCount(): Int {
        return locations.size
    }

    interface OnItemClickListener {
        fun onItemClick(locationId: String)
    }

    // filtering recyclerview items
    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filterList: List<Location>) {
        locations = filterList
        notifyDataSetChanged()
    }
}