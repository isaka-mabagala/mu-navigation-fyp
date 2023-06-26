package com.isaka.munavigation.activity

import android.os.Bundle
import com.isaka.munavigation.databinding.ActivityContactBinding

class ContactActivity : BaseActivity() {
    private lateinit var binding: ActivityContactBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // toolbar setup
        setupActionBar()
    }

    // setup action bar
    private fun setupActionBar() {
        setSupportActionBar(binding.tbContact)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = TOOL_BAR_TITLE

        binding.tbContact.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        const val TOOL_BAR_TITLE = "Contact Us"
    }
}