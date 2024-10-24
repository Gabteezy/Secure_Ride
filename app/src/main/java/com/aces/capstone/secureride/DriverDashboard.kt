package com.aces.capstone.secureride

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.model.RideRequest
import com.google.firebase.database.*

class DriverDashboard : AppCompatActivity() {

    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var rideRequestsRecyclerView: RecyclerView
    private lateinit var rideRequestAdapter: RideRequestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)

        // Initialize Firebase Database reference
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        // Set up RecyclerView
        rideRequestsRecyclerView = findViewById(R.id.rideRequestsRecyclerView)
        rideRequestsRecyclerView.layoutManager = LinearLayoutManager(this)
        rideRequestAdapter = RideRequestAdapter()
        rideRequestsRecyclerView.adapter = rideRequestAdapter

        // Listen for new ride requests
        listenForRideRequests()
    }

    private fun listenForRideRequests() {
        firebaseDatabaseReference.child("ride_requests")
            .orderByChild("status").equalTo("pending")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val rideRequest = snapshot.getValue(RideRequest::class.java)
                    if (rideRequest != null) {
                        rideRequest.id = snapshot.key // Assign the unique ID
                        rideRequestAdapter.addRideRequest(rideRequest)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle changes to existing ride requests
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // Handle removed ride requests
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle moved ride requests
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DriverDashboard, "Error loading ride requests: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.d("DriverDashboard", "Error: ${error.message}")
                }
            })
    }
}
