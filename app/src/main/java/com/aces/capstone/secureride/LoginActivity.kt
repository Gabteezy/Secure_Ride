package com.aces.capstone.secureride

import android.content.Context
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

        // Check if user is already logged in
        val sharedPreferences = getSharedPreferences("LoginSession", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val userType = sharedPreferences.getString("userType", "")

        // If the user is already logged in, skip login and go directly to the dashboard
        if (isLoggedIn && !userType.isNullOrEmpty()) {
            navigateToDashboard(userType)
        } else {
            // If not logged in, proceed with login or sign up
            setupButtonClickListeners()
        }
    }

    private fun setupButtonClickListeners() {
        // Access the button via binding
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this@LoginActivity, LoginUser::class.java))
        }

        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterAs::class.java))
        }
    }

    private fun navigateToDashboard(userType: String) {
        // Redirect to the appropriate dashboard based on user type
        val intent = when (userType) {
            "Driver" -> Intent(this, DriverDashboard::class.java)
            "Commuter" -> Intent(this, UserDashboard::class.java)
            "Admin" -> Intent(this, AdminDashboard::class.java)
            else -> Intent(this, LoginUser::class.java) // Default to login if something went wrong
        }
        startActivity(intent)
        finish() // Close the login activity
    }
}