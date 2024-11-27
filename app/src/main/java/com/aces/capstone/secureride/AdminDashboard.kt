package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityAdminDashboardBinding
import com.aces.capstone.secureride.ui.ListOfCommuterActivity
import com.aces.capstone.secureride.ui.ListOfDriversActivity
import com.aces.capstone.secureride.ui.ListOfRideRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminDashboard : AppCompatActivity() {

    // Declare ViewBinding and Firebase references
    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var firebaseDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Database reference
        firebaseDatabase = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        // Fetch data from Firebase
        fetchDriverCount()
        fetchCommuterCount()
        fetchRideRequestCount()

        // Set OnClickListener for CardView
        binding.cardViewDrivers.setOnClickListener {
            try {
                val intent = Intent(this, ListOfDriversActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("AdminDashboard", "Error on CardView click: ${e.message}")
            }
        }

        binding.cardViewCommuters.setOnClickListener {
            try {
                val intent = Intent(this, ListOfCommuterActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("AdminDashboard", "Error on CardView click: ${e.message}")
            }
        }
        binding.cardViewRides.setOnClickListener {
            try {
                val intent = Intent(this, ListOfRideRequest::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("AdminDashboard", "Error on CardView click: ${e.message}")
            }
        }

    }

    private fun fetchDriverCount() {
        val driversRef = firebaseDatabase.child("user").orderByChild("type").equalTo("Driver")
        driversRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driverCount = snapshot.childrenCount
                binding.driverCountText.text = driverCount.toString()  // Update view binding reference
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Failed to load drivers count: ${error.message}")
            }
        })
    }

    private fun fetchCommuterCount() {
        val commutersRef = firebaseDatabase.child("user").orderByChild("type").equalTo("Commuter")
        commutersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val commuterCount = snapshot.childrenCount
                binding.commuterCountText.text = commuterCount.toString()  // Update view binding reference
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Failed to load commuters count: ${error.message}")
            }
        })
    }

    private fun fetchRideRequestCount() {
        val rideRequestsRef = firebaseDatabase.child("ride_requests")
        rideRequestsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ridesCount = snapshot.childrenCount
                binding.ridesCountText.text = ridesCount.toString()  // Update view binding reference
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminDashboard", "Failed to load ride requests count: ${error.message}")
            }
        })
    }
}
