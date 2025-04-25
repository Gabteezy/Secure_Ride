package com.aces.capstone.secureride

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
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
import android.widget.Button
import android.widget.EditText
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
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private var googleMap: GoogleMap? = null
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var auth: FirebaseAuth
    private lateinit var driverLocation: LatLng
    private lateinit var riderLocation: LatLng
    private var isDialogShown = false
    private var waitingDialog: AlertDialog? = null
    private var arrivalDialog: AlertDialog? = null
    private var hasActiveRideToastShown = false
    private var currentRideRequest: RideRequest? = null
    private var destinationLatitude: Double? = null
    private var currentRideId: String? = null
    private var locationUpdateHandler: Handler? = null
    private var bookingMarker: Marker? = null
    private var bookingMarkerPosition: LatLng? = null
    private var currentRideStatus: String? = null
    private var gasolinePrice: Double = 17.0 // Default value

    private var destinationLongitude: Double? = null

    private val tagumCitySouthWest = LatLng(7.3400, 125.7500) // Correct South-West corner
    private val tagumCityNorthEast = LatLng(7.5300, 125.8550) // Correct North-East corner
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
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
        checkLocationPermissions()
        startLocationUpdates()
        checkActiveRideRequest()

        listenForRideStatusUpdates()
        fetchGasolinePrice()
    }

    override fun onResume() {
        super.onResume()

        // Restore the booking marker position if it exists
        bookingMarkerPosition?.let {
            setRideLocationMarker(it) // Restore the marker
        }

        // Check if there's a pending ride request
        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
        val rideId = sharedPrefs.getString("rideId", null)
        val rideStatus = sharedPrefs.getString("rideStatus", null)

        if (rideId != null && rideStatus == "pending") {
            // Reopen the waiting dialog
            showWaitingForDriverDialog()
            // Monitor the ride status again
            monitorRideStatus(rideId)
        } else if (rideStatus == "accepted") {
            // If the ride is accepted, hide the "Get Ride" button
            binding.getRideButton.visibility = View.GONE
            // Check if the driver arrival dialog should be shown
            if (sharedPrefs.getBoolean("isArrivalDialogShown", false)) {
                showDriverArrivalDialog("Driver Name") // Replace with actual driver name
            }
        } else if (rideStatus == "completed") {
            // If the ride is completed, show the "Get Ride" button again
            binding.getRideButton.visibility = View.VISIBLE
            // Dismiss the arrival dialog if it's shown
            arrivalDialog?.dismiss()
            clearArrivalDialogStateInPreferences()
        } else {
            // If there's no active ride, ensure the button is visible
            binding.getRideButton.visibility = View.VISIBLE
        }
    }

    private fun fetchGasolinePrice() {
        firebaseDatabaseReference.child("gasoline_price").child("updated_gasoline_price")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    gasolinePrice = snapshot.getValue(Double::class.java) ?: 17.0
                    Log.d("UserDashboard", "Gasoline price updated: $gasolinePrice")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserDashboard", "Failed to fetch gasoline price: ${error.message}")
                }
            })
    }

    private fun getBaseFare(gasolinePrice: Double): Double {
        return when {
            gasolinePrice < 20.00 -> 12.00 // Default for prices below 20
            gasolinePrice <= 29.99 -> 13.00
            gasolinePrice <= 39.99 -> 14.00
            gasolinePrice <= 49.99 -> 15.00
            gasolinePrice <= 59.99 -> 16.00
            gasolinePrice <= 69.99 -> 17.00
            gasolinePrice <= 79.99 -> 18.00
            gasolinePrice <= 89.99 -> 19.00
            gasolinePrice <= 99.99 -> 20.00
            else -> 21.00 // 100.00 and up
        }
    }


    private fun checkActiveRideRequest() {
        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
        val rideStatus = sharedPrefs.getString("rideStatus", null)

        if (rideStatus == "accepted") {
            binding.getRideButton.visibility = View.GONE // Hide the button if ride is accepted
        } else {
            binding.getRideButton.visibility = View.VISIBLE // Show the button otherwise
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

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permission is granted, proceed with location-related tasks
            startLocationUpdates() // For commuter
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, proceed with location-related tasks
                startLocationUpdates() // For commuter
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Location permission is required to access your location.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun startLocationUpdates() {
        locationUpdateHandler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                if (ActivityCompat.checkSelfPermission(this@UserDashboard, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            // Update the user's location in Firebase
                            val commuterLocationRef = firebaseDatabaseReference.child("commuter_locations").child(auth.currentUser ?.uid ?: "")
                            commuterLocationRef.setValue(LatLng(location.latitude, location.longitude))

                            // Update the marker on the map
                            setRideLocationMarker(LatLng(location.latitude, location.longitude))
                        }
                    }
                }
                locationUpdateHandler?.postDelayed(this, 3000) // Update every 3 seconds
            }
        }
        locationUpdateHandler?.post(runnable)
    }



    private fun searchLocation(locationName: String) {
        Log.d("SearchLocation", "Searching for: $locationName") // Log the input

        try {
            val addressList = geocoder.getFromLocationName(locationName, 1)
            if (addressList.isNullOrEmpty()) {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            } else {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)

                // Log the coordinates for debugging
                Log.d("SearchLocation", "Coordinates for $locationName: ${latLng.latitude}, ${latLng.longitude}")

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
            Log.e("SearchLocation", "Geocoder service not available", e)
            Toast.makeText(this, "Geocoder service not available", Toast.LENGTH_SHORT).show()
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

    //    private fun checkForNearbyDrivers(location: LatLng, callback: (Boolean) -> Unit) {
    //        val driversRef = firebaseDatabaseReference.child("drivers")
    //        driversRef.addListenerForSingleValueEvent(object : ValueEventListener {
    //            override fun onDataChange(snapshot: DataSnapshot) {
    //                var isDriverAvailable = false
    //                for (driverSnapshot in snapshot.children) {
    //                    val driverLocation = LatLng(
    //                        driverSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0,
    //                        driverSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0
    //                    )
    //                    val distance = calculateDistance(location.latitude, location.longitude, driverLocation.latitude, driverLocation.longitude)
    //                    if (distance <= 1000) { // Check if within 1000 meters
    //                        isDriverAvailable = true
    //                        break
    //                    }
    //                }
    //                callback(isDriverAvailable)
    //            }
    //
    //            override fun onCancelled(error: DatabaseError) {
    //                Log.e("User Dashboard", "Failed to check for nearby drivers: ${error.message}")
    //                callback(false) // Assume no drivers available on error
    //            }
    //        })
    //    }

    private fun requestRide() {
        val currentUser  = auth.currentUser
        if (currentUser  == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate the rideId first
        val rideId = firebaseDatabaseReference.child("ride_requests").push().key ?: ""
        Log.d("RideRequest", "Generated rideId: $rideId")  // Log to check if the rideId is correct

        // Check for existing ride requests (both accepted and pending)
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").orderByChild("userId").equalTo(currentUser .uid)

        rideRequestRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hasActiveRide = false
                for (rideSnapshot in snapshot.children) {
                    val status = rideSnapshot.child("status").getValue(String::class.java)
                    if (status == "accepted" || status == "pending") {
                        hasActiveRide = true
                        break
                    }
                }

                // If an active ride exists, display a message and return
                if (hasActiveRide) {
                    if (!hasActiveRideToastShown) {
                        Toast.makeText(this@UserDashboard, "You already have an active ride request.", Toast.LENGTH_SHORT).show()
                        hasActiveRideToastShown = true // Set the flag to true
                    }
                    return
                }

                // Get the number of commuters from the UI (assuming you have an EditText for this)
                val commuterCount = binding.commuterCountEditText.text.toString().toIntOrNull() ?: 1
                if (commuterCount < 1 || commuterCount > 6) {
                    Toast.makeText(this@UserDashboard, "Please specify a number of commuters between 1 and 6.", Toast.LENGTH_SHORT).show()
                    return
                }

                // Proceed with ride request if no active ride exists
                if (!hasLocationPermissions()) {
                    requestLocationPermissions()
                    return
                }

                // Disable the booking button to prevent multiple clicks
                binding.getRideButton.isEnabled = false

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null && destinationLatitude != null && destinationLongitude != null) {
                        val userReference = firebaseDatabaseReference.child("user").child(currentUser .uid)
                        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val firstname = snapshot.child("firstname").getValue(String::class.java) ?: "Unknown"
                                val lastname = snapshot.child("lastname").getValue(String::class.java) ?: "Unknown"
                                val userType = snapshot.child("usertype").getValue(String::class.java) ?: "Commuter"
                                val phone = snapshot.child("phone").getValue(String::class.java) ?: "N/A"

                                val pickupAddress = getAddressFromLocation(location.latitude, location.longitude)
                                val dropoffAddress = getAddressFromLocation(destinationLatitude!!, destinationLongitude!!)

                                val currentTime = System.currentTimeMillis()
                                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                val dateTime = dateFormat.format(Date(currentTime))

                                // Calculate fare before showing the waiting dialog
                                val distanceInKm = calculateDistance(location.latitude, location.longitude, destinationLatitude!!, destinationLongitude!!)
                                val totalFare = calculateTotalFare(distanceInKm, commuterCount)

                                // Show fare to the user
                                showFareDialog(totalFare, pickupAddress ?: "Unknown Pickup Location", dropoffAddress ?: "Unknown Drop-off Location", commuterCount) {
                                    // Proceed to show waiting dialog after user acknowledges the fare
                                    showWaitingForDriverDialog()
                                    monitorRideStatus(rideId)

                                    val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
                                    sharedPrefs.edit().putString("rideId", rideId).putString("rideStatus", "pending").apply()

                                    // Initialize the currentRideRequest variable
                                    currentRideRequest = RideRequest(
                                        userId = currentUser .uid,
                                        driverId = "",  // This will be updated when a driver accepts the request
                                        id = rideId,
                                        info = "Requesting a ride",
                                        pickupLocation = pickupAddress ?: "Unknown Pickup Location",
                                        dropoffLocation = dropoffAddress ?: "Unknown Drop-off Location",
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
                                        rideInfo = "Time: $dateTime\nRide ID: $rideId\nNumber of Commuters: $commuterCount\nPhone: $phone",
                                        commuterCount = commuterCount
                                    )

                                    // Set location marker
                                    setRideLocationMarker(LatLng(location.latitude, location.longitude))

                                    // Save ride request to Firebase
                                    firebaseDatabaseReference.child("ride_requests").child(rideId).setValue(currentRideRequest).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            // Hide the "Get a Ride" button after a successful ride request
                                            binding.getRideButton.visibility = View.GONE

                                            // Pass the rideId to the updateCommuterLocation function
                                            updateCommuterLocation(currentUser .uid, location, rideId)
                                            monitorRideStatus(rideId)
                                        } else {
                                            Toast.makeText(this@UserDashboard, "Failed to send ride request.", Toast.LENGTH_SHORT).show()
                                            // Re-enable the button if the request fails
                                            binding.getRideButton.isEnabled = true
                                            dismissWaitingDialog()
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@UserDashboard, "Failed to retrieve user details", Toast.LENGTH_SHORT).show()
                                // Re-enable the button if the request fails
                                binding.getRideButton.isEnabled = true
                                dismissWaitingDialog()
                            }
                        })
                    } else {
                        Toast.makeText(this@UserDashboard, "Unable to get your location", Toast.LENGTH_SHORT).show()
                        // Re-enable the button if the location is not available
                        binding.getRideButton.isEnabled = true
                        dismissWaitingDialog()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error case
                Log.e("User Dashboard", "Failed to check existing ride requests: ${error.message}")
                Toast.makeText(this@UserDashboard, "Failed to check existing ride requests.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showFareDialog(totalFare: Int, pickupLocation: String, dropoffLocation: String, commuterCount: Int, onConfirm: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_fare_confirmation, null)
        dialogView.findViewById<TextView>(R.id.tvFareAmount).text = "Total Fare: ₱$totalFare"
        dialogView.findViewById<TextView>(R.id.tvPickupLocation).text = "Pickup Location: $pickupLocation"
        dialogView.findViewById<TextView>(R.id.tvDropoffLocation).text = "Drop-off Location: $dropoffLocation"
        dialogView.findViewById<TextView>(R.id.tvCommuterCount).text = "Number of Commuters: $commuterCount"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent dismissing by tapping outside
            .create()

        dialogView.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            onConfirm() // Proceed to show waiting dialog
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
            binding.getRideButton.isEnabled = true // Re-enable the button if canceled
        }

        dialog.show()
    }


    private fun showWaitingForDriverDialog() {
        // Check if waitingDialog is already showing
        if (waitingDialog?.isShowing == true) {
            return // Prevent multiple dialogs
        }

        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_waiting_for_driver, null)

        // Load the GIF into the ImageView using Glide
        val waitingGIF = dialogView.findViewById<ImageView>(R.id.ivWaitingGIF) // Reference the correct ImageView
        Glide.with(this)
            .load(R.drawable.waitingdialog) // Replace with your GIF resource or URL
            .into(waitingGIF)

        // Create and show the dialog
        waitingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .show()
    }

    private fun dismissWaitingDialog() {
        Log.d("DialogLifecycle", "Attempting to dismiss dialog")
        runOnUiThread {
            if (waitingDialog?.isShowing == true) {
                waitingDialog?.dismiss()
                waitingDialog = null
                Log.d("DialogLifecycle", "Waiting Dialog dismissed.")
            } else {
                Log.d("DialogLifecycle", "Dialog is already dismissed or null.")
            }
        }
    }




    private fun monitorRideStatus(rideId: String) {
        val rideStatusRef = firebaseDatabaseReference.child("ride_requests").child(rideId).child("status")
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideId)

        val rideStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java) ?: return
                Log.d("RideStatus", "Ride status updated: $status")

                when (status) {
                    "accepted" -> {
                        dismissWaitingDialog()
                        binding.getRideButton.visibility = View.GONE // Hide button when ride is accepted
                        // Save ride status in shared preferences
                        saveRideStatusInPreferences("accepted")
                        Toast.makeText(
                            this@UserDashboard,
                            "Your booking has been accepted by the driver.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Update local status and clear rideId from shared preferences
                        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
                        sharedPrefs.edit().remove("rideId").apply()
                        rideStatusRef.removeEventListener(this)
                    }
                    "expired" -> {
                        dismissWaitingDialog()
                        binding.getRideButton.visibility = View.VISIBLE // Make the button visible
                        binding.getRideButton.isEnabled = true // Ensure the button is clickable
                        Toast.makeText(this@UserDashboard, "Your ride request has expired.", Toast.LENGTH_SHORT).show()

                        // Update local status and clear rideId from shared preferences
                        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
                        sharedPrefs.edit().remove("rideId").apply()
                        rideStatusRef.removeEventListener(this)
                    }
                    "cancelled" -> {
                        handleDriverCancellation(rideId)

                        // Update local status and clear rideId from shared preferences
                        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
                        sharedPrefs.edit().remove("rideId").apply()
                        rideStatusRef.removeEventListener(this)
                    }
                    "declined" -> {
                        handleDriverCancellation(rideId)
                        dismissWaitingDialog()

                        // Fetch RideRequest object to save it to history
                        rideRequestRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(rideSnapshot: DataSnapshot) {
                                val rideRequest = rideSnapshot.getValue(RideRequest::class.java)
                                if (rideRequest != null) {
                                    saveDeclinedRideToHistory(rideRequest)  // Save declined ride to history
                                } else {
                                    Log.e("RideStatus", "Failed to retrieve RideRequest data for saving declined ride to history.")
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("RideStatus", "Failed to retrieve RideRequest: ${error.message}")
                            }
                        })

                        Toast.makeText(
                            this@UserDashboard,
                            "The driver has declined your booking. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Update local status and clear rideId from shared preferences
                        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
                        sharedPrefs.edit().remove("rideId").apply()
                        rideStatusRef.removeEventListener(this)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@UserDashboard,
                    "Failed to track ride status: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        rideStatusRef.addValueEventListener(rideStatusListener)
    }

    private fun saveRideStatusInPreferences(status: String) {
        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("rideStatus", status).apply()
    }

    private fun clearRideStatusInPreferences() {
        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("rideStatus").apply()
    }




    private fun handleDriverCancellation(rideId: String) {
        dismissWaitingDialog()
        binding.getRideButton.isEnabled = true

        // Clear the saved state
        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("rideId").remove("rideStatus").apply()

        Log.d("RideStatus", "Driver canceled the ride: $rideId")
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

                // Check for ride status change and act accordingly
                when (status) {
                    "declined" -> {
                        binding.getRideButton.isEnabled = true
                        currentRideStatus = "declined"
                    }
                    "accepted" -> {
                        currentRideStatus = "accepted"
                        driverId?.let {
                            fetchDriverDetails(it, rideRequest!!.totalFare)
                            if (isDialogShown) {
                                dismissDriverDetailsDialog()
                            }
                        } ?: run {
                            binding.getRideButton.isEnabled = true
                        }
                    }
                    "completed" -> {
                        if (currentRideStatus != "completed") { // Avoid showing the details again
                            Toast.makeText(this@UserDashboard, "Your ride has been completed.", Toast.LENGTH_SHORT).show()
                            bookingMarker?.remove()
                            bookingMarker = null
                            bookingMarkerPosition = null
                            arrivalDialog?.dismiss()
                            currentRideStatus = "completed"
                            binding.getRideButton.visibility = View.VISIBLE // Show button when ride is completed
                            binding.getRideButton.isEnabled = true // Ensure the button is clickable
                            // Clear ride status in shared preferences
                            clearRideStatusInPreferences()
                            rideRequest?.let { completeRide(it) }
                        }
                    }
                }

                // Check if the driver has arrived
                if (status == "accepted" && driverId != null) {
                    checkDriverArrival(driverId, rideRequest)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle ride request removed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle ride request moved
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("User Dashboard", "Failed to listen for ride status updates: ${error.message}")
            }
        })
    }


    private fun checkDriverArrival(driverId: String, rideRequest: RideRequest?) {
        val driverRef = firebaseDatabaseReference.child("drivers").child(driverId)

        driverRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driverLocation = LatLng(
                    snapshot.child("latitude").getValue(Double::class.java) ?: return,
                    snapshot.child("longitude").getValue(Double::class.java) ?: return
                )

                val commuterLocation = LatLng(rideRequest?.latitude ?: return, rideRequest.longitude ?: return)
                val distance = calculateDistance(driverLocation.latitude, driverLocation.longitude, commuterLocation.latitude, commuterLocation.longitude)

                if (distance <= 50) { // Check if the driver is within 50 meters
                    // Dismiss the arrival dialog if it's shown
                    if (arrivalDialog?.isShowing == true) {
                        arrivalDialog?.dismiss()
                        arrivalDialog = null // Clear the dialog reference
                        Toast.makeText(this@UserDashboard, "Your driver has arrived!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserDashboard", "Failed to check driver arrival: ${error.message}")
            }
        })
    }


    private fun dismissDriverDetailsDialog() {
        if (isDialogShown) {
            // Logic to dismiss the driver details dialog
            // You may need to keep a reference to the dialog if you want to dismiss it
            // For example, if you have a dialog reference, you can call dialog.dismiss()
            isDialogShown = false
        }
    }

    private fun fetchDriverDetails(driverId: String, totalFare: Int) {
        Log.d("UserDashboard", "Fetching details for driver: $driverId")

        if (currentRideStatus == "completed" || currentRideStatus == "declined") {
            return // Don't fetch or show driver details if the ride is already completed/declined
        }

        val driverRef = firebaseDatabase.reference.child("user").child(driverId)

        driverRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val firstname = snapshot.child("firstname").getValue(String::class.java) ?: "Unknown"
                    val lastname = snapshot.child("lastname").getValue(String::class.java) ?: "Unknown"
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: "Unknown"
                    val address = snapshot.child("address").getValue(String::class.java) ?: "Unknown"
                    val profile = snapshot.child("profileImage").getValue(String::class.java) ?: ""

                    // Fetch additional driver stats (e.g., total rides)
                    val cateredCommutersRef = firebaseDatabase.reference
                        .child("drivers")
                        .child(driverId)
                        .child("completedRides")

                    cateredCommutersRef.get().addOnCompleteListener { task ->
                        val cateredCount = task.result?.childrenCount?.toInt() ?: 0

                        // Show the dialog with all driver details
                        showDriverDetailsDialog(
                            firstname, lastname, phone, address, profile, totalFare, cateredCount
                        )
                    }
                } else {
                    Log.d("UserDashboard", "Driver details not found.")
                    Toast.makeText(this@UserDashboard, "Driver details not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserDashboard", "Failed to fetch driver details: ${error.message}")
                Toast.makeText(this@UserDashboard, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun saveCompletedRideToHistory(rideRequest: RideRequest) {
        val historyRef = firebaseDatabaseReference.child("user_history").child(auth.currentUser?.uid ?: "")

        // Make sure rideRequest is valid and initialized properly
        if (rideRequest.id == null || rideRequest.firstName == null || rideRequest.lastName == null) {
            Log.e("UserDashboard", "Invalid ride request data!")
            return
        }

        val pickupAddress = rideRequest.pickupLocation ?: "Unknown"
        val dropoffAddress = rideRequest.dropoffLocation ?: "Unknown"

        val rideHistory: HashMap<String, Any?> = hashMapOf(
            "rideId" to rideRequest.id,
            "commuterName" to "${rideRequest.firstName} ${rideRequest.lastName}",
            "pickupLocation" to pickupAddress,
            "dropoffLocation" to dropoffAddress,
            "status" to "COMPLETED",
            "totalFare" to rideRequest.totalFare,
            "timestamp" to System.currentTimeMillis()
        )

        historyRef.push().setValue(rideHistory).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("UserDashboard", "Completed ride saved to history.")
            } else {
                Log.e("UserDashboard", "Failed to save completed ride to history.", task.exception)
            }
        }
    }

    private fun saveDeclinedRideToHistory(rideRequest: RideRequest) {
        val historyRef = firebaseDatabaseReference.child("user_history").child(auth.currentUser?.uid ?: "")

        // Ensure the rideRequest is valid and initialized properly
        if (rideRequest.id == null || rideRequest.firstName == null || rideRequest.lastName == null) {
            Log.e("DriverHistory", "Invalid ride request data!")
            return
        }

        val pickupAddress = rideRequest.pickupLocation ?: "Unknown"
        val dropoffAddress = rideRequest.dropoffLocation ?: "Unknown"

        val rideHistory: HashMap<String, Any?> = hashMapOf(
            "rideId" to rideRequest.id,
            "commuterName" to "${rideRequest.firstName} ${rideRequest.lastName}",
            "pickupLocation" to pickupAddress,
            "dropoffLocation" to dropoffAddress,
            "status" to "DECLINED",
            "totalFare" to rideRequest.totalFare,
            "timestamp" to System.currentTimeMillis()
        )

        historyRef.push().setValue(rideHistory).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("UserHistory", "Declined ride saved to history.")
            } else {
                Log.e("UserHistory", "Failed to save declined ride to history.", task.exception)
            }
        }
    }



    private fun showDriverDetailsDialog(
        firstname: String,
        lastname: String,
        phone: String,
        address: String,
        profile: String,
        totalFare: Int,
        cateredCommuters: Int
    ) {
        // Check if the dialog is already shown
        if (currentRideStatus == "completed" || currentRideStatus == "declined") {
            return
        }

        if (isDialogShown) {
            Log.d("DialogLifecycle", "Dialog is already shown, not displaying again.")
            return // Prevent multiple dialogs
        }

        isDialogShown = true // Set the flag to true

        val dialogView = layoutInflater.inflate(R.layout.dialog_driver_details, null)

        // Populate dialog views
        dialogView.findViewById<TextView>(R.id.tvDriverName).text = "Name: $firstname $lastname"
        dialogView.findViewById<TextView>(R.id.tvPhone).text = "Phone: $phone"
        dialogView.findViewById<TextView>(R.id.tvAddress).text = "Address: $address"
        dialogView.findViewById<TextView>(R.id.tvTotalFare).text = "Total Fare: ₱$totalFare"
        dialogView.findViewById<TextView>(R.id.tvCateredCommuters).text = "Catered Commuters: $cateredCommuters"

        val profileImageView = dialogView.findViewById<ImageView>(R.id.ivDriverProfile)

        // Load profile image (Base64 or URL)
        if (profile.isNotEmpty()) {
            if (profile.startsWith("http")) {
                // Load image from URL using Glide
                Glide.with(this)
                    .load(profile)
                    .placeholder(R.drawable.vector_profile)
                    .into(profileImageView)
            } else {
                // Decode Base64 string
                val bitmap = decodeBase64ToBitmap(profile)
                profileImageView.setImageBitmap(bitmap)
            }
        } else {
            profileImageView.setImageResource(R.drawable.vector_profile) // Default profile
        }

        // Set click listener to the profile image
        profileImageView.setOnClickListener {
            showFullScreenImage(profile)
        }

        // Build and display the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Driver Details")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                isDialogShown = false // Reset the flag when the dialog is dismissed
                showDriverArrivalDialog("$firstname $lastname") // Show arrival dialog after dismissing
            }
            .setOnDismissListener {
                isDialogShown = false // Reset the flag when the dialog is dismissed
            }
            .show()
    }

    private fun showFullScreenImage(imageUrl: String) {
        // Create a new dialog for the full-screen image
        val fullScreenDialogView = layoutInflater.inflate(R.layout.dialog_full_screen_image, null)
        val fullScreenImageView = fullScreenDialogView.findViewById<ImageView>(R.id.ivFullScreenImage)

        // Load the image into the full-screen ImageView
        if (imageUrl.startsWith("http")) {
            Glide.with(this)
                .load(imageUrl)
                .into(fullScreenImageView)
        } else {
            val bitmap = decodeBase64ToBitmap(imageUrl)
            fullScreenImageView.setImageBitmap(bitmap)
        }

        // Show the full-screen dialog
        AlertDialog.Builder(this)
            .setView(fullScreenDialogView)
            .setCancelable(true)
            .setOnDismissListener {
                // Handle any cleanup if necessary
            }
            .show()
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    private fun showDriverArrivalDialog(driverName: String) {
        if (!isFinishing) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_driver_arrival, null)

            // Set the dynamic message
            dialogView.findViewById<TextView>(R.id.tvWaitingMessage).text = "$driverName is on their way to pick you up!"

            // Load the motorcycle GIF using Glide
            val motorcycleGIF = dialogView.findViewById<ImageView>(R.id.ivMotorcycleGIF)
            Glide.with(this)
                .load(R.drawable.arrivaldialog) // Replace with the actual file name or URL
                .into(motorcycleGIF)

            // Show dialog on the UI thread
            runOnUiThread {
                if (!isDialogShown) {
                    isDialogShown = true
                    arrivalDialog = AlertDialog.Builder(this)
                        .setTitle("Waiting for driver arrival")
                        .setView(dialogView)
                        .setCancelable(false) // Prevent dismissing by tapping outside
                        .show()

                    // Save the dialog state in shared preferences
                    val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
                    sharedPrefs.edit().putBoolean("isArrivalDialogShown", true).apply()

                    // Add a listener to dismiss the dialog when the ride is completed
                    arrivalDialog?.setOnDismissListener {
                        isDialogShown = false // Reset the flag when the dialog is dismissed
                        arrivalDialog = null // Clear the dialog reference
                        clearArrivalDialogStateInPreferences()
                    }
                }
            }
        }
    }

    private fun clearArrivalDialogStateInPreferences() {
        val sharedPrefs = getSharedPreferences("RidePrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("isArrivalDialogShown").apply()
    }




    private fun completeRide(rideRequest: RideRequest) {
        // Mark the ride as completed in Firebase
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequest.id!!)
        rideRequestRef.child("status").setValue("completed").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Save the completed ride to history
                saveCompletedRideToHistory(rideRequest) // Call the function here
                // Show the rating dialog
                showRatingDialog(rideRequest.driverId ?: "")
            } else {
                Toast.makeText(this, "Failed to complete the ride", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRatingDialog(driverId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_review, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Submit") { dialog, _ ->
                val rating = ratingBar.rating
                submitRatingToFirebase(driverId, rating)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun submitRatingToFirebase(driverId: String, rating: Float) {
        val ratingId = firebaseDatabaseReference.child("drivers").child(driverId).child("ratings").push().key ?: return
        val ratingData = hashMapOf(
            "rating" to rating,
            "timestamp" to System.currentTimeMillis()
        )

        firebaseDatabaseReference.child("drivers").child(driverId).child("ratings").child(ratingId).setValue(ratingData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Rating submitted", Toast.LENGTH_SHORT).show()
                    calculateAverageRating(driverId) // Calculate average rating after submission
                } else {
                    Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun calculateAverageRating(driverId: String) {
        val ratingsRef = firebaseDatabaseReference.child("drivers").child(driverId).child("ratings")

        ratingsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val ratingsSnapshot = task.result
                var totalRating = 0f
                var count = 0

                for (ratingSnapshot in ratingsSnapshot.children) {
                    val rating = ratingSnapshot.child("rating").getValue(Float::class.java) ?: continue
                    totalRating += rating
                    count++
                }

                val averageRating = if (count > 0) totalRating / count else 0f
                // Update the driver's average rating in their profile
                firebaseDatabaseReference.child("drivers").child(driverId).child("averageRating").setValue(averageRating)
                    .addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Log.d("UserDashboard", "Driver's average rating updated to: $averageRating")
                        } else {
                            Log.e("UserDashboard", "Failed to update driver's average rating: ${updateTask.exception?.message}")
                        }
                    }
            } else {
                Log.e("UserDashboard", "Failed to calculate average rating: ${task.exception?.message}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun stopLocationUpdates() {
        locationUpdateHandler?.removeCallbacksAndMessages(null)
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

    private fun calculateTotalFare(distanceInKm: Double, commuterCount: Int): Int {
        val baseFare = getBaseFare(gasolinePrice) // Get base fare from the table
        val perKmRate = if (distanceInKm > 3) 2.0 else 0.0 // Add ₱2 per km after 3km
        val fareBeforeCommuterAdjustment = baseFare + (distanceInKm * perKmRate)
        val totalFare = fareBeforeCommuterAdjustment * commuterCount // Adjust for number of commuters
        return totalFare.toInt()
    }
}