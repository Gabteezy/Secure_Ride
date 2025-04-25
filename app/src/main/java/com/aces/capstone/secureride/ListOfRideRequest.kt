package com.aces.capstone.secureride.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.adapter.RideAdapter
import com.aces.capstone.secureride.databinding.ActivityListOfRideBinding
import com.aces.capstone.secureride.model.RideRequest
import com.google.firebase.database.*

class ListOfRideRequest : AppCompatActivity() {

    private lateinit var binding: ActivityListOfRideBinding
    private lateinit var rideAdapter: RideAdapter
    private val rideList = mutableListOf<RideRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListOfRideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the RecyclerView with the RideAdapter
        rideAdapter = RideAdapter(rideList, { ride ->
            // Handle edit action
            editRide(ride)
        }, { ride ->
            // Handle delete action
            deleteRide(ride)
        })

        // Set up RecyclerView
        binding.recyclerViewRides.apply {
            layoutManager = LinearLayoutManager(this@ListOfRideRequest)
            adapter = rideAdapter
        }

        // Fetch rides from Firebase
        fetchRides()

        // Back button action
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchRides() {
        val ridesRef = FirebaseDatabase.getInstance().getReference("ride_requests")

        ridesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rideList.clear() // Clear the current list
                for (rideSnapshot in snapshot.children) {
                    val ride = rideSnapshot.getValue(RideRequest::class.java) // Get as RideRequest
                    if (ride != null) {
                        rideList.add(ride) // Add the ride to the list
                    }
                }
                rideAdapter.notifyDataSetChanged() // Notify adapter that the data has changed
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListOfRideRequest, "Failed to load rides: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun editRide(ride: RideRequest) {
        Toast.makeText(this, "Edit functionality for Ride ID: ${ride.id}", Toast.LENGTH_SHORT).show()
        // Navigate to an EditRideActivity or show a dialog to edit the ride details
    }

    private fun deleteRide(ride: RideRequest) {
        val rideRef = FirebaseDatabase.getInstance().getReference("rides").child(ride.id ?: return)
        rideRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Ride deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete ride: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}