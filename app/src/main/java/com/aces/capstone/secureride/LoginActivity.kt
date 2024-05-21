package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    // Declare the binding variable
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the binding variable
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Access the button via binding
        binding.btnStarted.setOnClickListener {
            startActivity(Intent(this@LoginActivity, LoginUser::class.java))
        }
    }
}