package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityBookingDetailsBinding
import com.aces.capstone.secureride.model.BookingRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookingDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBookingDetailsBinding
    private lateinit var selectedDate: String
    private lateinit var selectedTimeSlot: String
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bottomNavigationView: BottomNavigationView = binding.dashboardNav
        setupBottomNavigation(bottomNavigationView)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val timeSlotButtons = listOf<Button>(
            findViewById(R.id.timeSlot),
            findViewById(R.id.timeSlot1),
            findViewById(R.id.timeSlot2),
            findViewById(R.id.timeSlot3)
        )

        // Set a date change listener for the CalendarView
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
        }

        timeSlotButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedTimeSlot = "${index + 1}:00pm"
                Toast.makeText(this, "${index + 1}:00pm selected", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnConfirm.setOnClickListener {
            createBookingRequest()
            startActivity(Intent(this@BookingDetails, MapActivity::class.java))
        }
    }

    private fun createBookingRequest() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val pickupLocation = "Sample Pickup Location" // Replace with actual pickup location
            val dropoffLocation = "Sample Dropoff Location" // Replace with actual dropoff location
            val driverId = "driverId" // Replace with the appropriate driver ID

            // Validate essential fields
            if (pickupLocation.isNullOrEmpty() || dropoffLocation.isNullOrEmpty() || driverId.isNullOrEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return
            }

            val type = "sampleType" // Replace with the appropriate type
            val uid = currentUser.uid
            val name = "John"// Make sure user's display name is set correctly

            val bookingRequest = BookingRequest(
                commuterId = currentUser.uid,
                driverId = driverId,
                firstname = name,
                uid = uid,
                type = type,
                pickupLocation = pickupLocation,
                dropoffLocation = dropoffLocation,
                timeSlot = selectedTimeSlot,
                date = selectedDate
            )

            // Store the booking request in Firestore
            db.collection("bookingRequests")
                .add(bookingRequest)
                .addOnSuccessListener { documentReference ->
                    Log.d("BookingDetails", "Booking request sent successfully with ID: ${documentReference.id}")
                    Toast.makeText(this, "Booking request sent", Toast.LENGTH_SHORT).show()

                    // Notify the driver
                    notifyDriver(driverId, documentReference.id)
                }
                .addOnFailureListener { e ->
                    Log.e("BookingDetails", "Error sending booking request: ${e.message}")
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.w("BookingDetails", "User not logged in")
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun notifyDriver(driverId: String, bookingRequestId: String) {
        val driverRef = db.collection("drivers").document(driverId)
        driverRef.update("currentBookingRequestId", bookingRequestId)
            .addOnSuccessListener {
                Log.d("Notification", "Driver notified successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Error notifying driver: ${e.message}")
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