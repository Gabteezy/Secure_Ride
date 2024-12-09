package com.aces.capstone.secureride

import android.Manifest
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
import android.view.animation.LinearInterpolator
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
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
    private var isDialogShown = false
    private var driverMarker: Marker? = null
    private var destinationLatitude: Double? = null
    private var currentRideId: String? = null
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
    }
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        // You can now use googleMap to interact with the map, for example:
        googleMap.uiSettings.isZoomControlsEnabled = true
        // other map setup code...
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

    private fun openGoogleMaps(latLng: LatLng) {
        val uri = "geo:${latLng.latitude},${latLng.longitude}?q=${latLng.latitude},${latLng.longitude}(Location)"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        startActivity(intent)
    }

    private fun requestRide() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && destinationLatitude != null && destinationLongitude != null) {
                val userReference = firebaseDatabaseReference.child("user").child(currentUser.uid)
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
                            userId = currentUser.uid,
                            driverId = "",  // This will be updated when a driver accepts the request
                            id = rideRequestRef.key ?: "",
                            info = "Requesting a ride",
                            pickupLocation = pickupAddress ?: "Unknown Pickup Location",
                            dropoffLocation = dropoffAddress ?: "Unknown Dropoff Location",
                            dropOffLatitude = destinationLatitude!!,
                            dropOffLongitude = destinationLongitude!!,
                            destination = "User's Destination",
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

                        rideRequestRef.setValue(rideRequest).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val commuterLocationRef = firebaseDatabaseReference.child("commuter_locations").child(currentUser.uid)
                                commuterLocationRef.setValue(LatLng(location.latitude, location.longitude)).addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(this@UserDashboard, "Ride requested successfully!", Toast.LENGTH_SHORT).show()

                                        // Save the rideId and start listening for ride status updates
                                        currentRideId = rideRequest.id
                                        listenForRideStatusUpdates()
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

    private fun listenForRideStatusUpdates() {
        val currentUser = auth.currentUser ?: return
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests")
            .orderByChild("userId")
            .equalTo(currentUser.uid)

        rideRequestRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // This is called when a new ride request is added
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // This is called when a ride request is updated
                val status = snapshot.child("status").getValue(String::class.java)
                val driverId = snapshot.child("driverId").getValue(String::class.java)

                when (status) {
                    "declined" -> {
                        Toast.makeText(this@UserDashboard, "Your ride request has been declined by the driver.", Toast.LENGTH_SHORT).show()
                    }
                    "accepted" -> {
                        Toast.makeText(this@UserDashboard, "Your ride request has been accepted by the driver.", Toast.LENGTH_SHORT).show()

                        // Fetch and show driver details after ride is accepted
                        if (driverId != null) {
                            fetchDriverDetails(driverId)
                        } else {
                            Toast.makeText(this@UserDashboard, "Driver ID not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "completed" -> {
                        Toast.makeText(this@UserDashboard, "Your ride has been completed.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // This is called when a ride request is removed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // This is called when a ride request is moved
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserDashboard", "Failed to listen for ride status updates: ${error.message}")
            }
        })
    }



    private fun fetchDriverDetails(driverId: String) {
        Log.d("UserDashboard", "Fetching details for driver: $driverId")
        val driverRef = firebaseDatabaseReference.child("user").child(driverId)

        driverRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firstname = snapshot.child("firstname").getValue(String::class.java) ?: "Unknown"
                    val lastname = snapshot.child("lastname").getValue(String::class.java) ?: "Unknown"
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: "Unknown"

                    // Only log if valid driver details are fetched
                    if (firstname != "Unknown" || lastname != "Unknown" || phone != "Unknown") {
                        Log.d("UserDashboard", "Driver details fetched: $firstname $lastname, $phone")
                    } else {
                        Log.d("UserDashboard", "Driver details fetched: Unknown Unknown, Unknown")
                    }

                    // Show the driver details dialog if details are valid
                    if (!isDialogShown && (firstname != "Unknown" || lastname != "Unknown" || phone != "Unknown")) {
                        showDriverDetailsDialog(driverId, firstname, lastname, phone)
                    }
                } else {
                    Log.d("UserDashboard", "Driver details not found.")
                    Toast.makeText(this@UserDashboard, "Driver details not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserDashboard", "Failed to fetch driver details: ${error.message}")
            }
        })
    }


    private fun showDriverDetailsDialog(driverId: String, firstname: String, lastname: String, phone: String) {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_driver_details, null)

        // Set the values dynamically
        dialogView.findViewById<TextView>(R.id.tvDriverName).text = "Name: $firstname $lastname"
        dialogView.findViewById<TextView>(R.id.tvPhone).text = "Phone: $phone"
        dialogView.findViewById<TextView>(R.id.tvDriverId).text = "Driver ID: $driverId"

        // Show dialog on the UI thread
        runOnUiThread {
            if (!isDialogShown) {
                isDialogShown = true
                AlertDialog.Builder(this)
                    .setTitle("Driver Accepted Your Ride")
                    .setView(dialogView)
                    .setPositiveButton("Confirm") { dialog, _ ->
                        // Handle confirmation
                        Toast.makeText(this@UserDashboard, "Ride Confirmed", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        isDialogShown = false  // Reset the flag
                    }
                    .setNegativeButton("Decline") { dialog, _ ->
                        // Handle decline
                        Toast.makeText(this@UserDashboard, "Ride Declined", Toast.LENGTH_SHORT).show()
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

    private fun calculatetotalFare(distanceInKm: Double): Int {
        val baseFare = 17.0 // Example base fare
        val perKmRate = 2.0 // Example rate per kilometer
        val totalFare = baseFare + (distanceInKm * perKmRate)
        return totalFare.toInt() // Convert the fare to an integer
    }
}