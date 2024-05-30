package com.aces.capstone.secureride

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.aces.capstone.secureride.databinding.ActivityPrivacyBinding

class Privacy : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set click listener on the TextView
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }
    }
}
