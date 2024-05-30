package com.aces.capstone.secureride

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aces.capstone.secureride.databinding.ActivityLogoutPromptBinding

class LogoutPrompt : AppCompatActivity() {
    private lateinit var binding: ActivityLogoutPromptBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogoutPromptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if binding is not null
        binding.btnConfirmLogout.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.btnCancel.setOnClickListener {
            startActivity(Intent(this, Profile::class.java))
        }
    }
}
