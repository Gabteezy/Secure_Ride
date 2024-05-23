package com.aces.capstone.secureride

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import com.aces.capstone.secureride.databinding.ActivityUserDashboardBinding

class UserDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityUserDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNavigationView: BottomNavigationView = binding.dashboardNav

        setupBottomNavigation(bottomNavigationView)

        binding.book.setOnClickListener {
            startActivity(Intent(this@UserDashboard, Search::class.java))
        }
        binding.book1.setOnClickListener {
            startActivity(Intent(this@UserDashboard, Search::class.java))
        }
    }

    private fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCustomerHome -> {
                    // Home is already selected, no action needed
                    true
                }
                R.id.navCustomerFindRide -> {
                    // Navigate to SearchActivity
                    startActivity(Intent(this@UserDashboard, Search::class.java))
                    true
                }
                R.id.navCustomerMyRides -> {
                    // Navigate to Map activity
                    startActivity(Intent(this@UserDashboard, Map::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
