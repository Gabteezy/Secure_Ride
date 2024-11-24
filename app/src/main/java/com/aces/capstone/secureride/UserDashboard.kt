package com.aces.capstone.secureride

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aces.capstone.secureride.databinding.ActivityUserDashboardBinding
import com.aces.capstone.secureride.model.RideRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.IOException

class UserDashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityUserDashboardBinding
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private var googleMap: GoogleMap? = null
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var auth: FirebaseAuth

    private var destinationLatitude: Double? = null
    private var destinationLongitude: Double? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this)

        // Initialize the map fragment
        mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Handle search button click
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchLocation(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        // Handle "Get a Ride" button click
        binding.getRideButton.setOnClickListener {
            requestRide()
        }

        listenForRideStatusUpdates()
    }

    private fun requestRide() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Get last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && destinationLatitude != null && destinationLongitude != null) {
                // Fetch user information from the Realtime Database
                val userReference = firebaseDatabaseReference.child("user").child(currentUser.uid)
                userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Retrieve user data
                        val firstname = snapshot.child("firstname").getValue(String::class.java) ?: "Unknown"
                        val lastname = snapshot.child("lastname").getValue(String::class.java) ?: "Unknown"
                        val userType = snapshot.child("usertype").getValue(String::class.java) ?: "Commuter"

                        val pickupAddress = getAddressFromLocation(location.latitude, location.longitude)
                        val dropoffAddress = getAddressFromLocation(destinationLatitude!!, destinationLongitude!!)

                        val results = FloatArray(1)
                        Location.distanceBetween(
                            location.latitude, location.longitude, // Current location
                            destinationLatitude!!, destinationLongitude!!, // Destination location
                            results
                        )

                        // Calculate the fare based on distance
                        val distanceInKm = calculateDistance(location.latitude, location.longitude, destinationLatitude!!, destinationLongitude!!)
                        val totalFare = calculatetotalFare(distanceInKm)

                        // Prepare a new ride request
                        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").push()
                        val rideRequest = RideRequest(
                            userId = currentUser.uid,
                            id = rideRequestRef.key ?: "",
                            info = "Requesting a ride",
                            pickupLocation = pickupAddress ?: "Unknown Pickup Location",
                            dropoffLocation = dropoffAddress ?: "Unknown Dropoff Location",
                            destination = "User's Destination",
                            firstName = firstname,
                            lastName = lastname,
                            userType = userType,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            status = "pending",
                            totalFare = totalFare
                        )

                        // Set a marker on the map at the user's location
                        setRideLocationMarker(LatLng(location.latitude, location.longitude))

                        // Save the ride request to the database
                        rideRequestRef.setValue(rideRequest).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this@UserDashboard, "Ride requested successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@UserDashboard, "Failed to send ride request.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@UserDashboard, "Failed to retrieve user details", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Unable to get your location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: IOException) {
            null
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000 // Convert meters to kilometers
    }

    private fun calculatetotalFare(distanceInKm: Float): Double {
        val baseFare = 17.0 // PHP 17 for the first 3 km
        val additionalFare = 2.0 // PHP 2 for every km after 3 km

        return if (distanceInKm <= 3) {
            baseFare
        } else {
            baseFare + (distanceInKm - 3) * additionalFare
        }
    }

    private fun setRideLocationMarker(location: LatLng) {
        googleMap?.apply {
            clear() // Clear any previous markers
            addMarker(MarkerOptions().position(location).title("Ride Location"))
            moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        enableMyLocation()
    }

    private fun searchLocation(query: String) {
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val location = addresses[0]
                val latLng = LatLng(location.latitude, location.longitude)

                setRideLocationMarker(latLng)
                destinationLatitude = location.latitude
                destinationLongitude = location.longitude
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error searching location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenForRideStatusUpdates() {
        val rideRequestsRef = firebaseDatabaseReference.child("ride_requests")
        rideRequestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val rideRequest = it.getValue(RideRequest::class.java)
                    if (rideRequest?.userId == auth.currentUser?.uid) {
                        if (rideRequest != null) {
                            if (rideRequest.status == "completed") {
                                Toast.makeText(this@UserDashboard, "Your ride is complete!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserDashboard, "Failed to load ride status updates", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

