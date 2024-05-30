package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityBookingDetailsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BookingDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBookingDetailsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var selectedDate: String? = null
    private var selectedTimeSlot: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Use binding.root to set the content view

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val bottomNavigationView: BottomNavigationView = binding.dashboardNav
        setupBottomNavigation(bottomNavigationView)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val timeSlotButton = findViewById<Button>(R.id.timeSlot)
        val timeSlotButton1 = findViewById<Button>(R.id.timeSlot1)
        val timeSlotButton2 = findViewById<Button>(R.id.timeSlot2)
        val timeSlotButton3 = findViewById<Button>(R.id.timeSlot3)

        // Set a date change listener for the CalendarView
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$year-${month + 1}-$dayOfMonth"
        }

        timeSlotButton.setOnClickListener {
            selectedTimeSlot = "1:00pm"
            Toast.makeText(this, "1:00pm selected", Toast.LENGTH_SHORT).show()
        }

        timeSlotButton1.setOnClickListener {
            selectedTimeSlot = "2:00pm"
            Toast.makeText(this, "2:00pm selected", Toast.LENGTH_SHORT).show()
        }

        timeSlotButton2.setOnClickListener {
            selectedTimeSlot = "3:00pm"
            Toast.makeText(this, "3:00pm selected", Toast.LENGTH_SHORT).show()
        }

        timeSlotButton3.setOnClickListener {
            selectedTimeSlot = "4:00pm"
            Toast.makeText(this, "4:00pm selected", Toast.LENGTH_SHORT).show()
        }

        binding.btnConfirm.setOnClickListener {
            createBooking()
        }
    }

    private fun createBooking() {
        val commuterId = auth.currentUser?.uid
        val bookingId = database.reference.child("bookings").push().key

        if (commuterId != null && bookingId != null && selectedDate != null && selectedTimeSlot != null) {
            val bookingDetails = mapOf(
                "commuterId" to commuterId,
                "driverId" to null,
                "details" to mapOf(
                    "destination" to "Destination Address",
                    "pickup" to "Pickup Address",
                    "time" to selectedTimeSlot,
                    "date" to selectedDate
                ),
                "status" to "requested"
            )

            database.reference.child("bookings").child(bookingId).setValue(bookingDetails).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Booking created successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@BookingDetails, MapActivity::class.java))
                } else {
                    Toast.makeText(this, "Failed to create booking", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Please select a date and time slot", Toast.LENGTH_SHORT).show()
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
                R.id.navCustomerProfile -> {
                    startActivity(Intent(this@BookingDetails, Profile::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.navCustomerMyRides
    }
}
