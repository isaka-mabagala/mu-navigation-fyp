package com.isaka.munavigation.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.isaka.munavigation.R
import com.isaka.munavigation.databinding.ActivitySettingsBinding
import com.isaka.munavigation.util.Constants.PRIVACY_URL

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // toolbar setup
        setupActionBar()

        // redirect to privacy webpage
        binding.settingTermsConditions.setOnClickListener {
            val viewIntent = Intent("android.intent.action.VIEW", Uri.parse(PRIVACY_URL))
            startActivity(viewIntent)
        }

        // sharing app
        binding.settingShare.setOnClickListener {
            val shareIntent = Intent().apply {
                this.action = Intent.ACTION_SEND
                this.type = "text/plain"
                this.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_share_text))
            }
            startActivity(shareIntent)
        }

        // move to contact activity
        binding.settingContact.setOnClickListener {
            val intent = Intent(this, ContactActivity::class.java)
            startActivity(intent)
        }

        // move to about activity
        binding.settingAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }

        // move to FAQ activity
        binding.settingFaq.setOnClickListener {
            val intent = Intent(this, FaqActivity::class.java)
            startActivity(intent)
        }
    }

    // setup action bar
    private fun setupActionBar() {
        setSupportActionBar(binding.tbSettings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = TOOL_BAR_TITLE

        binding.tbSettings.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        const val TOOL_BAR_TITLE = "Settings"
    }
}