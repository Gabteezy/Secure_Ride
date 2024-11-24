package com.aces.capstone.secureride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.adapter.RideRequestAdapter
import com.aces.capstone.secureride.databinding.ActivityDriverDashboardBinding
import com.aces.capstone.secureride.model.RideRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class DriverDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityDriverDashboardBinding
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var rideRequestAdapter: RideRequestAdapter
    private val rideRequests = mutableListOf<RideRequest>()

    // Location client
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var driverLatitude: Double = 0.0
    private var driverLongitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup RecyclerView
        rideRequestAdapter = RideRequestAdapter(rideRequests, { rideRequest ->
            acceptRide(rideRequest)
        }, { rideRequest ->
            declineRide(rideRequest)
        })

        binding.rideRequestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DriverDashboard)
            adapter = rideRequestAdapter
        }

        // Fetch available ride requests
        fetchAvailableRides()
    }

    private fun fetchAvailableRides() {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").orderByChild("status").equalTo("pending")
        rideRequestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rideRequests.clear() // Clear the list to avoid duplicates
                for (requestSnapshot in snapshot.children) {
                    val rideRequest = requestSnapshot.getValue(RideRequest::class.java)
                    if (rideRequest != null) {
                        rideRequests.add(rideRequest)
                    }
                }
                rideRequestAdapter.notifyDataSetChanged() // Notify the adapter about data changes

                // Manage the visibility of the no requests text view
                if (rideRequests.isEmpty()) {
                    binding.noRequestsTextView.visibility = View.VISIBLE
                    binding.historyLogoImageView.visibility = View.VISIBLE
                    binding.rideRequestsRecyclerView.visibility = View.GONE
                } else {
                    binding.noRequestsTextView.visibility = View.GONE
                    binding.historyLogoImageView.visibility = View.GONE
                    binding.rideRequestsRecyclerView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriverDashboard, "Failed to load ride requests: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun acceptRide(rideRequest: RideRequest) {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequest.id.toString())
        rideRequestRef.child("status").setValue("accepted").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Ride accepted successfully!", Toast.LENGTH_SHORT).show()

                // Get the driver's current location
                getDriverLocation { latitude, longitude ->
                    // Start the MapActivity with driver's location and user's destination
                    val intent = Intent(this, MapsActivity::class.java)
                    intent.putExtra("driver_latitude", latitude)
                    intent.putExtra("driver_longitude", longitude)
                    intent.putExtra("user_latitude", rideRequest.latitude) // Assuming these are available in RideRequest
                    intent.putExtra("user_longitude", rideRequest.longitude)
                    startActivity(intent)
                }
            } else {
                Toast.makeText(this, "Failed to accept ride.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDriverLocation(callback: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    driverLatitude = location.latitude
                    driverLongitude = location.longitude
                    callback(driverLatitude, driverLongitude)
                } else {
                    Toast.makeText(this, "Unable to get driver location.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun declineRide(rideRequest: RideRequest) {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequest.id.toString())
        rideRequestRef.child("status").setValue("declined").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Ride declined successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to decline ride.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}