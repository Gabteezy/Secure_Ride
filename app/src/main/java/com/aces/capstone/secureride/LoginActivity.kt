package com.aces.capstone.secureride


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.UserRegister
import com.aces.capstone.secureride.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val intent = Intent(this@LoginActivity, Map::class.java)
            startActivity(intent)
        }

        binding.btnRegister.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.btnRegister.setBackgroundColor(Color.RED)
                    true
                }
                MotionEvent.ACTION_UP -> {

                    val intent = Intent(this@LoginActivity, UserRegister::class.java)
                    startActivity(intent)


                    binding.btnRegister.setBackgroundColor(Color.GRAY)

                    true
                }
                else -> false
            }
        }
    }

    private fun showToast(message: String) {
        // Implement your showToast logic here
    }
}
