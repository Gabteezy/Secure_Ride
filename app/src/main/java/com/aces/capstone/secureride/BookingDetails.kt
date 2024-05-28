package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.CalendarView
import android.widget.Toast
import com.aces.capstone.secureride.databinding.ActivityBookingDetailsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class BookingDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBookingDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Use binding.root to set the content view

        val bottomNavigationView: BottomNavigationView = binding.dashboardNav

        setupBottomNavigation(bottomNavigationView)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val timeSlotButton = findViewById<Button>(R.id.timeSlot)
        val timeSlotButton1 = findViewById<Button>(R.id.timeSlot1)
        val timeSlotButton2 = findViewById<Button>(R.id.timeSlot2)
        val timeSlotButton3 = findViewById<Button>(R.id.timeSlot3)

        // Set a date change listener for the CalendarView
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Your date change logic here
        }

        timeSlotButton.setOnClickListener {
            Toast.makeText(this, "1:00pm selected", Toast.LENGTH_SHORT).show()
        }

        timeSlotButton1.setOnClickListener {
            Toast.makeText(this, "2:00pm selected", Toast.LENGTH_SHORT).show()
        }

        timeSlotButton2.setOnClickListener {
            Toast.makeText(this, "3:00pm selected", Toast.LENGTH_SHORT).show()
        }

        timeSlotButton3.setOnClickListener {
            Toast.makeText(this, "4:00pm selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnConfirm.setOnClickListener {
            startActivity(Intent(this@BookingDetails, MapActivity::class.java))
        }
    }

    private fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCustomerHome -> {
                    startActivity(Intent(this@BookingDetails, UserDashboard::class.java))
                    true
                }
                R.id.navCustomerFindRide -> {
                    // Navigate to SearchActivity
                    startActivity(Intent(this@BookingDetails, Search::class.java))
                    true
                }
                R.id.navCustomerMyRides -> {
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.navCustomerMyRides
    }
}