package com.aces.capstone.secureride

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
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
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
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
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()

        // Set the bounds to limit the map to Tagum City
        val tagumCityBounds = LatLngBounds(
            LatLng(7.422, 125.757), // Southwest corner of Tagum City
            LatLng(7.495, 125.855) // Northeast corner of Tagum City
        )
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(tagumCityBounds, 0))
    }

    private fun searchLocation(query: String) {
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val location = LatLng(address.latitude, address.longitude)

                val tagumCityBounds = LatLngBounds(
                    LatLng(7.422, 125.757),
                    LatLng(7.495, 125.855)
                )

                if (tagumCityBounds.contains(location)) {
                    googleMap?.apply {
                        clear()
                        addMarker(MarkerOptions().position(location).title(query))
                        moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                    }
                } else {
                    Toast.makeText(this, "Location is outside Tagum City", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No location found for \"$query\"", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Error occurred while searching", Toast.LENGTH_SHORT).show()
            Log.e("Geocoding", "Geocoding error: ${e.message}")
        }
    }

    private fun requestRide() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the current user location
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Retrieve user's first name and last name from the database dynamically
                val userReference = firebaseDatabaseReference.child("users").child(currentUser.uid)
                userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val firstName = snapshot.child("firstName").getValue(String::class.java) ?: "Unknown"
                        val lastName = snapshot.child("lastName").getValue(String::class.java) ?: "Unknown"

                        // Create a ride request with dynamic user details
                        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").push()
                        val rideRequest = RideRequest(
                            userId = currentUser.uid,
                            id = rideRequestRef.key ?: "",
                            info = "Requesting a ride",
                            destination = "User's Destination", // Set this based on user input
                            firstName = firstName,
                            lastName = lastName,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            status = "pending"
                        )
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
                        Log.e("Firebase", "Error retrieving user data: ${error.message}")
                    }
                })
            } else {
                Toast.makeText(this, "Unable to get your location", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
