package com.aces.capstone.secureride

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSubmit.setOnClickListener {
            val intent = Intent(this@LoginActivity, UserDashboard::class.java)
            startActivity(intent)
        }

        btnRegister = binding.btnRegister
        btnRegister.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    btnRegister.setBackgroundColor(Color.RED)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    btnRegister.setBackgroundColor(Color.GRAY)
                    val intent = Intent(this@LoginActivity, UserRegister::class.java)
                    startActivity(intent)
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    btnRegister.setBackgroundColor(Color.GRAY) // Reset color on cancel
                    true
                }

                else -> false
            }
        }}


        private fun showToast(message: String) {
        // Implement your showToast logic here
    }
}