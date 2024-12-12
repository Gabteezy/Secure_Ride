package com.aces.capstone.secureride

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.SearchView
import android.widget.TextView
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
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
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
    private var isDialogShown = false
    private var driverMarker: Marker? = null
    private var destinationLatitude: Double? = null
    private var currentRideId: String? = null
    private var bookingMarker: Marker? = null
    private var bookingMarkerPosition: LatLng? = null
    private var destinationLongitude: Double? = null
    private val tagumCitySouthWest = LatLng(7.4046, 125.7516) // South-West corner
    private val tagumCityNorthEast = LatLng(7.5281, 125.8507) // North-East corner

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

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    searchLocation(query)
                    hideKeyboard() // Hide the keyboard after search
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavView)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCommuterHome -> {
                    true
                }
                R.id.navCommuterHistory -> {
                    val intent = Intent(this@UserDashboard, UserHistory::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navCommuterProfile -> {
                    val intent = Intent(this@UserDashboard, UserProfile::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        binding .getRideButton.setOnClickListener {
            requestRide()
        }

        startLocationUpdates()
        listenForRideStatusUpdates()
    }

    override fun onResume() {
        super.onResume()
        // Check if the booking marker position is not null
        bookingMarkerPosition?.let {
            setRideLocationMarker(it) // Restore the marker
        }
    }
    private fun setRideLocationMarker(latLng: LatLng) {
        // Remove the existing marker if it exists
        bookingMarker?.remove()

        // Add a new marker and store its reference
        bookingMarker = googleMap?.addMarker(MarkerOptions().position(latLng).title("Your Location"))
        bookingMarkerPosition = latLng // Store the position
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Define the bounds for Tagum City
        val bounds = LatLngBounds(tagumCitySouthWest, tagumCityNorthEast)

        // Limit the camera movement within the bounds of Tagum City
        googleMap.setLatLngBoundsForCameraTarget(bounds)

        // Set up location tracking using FusedLocationProviderClient
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                // Get the user's current location
                val userLocation = LatLng(it.latitude, it.longitude)

                // Add a marker at the user's location
                googleMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))

                // Update the camera position to the user's location with a zoom level of 15
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(userLocation, 15f)
                googleMap.animateCamera(cameraUpdate)
            } ?: run {
                // If location is null, you can default to Tagum City center
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(7.4663, 125.8007), 13f)
                googleMap.animateCamera(cameraUpdate)
            }
        }
    }

    private fun searchLocation(locationName: String) {
        try {
            val addressList = geocoder.getFromLocationName(locationName, 1)
            if (addressList.isNullOrEmpty()) {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            } else {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)

                // Check if the searched location is within Tagum City
                if (!isWithinTagumCity(latLng)) {
                    Toast.makeText(this, "Location must be within Tagum City", Toast.LENGTH_SHORT).show()
                    return
                }

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

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = currentFocus ?: View(this) // Get the current focused view or create a new one
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun isWithinTagumCity(location: LatLng): Boolean {
        return location.latitude in tagumCitySouthWest.latitude..tagumCityNorthEast.latitude &&
                location.longitude in tagumCitySouthWest.longitude..tagumCityNorthEast.longitude
    }

    private fun requestRide() {
        val currentUser  = auth.currentUser
        if (currentUser  == null) {
            Toast.makeText(this, "User  not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (!hasLocationPermissions()) {
            requestLocationPermissions()
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
                        val totalFare = calculateTotalFare(distanceInKm)

                        val rideRequest = RideRequest(
                            userId = currentUser .uid,
                            driverId = "",  // This will be updated when a driver accepts the request
                            id = rideId,
                            info = "Requesting a ride",
                            pickupLocation = pickupAddress ?: "Unknown Pickup Location",
                            dropoffLocation = dropoffAddress ?: "Unknown Dropoff Location",
                            dropOffLatitude = destinationLatitude!!,
                            dropOffLongitude = destinationLongitude!!,
                            destination = "User 's Destination",
                            firstName = firstname,
                            lastName = lastname,
                            userType = userType,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            status = "pending",  // Initial status is "pending"
                            totalFare = totalFare,
                            rideInfo = rideInfo
                        )

                        setRideLocationMarker(LatLng(location.latitude, location.longitude))

                        firebaseDatabaseReference.child("ride_requests").child(rideId).setValue(rideRequest).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Pass the rideId to the updateCommuterLocation function
                                updateCommuterLocation(currentUser .uid, location, rideId)
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

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun updateCommuterLocation(userId: String, location: Location, rideId: String) {
        val commuterLocationRef = firebaseDatabaseReference.child("commuter_locations").child(userId)
        commuterLocationRef.setValue(LatLng(location.latitude, location.longitude)).addOnCompleteListener { updateTask ->
            if (updateTask.isSuccessful) {
                Toast.makeText(this@UserDashboard, "Ride requested successfully!", Toast.LENGTH_SHORT).show()
                currentRideId = rideId // Now this will work
                listenForRideStatusUpdates()
            } else {
                Toast.makeText(this@UserDashboard, "Failed to update commuter location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun listenForRideStatusUpdates() {
        val currentUser = auth.currentUser ?: return
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests")
            .orderByChild("userId")
            .equalTo(currentUser.uid)

        rideRequestRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle new ride request added
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val status = snapshot.child("status").getValue(String::class.java)
                val driverId = snapshot.child("driverId").getValue(String::class.java)
                val rideRequest = snapshot.getValue(RideRequest::class.java)

                when (status) {
                    "declined" -> {
                        Toast.makeText(this@UserDashboard, "Your ride request has been declined by the driver.", Toast.LENGTH_SHORT).show()
                    }
                    "accepted" -> {
                        Toast.makeText(this@UserDashboard, "Your ride request has been accepted by the driver.", Toast.LENGTH_SHORT).show()
                        rideRequest?.let { saveCompletedRideToHistory(it) }
                        driverId?.let { fetchDriverDetails(it, rideRequest!!.totalFare) } ?: run {
                            Toast.makeText(this@UserDashboard, "Driver ID not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "completed" -> {
                        Toast.makeText(this@UserDashboard, "Your ride has been completed.", Toast.LENGTH_SHORT).show()
                        bookingMarker?.remove()
                        bookingMarker = null
                        bookingMarkerPosition = null
                        rideRequest?.let { saveCompletedRideToHistory(it) }
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle ride request removed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle ride request moved
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("User  Dashboard", "Failed to listen for ride status updates: ${error.message}")
            }
        })
    }

    private fun fetchDriverDetails(driverId: String, totalFare: Int) {
        Log.d("UserDashboard", "Fetching details for driver: $driverId")
        val driverRef = firebaseDatabaseReference.child("user").child(driverId)

        driverRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firstname = snapshot.child("firstname").getValue(String::class.java) ?: "Unknown"
                    val lastname = snapshot.child("lastname").getValue(String::class.java) ?: "Unknown"
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: "Unknown"
                    val address = snapshot.child("address").getValue(String::class.java) ?: "Unknown"
                    val profile = snapshot.child("profileimage").getValue(String::class.java) ?: ""
                    val platenumber = snapshot.child("platenumber").getValue(String::class.java) ?: "Unknown"
                    val licenceImage = snapshot.child("licenceImage").getValue(String::class.java) ?: "Unknown"

                    // Only log if valid driver details are fetched
                    if (firstname != "Unknown" || lastname != "Unknown" || phone != "Unknown" || address != "Unknown" || profile.isNotEmpty() || platenumber != "Unknown" || licenceImage != "Unknown") {
                        Log.d("UserDashboard", "Driver details fetched: $firstname $lastname, $phone")
                    } else {
                        Log.d("UserDashboard", "Driver details fetched: Unknown Unknown, Unknown")
                    }

                    // Show the driver details dialog if details are valid
                    if (!isDialogShown && (firstname != "Unknown" || lastname != "Unknown" || phone != "Unknown" || address != "Unknown" || profile.isNotEmpty() || platenumber != "Unknown" || licenceImage != "Unknown")) {
                        showDriverDetailsDialog(firstname, lastname, phone, address, profile, platenumber, licenceImage, totalFare)
                    }
                } else {
                    Log.d("UserDashboard", "Driver details not found.")
                    Toast.makeText(this@UserDashboard, "Driver details not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserDashboard ", "Failed to fetch driver details: ${error.message}")
            }
        })
    }
    private fun saveCompletedRideToHistory(rideRequest: RideRequest) {
        val historyRef = firebaseDatabaseReference.child("user_history").child(rideRequest.userId ?: "")
        val rideHistory = hashMapOf(
            "rideId" to rideRequest.id,
            "commuterName" to "${rideRequest.firstName} ${rideRequest.lastName}",
            "pickupLocation" to rideRequest.pickupLocation,
            "dropoffLocation" to rideRequest.dropoffLocation,
            "status" to "completed",
            "rideInfo" to rideRequest.rideInfo,
            "fare" to rideRequest.totalFare,
            "timestamp" to System.currentTimeMillis()
        )

        historyRef.push().setValue(rideHistory).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("User Dashboard", "Completed ride saved to user history.")
            } else {
                Log.e("User Dashboard", "Failed to save completed ride to user history: ${task.exception?.message}")
            }
        }
    }


    private fun showDriverDetailsDialog(firstname: String, lastname: String, phone: String, address: String, profile: String, platenumber: String, licenceImage: String, totalFare: Int) {
        if (!isFinishing()) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_driver_details, null)

            // Set the values dynamically for TextViews
            dialogView.findViewById<TextView>(R.id.tvDriverName).text = "Name: $firstname $lastname"
            dialogView.findViewById<TextView>(R.id.tvPhone).text = "Phone: $phone"
            dialogView.findViewById<TextView>(R.id.tvAddress).text = "Address: $address"
            dialogView.findViewById<TextView>(R.id.tvPlateNumber).text =
                "Plate Number: $platenumber"
            dialogView.findViewById<TextView>(R.id.tvDriverLicense).text =
                "licenceImage: $licenceImage"
            dialogView.findViewById<TextView>(R.id.tvTotalFare).text =
                "Total Fare: ₱$totalFare" // Display the total fare

            // Set the profile image (profile is expected to be a URI or URL here)
            val profileImageView = dialogView.findViewById<ImageView>(R.id.ivDriverProfile)

            if (profile.isNotEmpty()) {
                // Check if the profile is a valid URL or URI
                if (profile.startsWith("http") || profile.startsWith("content://")) {
                    // If the profile is a URL or URI, load the image using Glide or Picasso
                    Glide.with(this)
                        .load(profile) // Load image from URL or URI
                        .placeholder(R.drawable.vector_profile) // Placeholder image if the URL fails
                        .into(profileImageView)
                } else {
                    // If profile is a resource name (local resource), use this method
                    profileImageView.setImageDrawable(
                        resources.getDrawable(
                            resources.getIdentifier(
                                profile,
                                "drawable",
                                packageName
                            )
                        )
                    )
                }
            } else {
                // Set a default image if profile is empty
                profileImageView.setImageResource(R.drawable.vector_profile) // Default image
            }

            // Show dialog on the UI thread
            runOnUiThread {
                if (!isDialogShown) {
                    isDialogShown = true
                    AlertDialog.Builder(this)
                        .setTitle("Driver Accepted Your Ride")
                        .setView(dialogView)
                        .setPositiveButton("Okay") { dialog, _ ->
                            // Handle confirmation
                            Toast.makeText(this@UserDashboard, "Ride Confirmed", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            isDialogShown = false  // Reset the flag
                        }
                        .setOnCancelListener {
                            isDialogShown = false  // Reset the flag if the dialog is cancelled
                        }
                        .show()
                }
            }
        }
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
                handler.postDelayed(this, 1000) // Update every 1 seconds
            }
        }
        handler.post(runnable)
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

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble() / 1000 // Convert Float to Double, and then to kilometers
    }

    private fun calculateTotalFare(distanceInKm: Double): Int {
        val baseFare = 17.0 // Example base fare
        val perKmRate = 2.0 // Example rate per kilometer
        val totalFare = baseFare + (distanceInKm * perKmRate)
        return totalFare.toInt() // Convert the fare to an integer
    }
}