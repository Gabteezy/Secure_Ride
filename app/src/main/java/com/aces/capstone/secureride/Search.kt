package com.aces.capstone.secureride

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aces.capstone.secureride.databinding.ActivitySearchBinding

class Search : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_search)

        binding.confirmBooking.setOnClickListener {
            startActivity(Intent(this@Search, BookingDetails::class.java))
        }
        binding.confirmBooking1.setOnClickListener {
            startActivity(Intent(this@Search, BookingDetails::class.java))
        }
        binding.confirmBooking2.setOnClickListener {
            startActivity(Intent(this@Search, BookingDetails::class.java))
        }
    }
}