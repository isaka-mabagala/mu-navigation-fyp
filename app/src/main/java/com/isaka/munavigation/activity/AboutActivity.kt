package com.isaka.munavigation.activity

import android.annotation.SuppressLint
import android.os.Bundle
import com.isaka.munavigation.BuildConfig
import com.isaka.munavigation.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // set app version
        val appVersion = BuildConfig.VERSION_NAME
        binding.tvAppVersion.text = "Version $appVersion"

        // toolbar setup
        setupActionBar()
    }

    // setup action bar
    private fun setupActionBar() {
        setSupportActionBar(binding.tbAbout)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = TOOL_BAR_TITLE

        binding.tbAbout.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        const val TOOL_BAR_TITLE = "About"
    }
}