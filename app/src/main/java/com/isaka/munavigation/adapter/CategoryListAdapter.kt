package com.isaka.munavigation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.isaka.munavigation.databinding.RecyclerviewCategoryListBinding
import com.isaka.munavigation.model.Category

class CategoryListAdapter(
    private var categories: List<Category>, private val listener: OnItemClickListener
) : RecyclerView.Adapter<CategoryListAdapter.CategoryListViewHolder>() {
    private lateinit var context: Context

    inner class CategoryListViewHolder(private val itemBinding: RecyclerviewCategoryListBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

        // binding list data
        fun bindItem(category: Category) {
            itemBinding.tvCategoryName.text = category.name
        }

        init {
            // set item on click event
            itemBinding.root.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position: Int = adapterPosition

            if (position != RecyclerView.NO_POSITION) {
                val category = categories[position]
                val alias = category.alias
                listener.onItemClick(alias)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryListViewHolder {
        context = parent.context

        return CategoryListViewHolder(
            RecyclerviewCategoryListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: CategoryListViewHolder, position: Int) {
        val category = categories[position]
        holder.bindItem(category)
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    interface OnItemClickListener {
        fun onItemClick(alias: String)
    }

    // filtering recyclerview items
    @SuppressLint("NotifyDataSetChanged")
    fun filterList(filterList: List<Category>) {
        categories = filterList
        notifyDataSetChanged()
    }
}