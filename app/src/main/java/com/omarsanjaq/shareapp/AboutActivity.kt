package com.omarsanjaq.shareapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    private lateinit var developerLayout: LinearLayout
    private lateinit var developerName: TextView
    private lateinit var developerImage: ImageView

    private val TELEGRAM_URL = "https://t.me/Om9r0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        developerLayout = findViewById(R.id.developerLayout)
        developerName = findViewById(R.id.developerName)
        developerImage = findViewById(R.id.developerImage)

        // يمكنك لاحقاً تحميل صورة من URL أو من الموارد
        // باستخدام Glide أو أي مكتبة أخرى
    }

    private fun setupListeners() {
        developerLayout.setOnClickListener {
            openTelegram()
        }

        developerName.setOnClickListener {
            openTelegram()
        }
    }

    private fun openTelegram() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_URL))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
