package com.aces.capstone.secureride

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aces.capstone.secureride.databinding.ActivityAdminDashboardBinding
import com.aces.capstone.secureride.databinding.ActivityLoginBinding

class AdminDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Access the button via binding
        binding.logout.setOnClickListener {
            startActivity(Intent(this@AdminDashboard, LogoutPrompt::class.java))
        }
    }
    }