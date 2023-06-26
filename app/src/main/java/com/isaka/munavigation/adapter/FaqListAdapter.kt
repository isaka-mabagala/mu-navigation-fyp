package com.isaka.munavigation.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.isaka.munavigation.databinding.RecyclerviewFaqListBinding
import com.isaka.munavigation.model.Faq

class FaqListAdapter(
    private var faqs: List<Faq>
) : RecyclerView.Adapter<FaqListAdapter.FaqListViewHolder>() {
    private lateinit var context: Context

    inner class FaqListViewHolder(private val itemBinding: RecyclerviewFaqListBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        // binding list data
        @SuppressLint("NotifyDataSetChanged")
        fun bindItem(faq: Faq) {
            itemBinding.tvQuestion.text = faq.question
            itemBinding.tvAnswer.text = faq.answer

            val isVisible: Boolean = faq.visibility
            itemBinding.tvAnswer.visibility = if (isVisible) View.VISIBLE else View.GONE
            itemBinding.ivArrow.rotation = if (isVisible) 180F else 0F

            // set question on click event
            itemBinding.cvQuestion.setOnClickListener {
                faq.visibility = !faq.visibility
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqListViewHolder {
        context = parent.context

        return FaqListViewHolder(
            RecyclerviewFaqListBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: FaqListViewHolder, position: Int) {
        val faq = faqs[position]
        holder.bindItem(faq)
    }

    override fun getItemCount(): Int {
        return faqs.size
    }
}