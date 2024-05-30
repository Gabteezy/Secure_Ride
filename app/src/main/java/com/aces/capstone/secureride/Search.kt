package com.aces.capstone.secureride

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.aces.capstone.secureride.databinding.ActivitySearchBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class Search : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root) // Use binding.root to set the content view

        val bottomNavigationView: BottomNavigationView = binding.dashboardNav

        setupBottomNavigation(bottomNavigationView)

        binding.confirmBooking.setOnClickListener {
            startActivity(Intent(this@Search, BookingDetails::class.java))
        }
    }

    private fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCustomerHome -> {
                    startActivity(Intent(this@Search, UserDashboard::class.java))
                    true
                }
                R.id.navCustomerFindRide -> {
                    true
                }
                R.id.navCustomerMyRides -> {
                    // Navigate to Map activity
                    startActivity(Intent(this@Search, BookingDetails::class.java))
                    true
                }
                R.id.navCustomerProfile -> {
                    // Navigate to Map activity
                    startActivity(Intent(this@Search, Profile::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.navCustomerFindRide
    }
}
