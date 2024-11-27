package com.aces.capstone.secureride

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.RatingBar
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aces.capstone.secureride.databinding.ActivityUserDashboardBinding
import com.aces.capstone.secureride.model.RideRequest
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserDashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityUserDashboardBinding
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private var googleMap: GoogleMap? = null
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var driverLocation: LatLng
    private lateinit var riderLocation: LatLng

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

        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        driverLocation = LatLng(intent.getDoubleExtra("driver_latitude", 0.0), intent.getDoubleExtra("driver_longitude", 0.0))
        riderLocation = LatLng(intent.getDoubleExtra("user_latitude", 0.0), intent.getDoubleExtra("user_longitude", 0.0))

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

        binding .getRideButton.setOnClickListener {
            requestRide()
        }

        startLocationUpdates()
        listenForRideStatusUpdates()
        listenForDriverLocationUpdates()
    }

    private fun searchLocation(locationName: String) {
        try {
            val addressList = geocoder.getFromLocationName(locationName, 1)
            if (addressList.isNullOrEmpty()) {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            } else {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)
                googleMap?.addMarker(MarkerOptions().position(latLng).title(locationName))
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                destinationLatitude = address.latitude
                destinationLongitude = address.longitude

                // Save the searched location to Firebase
                val currentUser  = auth.currentUser
                if (currentUser  != null) {
                    val searchedLocationRef = firebaseDatabaseReference.child("searched_locations").child(currentUser .uid)
                    searchedLocationRef.setValue(latLng).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Searched location saved successfully.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to save searched location.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestRide() {
        val currentUser  = auth.currentUser
        if (currentUser  == null) {
            Toast.makeText(this, "User  not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && destinationLatitude != null && destinationLongitude != null) {
                val userReference = firebaseDatabaseReference.child("user").child(currentUser .uid)
                userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val firstname = snapshot.child("firstname").getValue(String::class.java) ?: "Unknown"
                        val lastname = snapshot.child("lastname").getValue(String::class.java) ?: "Unknown"
                        val userType = snapshot.child("usertype").getValue(String::class.java) ?: "Commuter"

                        val pickupAddress = getAddressFromLocation(location.latitude, location.longitude)
                        val dropoffAddress = getAddressFromLocation(destinationLatitude!!, destinationLongitude!!)

                        val currentTime = System.currentTimeMillis()
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val dateTime = dateFormat.format(Date(currentTime))

                        val rideId = firebaseDatabaseReference.child("ride_requests").push().key ?: ""
                        val rideInfo = """
                            Time: $dateTime
                            Ride ID: $rideId
                        """.trimIndent()

                        val distanceInKm = calculateDistance(location.latitude, location.longitude, destinationLatitude!!, destinationLongitude!!)
                        val totalFare = calculatetotalFare(distanceInKm)

                        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").push()
                        val rideRequest = RideRequest(
                            userId = currentUser .uid,
                            id = rideRequestRef.key ?: "",
                            info = "Requesting a ride",
                            pickupLocation = pickupAddress ?: "Unknown Pickup Location",
                            dropoffLocation = dropoffAddress ?: "Unknown Dropoff Location",
                            dropOffLatitude = destinationLatitude!!, // Add dropOffLatitude
                            dropOffLongitude = destinationLongitude!!, // Add dropOffLongitude
                            destination = "User 's Destination",
                            firstName = firstname,
                            lastName = lastname,
                            userType = userType,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            status = "pending",
                            totalFare = totalFare,
                            rideInfo = rideInfo
                        )

                        setRideLocationMarker(LatLng(location.latitude, location.longitude))

                        rideRequestRef.setValue(rideRequest).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val commuterLocationRef = firebaseDatabaseReference.child("commuter_locations").child(currentUser .uid)
                                commuterLocationRef.setValue(LatLng(location.latitude, location.longitude)).addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(this@UserDashboard, "Ride requested successfully!", Toast.LENGTH_SHORT).show()

                                    } else {
                                        Toast.makeText(this@UserDashboard, "Failed to update commuter location.", Toast.LENGTH_SHORT).show()
                                    }
                                }
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

    private fun setRideLocationMarker(latLng: LatLng) {
        googleMap?.addMarker(MarkerOptions().position(latLng).title("Your Location"))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun startLocationUpdates() {
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                if (ActivityCompat.checkSelfPermission(this@UserDashboard, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val commuterLocationRef = firebaseDatabaseReference.child("commuter_locations").child(auth.currentUser ?.uid ?: "")
                            commuterLocationRef.setValue(LatLng(location.latitude, location.longitude))
                        }
                    }
                }
                handler.postDelayed(this, 5000) // Update every 5 seconds
            }
        }
        handler.post(runnable)
    }

    private fun saveDriverRating(driverId: String, newRating: Float) {
        val driverRef = firebaseDatabaseReference.child("drivers").child(driverId)

        driverRef.get().addOnSuccessListener { snapshot ->
            val currentRating = snapshot.child("rating").getValue(Float::class.java) ?: 0f
            val totalRatings = snapshot.child("totalRatings").getValue(Int::class.java) ?: 0

            val updatedTotalRatings = totalRatings + 1
            val updatedRating = (currentRating * totalRatings + newRating) / updatedTotalRatings

            driverRef.updateChildren(mapOf(
                "rating" to updatedRating,
                "totalRatings" to updatedTotalRatings
            )).addOnSuccessListener {
                Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to submit rating.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap?.isMyLocationEnabled = true
            if (::driverLocation.isInitialized && ::riderLocation.isInitialized) {
                requestDirections(driverLocation, riderLocation)
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestDirections(origin: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.google_map_api_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=$apiKey"

        val directionsRequest = StringRequest(Request.Method.GET, url, { response ->
            val jsonResponse = JSONObject(response)
            val routes = jsonResponse.getJSONArray("routes")
            if (routes.length() > 0) {
                val points = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")
                drawPolyline(points)
            }
        }, { error ->
            Toast.makeText(this, "Failed to get directions: ${error.message}", Toast.LENGTH_SHORT).show()
        })

        Volley.newRequestQueue(this).add(directionsRequest)
    }

    private fun drawPolyline(encodedPolyline: String) {
        val pointsList = PolyUtil.decode(encodedPolyline)
        googleMap?.addPolyline(PolylineOptions().addAll(pointsList).color(Color.RED))
    }

    private fun listenForDriverLocationUpdates() {
        val driverId = intent.getStringExtra("driverId") ?: return
        val driverLocationRef = firebaseDatabaseReference.child("driver_locations").child(driverId)

        driverLocationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lon = snapshot.child("longitude").getValue(Double::class.java)

                if (lat != null && lon != null) {
                    driverLocation = LatLng(lat, lon)
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(driverLocation)
                            .title("Driver Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 15f))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("User  Dashboard", "Failed to load driver location: ${error.message}")
            }
        })
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun listenForRideStatusUpdates() {
        val rideRequestId = intent.getStringExtra("rideRequestId") ?: return
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequestId)

        rideRequestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rideStatus = snapshot.child("status").getValue(String::class.java) ?: return
                Toast.makeText(this@UserDashboard, "Ride status: $rideStatus", Toast.LENGTH_SHORT).show()

                if (rideStatus == "completed") {
                    val driverId = snapshot.child("driverId").getValue(String::class.java) ?: return

                    val ratingBar = RatingBar(this@UserDashboard)
                    AlertDialog.Builder(this@UserDashboard)
                        .setTitle("Rate Your Driver")
                        .setView(ratingBar)
                        .setPositiveButton("Submit") { _, _ ->
                            val rating = ratingBar.rating
                            saveDriverRating(driverId, rating)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserDashboard, "Failed to retrieve ride status", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble() / 1000 // Convert Float to Double, and then to kilometers
    }

    private fun calculatetotalFare(distanceInKm: Double): Double {
        val baseFare = 17.0 // Example base fare
        val perKmRate = 2.0 // Example rate per kilometer
        return baseFare + (distanceInKm * perKmRate)

    }
}