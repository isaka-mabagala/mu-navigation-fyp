package com.isaka.munavigation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.isaka.munavigation.activity.BaseActivity
import com.isaka.munavigation.databinding.RecyclerviewLocationListBinding
import com.isaka.munavigation.model.Location
import com.isaka.munavigation.util.Mapbox.locationDistanceConverter

class LocationListAdapter(
    private var locations: List<Location>, private val listener: OnItemClickListener
) : RecyclerView.Adapter<LocationListAdapter.LocationListViewHolder>() {
    private lateinit var context: Context

    inner class LocationListViewHolder(private val itemBinding: RecyclerviewLocationListBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

        // binding list data
        fun bindItem(location: Location) {
            itemBinding.tvLocationTitle.text = location.title
            itemBinding.tvLocationDescription.text = location.description
            itemBinding.ivLocationIcon.setImageResource(getLocationIcon(location.category))
            itemBinding.tvLocationDistance.text = locationDistanceConverter(location.distance!!)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationListViewHolder {
        context = parent.context

        return LocationListViewHolder(
            RecyclerviewLocationListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: LocationListViewHolder, position: Int) {
        val location = locations[position]
        holder.bindItem(location)
    }

    override fun getItemCount(): Int {
        return locations.size
    }

    interface OnItemClickListener {
        fun onItemClick(locationId: String)
    }

    // get location icon by category
    private fun getLocationIcon(category: String): Int {
        val locationCategory = BaseActivity().getCategoryByAlias(category, context)
        return BaseActivity().drawableResourceId(locationCategory.icon, context)
    }

    // filtering recyclerview items
    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filterList: List<Location>) {
        locations = filterList
        notifyDataSetChanged()
    }
}