package com.aces.capstone.secureride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.adapter.RideRequestAdapter
import com.aces.capstone.secureride.databinding.ActivityDriverDashboardBinding
import com.aces.capstone.secureride.model.RideRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import android.os.CountDownTimer
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.IOException

class DriverDashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityDriverDashboardBinding
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var rideRequestAdapter: RideRequestAdapter
    private val rideRequests = mutableListOf<RideRequest>()
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var commuterMarker: Marker? = null
    private var driverMarker: Marker? = null
    private lateinit var mapFragment: SupportMapFragment
    private var rideRequestId: String? = null
    private lateinit var rideRequest: RideRequest
    private var commuterLocation: LatLng? = null
    private var commuterDestination: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        rideRequestAdapter = RideRequestAdapter(rideRequests, { rideRequest -> acceptRide(rideRequest) }, { rideRequest -> declineRide(rideRequest) })

        binding.rideRequestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DriverDashboard)
            adapter = rideRequestAdapter
        }

        fetchAvailableRides()

        bottomNavigationView = binding.bottomNavView

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navDriverHome -> {
                    false
                }
                R.id.navDriverHistory -> {
                    val intent = Intent(this@DriverDashboard, DriverHistory::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navDriverProfile -> {
                    val intent = Intent(this@DriverDashboard, DriverProfile::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Set up map fragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permission already granted, perform location-related tasks
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val driverLocation = LatLng(it.latitude, it.longitude)
                    updateDriverLocationOnMap(driverLocation)
                    openMapForNavigation(commuterLocation!!)  // Start navigation to commuter
                }
            }
        }
    }

    private fun fetchAvailableRides() {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").orderByChild("status").equalTo("pending")
        rideRequestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rideRequests.clear()
                Log.d("DriverDashboard", "Ride request count: ${snapshot.childrenCount}")

                var isAnyRideConfirmed = false  // Track if any ride has been confirmed

                for (requestSnapshot in snapshot.children) {
                    val rideRequest = requestSnapshot.getValue(RideRequest::class.java)
                    rideRequest?.let {
                        rideRequests.add(it)
                        // Check if the ride has been confirmed by the commuter
                        if (it.confirmationStatus) {
                            isAnyRideConfirmed = true
                        }
                    }
                }

                // Remove expired ride requests
                rideRequests.removeAll { it.status == "expired" }

                rideRequestAdapter.notifyDataSetChanged()

                // Show or hide the 'No Requests' text view
                binding.noRequestsTextView.visibility = if (rideRequests.isEmpty()) View.VISIBLE else View.GONE

                // Show or hide the ride requests RecyclerView based on the availability of requests
                binding.rideRequestsRecyclerView.visibility = if (rideRequests.isEmpty()) View.GONE else View.VISIBLE

                // Show or hide the map based on whether any ride is confirmed
                toggleMapVisibility(!isAnyRideConfirmed)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverDashboard", "Failed to load ride requests: ${error.message}")
                Toast.makeText(this@DriverDashboard, "Failed to load ride requests.", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun startRideRequestTimer(rideRequest: RideRequest) {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequest.id ?: return)

        object : CountDownTimer(300000, 1000) {  // 5mins seconds timer
            override fun onTick(millisUntilFinished: Long) {
                // Update UI with the remaining time if needed
                Log.d("DriverDashboard", "Time remaining: ${millisUntilFinished / 1000} seconds")
            }

            override fun onFinish() {
                // Timer finished, update ride status to expired
                rideRequestRef.child("status").setValue("expired").addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this@DriverDashboard, "Ride request expired.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("DriverDashboard", "Failed to update ride request to expired.")
                    }
                }
            }
        }.start()
    }




    private fun toggleMapVisibility(shouldHide: Boolean) {
        if (shouldHide) {
            // Hide the map and show the RecyclerView
            mapFragment.view?.visibility = View.GONE
            binding.rideRequestsRecyclerView.visibility = View.VISIBLE
        } else {
            // Show the map and hide the RecyclerView
            mapFragment.view?.visibility = View.VISIBLE
            binding.rideRequestsRecyclerView.visibility = View.GONE
        }
    }

    private fun acceptRide(rideRequest: RideRequest) {
        rideRequestId = rideRequest.id ?: return
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequestId!!)
        val currentDriverId = auth.currentUser?.uid ?: return

        // Assuming driver details are fetched from a separate "drivers" node in Firebase
        firebaseDatabaseReference.child("drivers").child(currentDriverId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driverName = snapshot.child("firstname").getValue(String::class.java)
                val driverPhone = snapshot.child("phone").getValue(String::class.java)

                rideRequestRef.child("status").setValue("accepted").addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Adding driver details to the ride request
                        rideRequestRef.child("driverId").setValue(currentDriverId)
                        rideRequestRef.child("driverName").setValue(driverName)
                        rideRequestRef.child("driverPhone").setValue(driverPhone)

                        // Proceed with rest of the flow (e.g., setting map markers, navigating to commuter)
                        binding.rideRequestsRecyclerView.visibility = View.GONE
                        binding.noRequestsTextView.visibility = View.GONE
                        mapFragment.view?.visibility = View.VISIBLE

                        commuterLocation = LatLng(rideRequest.latitude, rideRequest.longitude)
                        setCommuterMarker(commuterLocation!!)
                        commuterDestination = LatLng(rideRequest.dropOffLatitude, rideRequest.dropOffLongitude)

                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                val driverLocation = LatLng(it.latitude, it.longitude)
                                updateDriverLocationOnMap(driverLocation)
                                openMapForNavigation(commuterLocation!!)
                                monitorDriverArrival(driverLocation, commuterLocation!!)
                            }
                        }
                    } else {
                        Toast.makeText(this@DriverDashboard, "Failed to accept ride.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverDashboard", "Failed to fetch driver details: ${error.message}")
            }
        })
    }


    private fun updateDriverLocationOnMap(driverLocation: LatLng) {
        driverMarker?.remove()
        driverMarker = googleMap.addMarker(
            MarkerOptions().position(driverLocation).title("Driver Location")
        )
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 12f))
    }

    private fun setCommuterMarker(commuterLocation: LatLng) {
        commuterMarker?.remove()
        commuterMarker = googleMap.addMarker(
            MarkerOptions().position(commuterLocation).title("Commuter Location")
        )
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(commuterLocation, 12f))
    }

    private fun monitorDriverArrival(driverLocation: LatLng, targetLocation: LatLng) {
        val distanceToTarget = FloatArray(1)
        Location.distanceBetween(
            driverLocation.latitude, driverLocation.longitude,
            targetLocation.latitude, targetLocation.longitude, distanceToTarget
        )

        if (distanceToTarget[0] < 50) { // Within 50 meters of target location
            if (targetLocation == commuterLocation) {
                // Arrived at commuter's location
                Toast.makeText(this, "Arrived at commuter location", Toast.LENGTH_SHORT).show()
                openMapForNavigation(commuterDestination!!) // Start navigating to the destination
            } else if (targetLocation == commuterDestination) {
                // Arrived at commuter's destination
                Toast.makeText(this, "Arrived at the destination", Toast.LENGTH_SHORT).show()

                // Mark the ride as completed in Firebase
                val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequestId!!)
                rideRequestRef.child("status").setValue("completed").addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Save completed ride to Driver History
                        saveCompletedRideToHistory(rideRequest)

                        Toast.makeText(this, "Ride completed successfully", Toast.LENGTH_SHORT).show()
                        // Navigate back to the DriverDashboard
                        val intent = Intent(this@DriverDashboard, DriverDashboard::class.java)
                        startActivity(intent)
                        finish() // Close the current activity
                    } else {
                        Toast.makeText(this, "Failed to complete the ride", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // Continuously monitor location updates if not arrived
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val newDriverLocation = LatLng(it.latitude, it.longitude)
                    updateDriverLocationOnMap(newDriverLocation)
                    monitorDriverArrival(newDriverLocation, targetLocation)
                }
            }
        }
    }

    private fun saveCompletedRideToHistory(rideRequest: RideRequest) {
        val historyRef = firebaseDatabaseReference.child("driver_history").child(auth.currentUser?.uid ?: "")

        // Get the addresses from the RideRequest (assuming they are stored as human-readable addresses)
        val pickupAddress = rideRequest.pickupLocation ?: "Unknown" // Default to "Unknown" if null
        val dropoffAddress = rideRequest.dropoffLocation ?: "Unknown" // Default to "Unknown" if null

        // Create a map to store the ride history in Firebase
        val rideHistory: HashMap<String, Any?> = hashMapOf(
            "rideId" to rideRequest.id,
            "commuterName" to "${rideRequest.firstName} ${rideRequest.lastName}",
            "pickuplocation" to pickupAddress, // Use 'pickuplocation' here
            "dropofflocation" to dropoffAddress, // Use 'dropofflocation' here
            "status" to "completed",  // Mark the status as completed
            "rideInfo" to rideRequest.rideInfo,
            "fare" to rideRequest.totalFare,
            "timestamp" to System.currentTimeMillis()
        )

        historyRef.push().setValue(rideHistory).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("DriverDashboard", "Completed ride saved to history.")
            } else {
                Log.e("DriverDashboard", "Failed to save completed ride to history: ${task.exception?.message}")
            }
        }
    }



    private fun openMapForNavigation(destination: LatLng) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps not installed.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun declineRide(rideRequest: RideRequest) {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequest.id!!)
        rideRequestRef.child("status").setValue("declined").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Save the declined ride to history
                saveDeclinedRideToHistory(rideRequest)

                Toast.makeText(this, "Ride declined.", Toast.LENGTH_SHORT).show()
                binding.rideRequestsRecyclerView.visibility = View.VISIBLE
                binding.noRequestsTextView.visibility = View.VISIBLE

                // Hide the map
                mapFragment.view?.visibility = View.GONE
            } else {
                Toast.makeText(this, "Failed to decline ride.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun saveDeclinedRideToHistory(rideRequest: RideRequest) {
        val historyRef = firebaseDatabaseReference.child("driver_history").child(auth.currentUser?.uid ?: "")

        // Get the addresses from the RideRequest (assuming they are stored as human-readable addresses)
        val pickupAddress = rideRequest.pickupLocation ?: "Unknown" // Default to "Unknown" if null
        val dropoffAddress = rideRequest.dropoffLocation ?: "Unknown" // Default to "Unknown" if null


        // Create a map to store the ride history in Firebase
        val rideHistory: HashMap<String, Any?> = hashMapOf(
            "rideId" to rideRequest.id,
            "commuterName" to "${rideRequest.firstName} ${rideRequest.lastName}",
            "pickuplocation" to pickupAddress, // Use 'pickuplocation' here
            "dropofflocation" to dropoffAddress, // Use 'dropofflocation' here
            "status" to "Declined",
            "rideInfo" to rideRequest.rideInfo,
            "fare" to rideRequest.totalFare,
            "timestamp" to System.currentTimeMillis()
        )


        // Save the declined ride to the Firebase database
        historyRef.push().setValue(rideHistory)
    }

}
