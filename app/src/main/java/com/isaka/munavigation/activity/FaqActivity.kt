package com.isaka.munavigation.activity

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.isaka.munavigation.adapter.FaqListAdapter
import com.isaka.munavigation.databinding.ActivityFaqBinding
import com.isaka.munavigation.model.Faq

class FaqActivity : BaseActivity() {
    private val context = this
    private lateinit var binding: ActivityFaqBinding
    private lateinit var adapter: FaqListAdapter
    private var faqList: MutableList<Faq> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // toolbar setup
        setupActionBar()

        // binding faq list data
        populateFaqListToUi()
    }

    private fun populateFaqListToUi() {
        faqList = getFaqList() as MutableList<Faq>

        // set faq to adapter
        if (faqList.isNotEmpty()) {
            adapter = FaqListAdapter(faqList)
            binding.rvFaqList.adapter = adapter
            binding.rvFaqList.layoutManager = LinearLayoutManager(context)
            binding.rvFaqList.setHasFixedSize(true)
        }
    }

    // setup action bar
    private fun setupActionBar() {
        setSupportActionBar(binding.tbFaq)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = TOOL_BAR_TITLE

        binding.tbFaq.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        const val TOOL_BAR_TITLE = "FAQs"
    }
}