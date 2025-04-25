package com.aces.capstone.secureride

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
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
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aces.capstone.secureride.model.RideHistory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.IOException
import java.util.Calendar

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
    private var rideRequestTimer: CountDownTimer? = null
    private lateinit var mapFragment: SupportMapFragment
    private var rideRequestId: String? = null
    private lateinit var rideRequest: RideRequest
    private val rideRequestTimers = HashMap<String, CountDownTimer>()
    private var commuterLocation: LatLng? = null
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var commuterDestination: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var acceptedCommutersCount = 0
    private var driverLocation: LatLng? = null
    private val MAX_COMMUTERS = 6
    private var totalFareToday: Double = 0.0
    private lateinit var todaysEarnings: TextView
    private lateinit var yesterdaysEarnings: TextView
    private lateinit var weeksEarnings: TextView

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        todaysEarnings = findViewById(R.id.todaysEarnings)
        yesterdaysEarnings = findViewById(R.id.yesterdaysEarnings)
        weeksEarnings = findViewById(R.id.weeksEarnings)

        auth = FirebaseAuth.getInstance()
        setDriverOnlineStatus(true)
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        rideRequestAdapter = RideRequestAdapter(
            this, // Pass the context (Activity or Fragment)
            rideRequests,
            { rideRequest -> acceptRide(rideRequest) },
            { rideRequest -> declineRide(rideRequest) },
            { rideRequest -> referDriver(rideRequest) } // Add the refer callback
        )

        binding.rideRequestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DriverDashboard)
            adapter = rideRequestAdapter
        }
        fetchAvailableRides()

        bottomNavigationView = binding.bottomNavView

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navDriverHome -> {
                    // We are already in DriverDashboard, so no need to start new activity
                    true
                }
                R.id.navAcceptedRides -> {
                    // Start the DriverHistory activity
                    startActivity(Intent(this, AcceptedRidesActivity::class.java))
                    true
                }
                R.id.navDriverHistory -> {
                    // Start the DriverHistory activity
                    startActivity(Intent(this, DriverHistory::class.java))
                    true
                }
                R.id.navDriverProfile -> {
                    // Start the DriverProfile activity
                    startActivity(Intent(this, DriverProfile::class.java))
                    true
                }
                else -> false
            }
        }

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchAvailableRides()
            swipeRefreshLayout.isRefreshing = false // Stop the refreshing animation
        }

        // Make sure the correct menu item is selected when returning to this activity
        bottomNavigationView.selectedItemId = R.id.navDriverHome

        fetchDriverStats()
        fetchTotalFare()
        checkLocationPermissions()
        monitorSuspensionStatus()

        // Set up map fragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun setDriverOnlineStatus(isOnline: Boolean) {
        val userId = firebaseAuth.currentUser ?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("user").child(userId)

        userRef.child("isOnline").setValue(isOnline).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("DriverRegister", "Driver online status updated to $isOnline")
            } else {
                Log.e(
                    "DriverRegister",
                    "Failed to update online status: ${task.exception?.message}"
                )
            }
        }
    }

    private fun monitorSuspensionStatus() {
        val currentUser  = auth.currentUser
        if (currentUser  != null) {
            val userRef = firebaseDatabaseReference.child("user").child(currentUser .uid)

            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.getValue(UserData::class.java)
                    if (userData != null) {
                        if (userData.isSuspended) {
                            // Driver is suspended, log them out and redirect to login
                            handleSuspension()
                        }
                    } else {
                        Log.d("Error monitoring suspension status:", "Use data is null")

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("Error monitoring suspension status:", "status: ${error.message}")
                    Toast.makeText(this@DriverDashboard, "Error monitoring suspension status: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun handleSuspension() {
        // Log out the driver
        FirebaseAuth.getInstance().signOut()

        // Show a message to the driver
        Toast.makeText(this, "Your account has been suspended.", Toast.LENGTH_SHORT).show()

        // Redirect to the login screen
        startActivity(Intent(this, LoginUser ::class.java))
        finish() // Close the current activity
    }

    override fun onResume() {
        super.onResume()
        monitorSuspensionStatus()
    }


    override fun onPause() {
        super.onPause()
        // Set the driver as offline when the app is closed
        setDriverOnlineStatus(false)
    }


    private fun fetchTotalFare() {
        val userId = auth.currentUser ?.uid ?: return
        val historyRef = firebaseDatabaseReference.child("driver_history").child(userId)

        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalFareToday = 0.0
                var totalFareYesterday = 0.0
                var totalFareThisWeek = 0.0

                // Get the current time
                val currentTime = System.currentTimeMillis()
                val calendar = Calendar.getInstance()

                // Get the start of today
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfToday = calendar.timeInMillis

                // Get the start of yesterday
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val startOfYesterday = calendar.timeInMillis

                // Get the start of this week (Monday)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val startOfWeek = calendar.timeInMillis

                for (historySnapshot in snapshot.children) {
                    val rideHistory = historySnapshot.getValue(RideHistory::class.java)
                    if (rideHistory != null && rideHistory.status == "COMPLETED") {
                        // Ensure totalFare is added correctly as a Double
                        val rideFare = rideHistory.totalFare?.toDouble() ?: 0.0
                        val rideTimestamp = rideHistory.timestamp ?: 0L // Ensure you have a timestamp field

                        // Check if the ride was completed today
                        if (rideTimestamp >= startOfToday) {
                            totalFareToday += rideFare
                        }

                        // Check if the ride was completed yesterday
                        if (rideTimestamp >= startOfYesterday && rideTimestamp < startOfToday) {
                            totalFareYesterday += rideFare
                        }

                        // Check if the ride was completed this week
                        if (rideTimestamp >= startOfWeek) {
                            totalFareThisWeek += rideFare
                        }
                    }
                }

                // Update the UI with the calculated earnings
                todaysEarnings.text = String.format("Total Fare Today: %.2f", totalFareToday)
                yesterdaysEarnings.text = String.format("Yesterday's Earnings: %.2f", totalFareYesterday)
                weeksEarnings.text = String.format("Week's Earnings: %.2f", totalFareThisWeek)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverDashboard", "Failed to fetch total fare: ${error.message}")
                Toast.makeText(this@DriverDashboard, "Failed to fetch total fare.", Toast.LENGTH_SHORT).show()
            }
        })
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
    private fun referDriver(rideRequest: RideRequest) {
        // Logic to refer a driver
        // For example, you can show a Toast message or open a dialog
//        Toast.makeText(this, "Referred driver for ride request: ${rideRequest.id}", Toast.LENGTH_SHORT).show()

        // You can also implement additional logic here, such as opening a new screen or sending a request to a server
    }



    private fun fetchDriverStats() {
        val driverId = auth.currentUser ?.uid
        if (driverId != null) {
            val driverRef = firebaseDatabaseReference.child("drivers").child(driverId)
            driverRef.child("completedRides").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val completedRidesCount = snapshot.getValue(Long::class.java) ?: 0
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DriverDashboard", "Failed to fetch completed rides count: ${error.message}")
                }
            })
        }
    }



    private fun fetchAvailableRides() {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").orderByChild("status").equalTo("pending")
        rideRequestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Clear previous requests only once
                rideRequests.clear()
                Log.d("DriverDashboard", "Ride request count: ${snapshot.childrenCount}")

                var isAnyRideConfirmed = false  // Track if any ride has been confirmed

                // Get the current driver location once
                getCurrentDriverLocation { driverLocation ->
                    for (requestSnapshot in snapshot.children) {
                        val rideRequest = requestSnapshot.getValue(RideRequest::class.java)
                        rideRequest?.let {
                            // Check if the ride has been confirmed by the commuter
                            if (it.confirmationStatus) {
                                isAnyRideConfirmed = true
                            }

                            val commuterLocation = LatLng(it.latitude, it.longitude)
                            val distance = calculateDistance(driverLocation, commuterLocation)

                            Log.d("DriverDashboard", "Distance to commuter: $distance meters")
                            Log.d("DriverDashboard", "Driver Location: $driverLocation")
                            Log.d("DriverDashboard", "Commuter Location: $commuterLocation")

                            // Adjust the distance threshold
                            if (distance <= 1000) {  // Only add ride requests within 1000 meters
                                // Check if the request is already in the list
                                if (!rideRequests.contains(it)) {
                                    rideRequests.add(it)
                                    Log.d("DriverDashboard", "Adding ride request: ${it.id}")

                                    // Start the timer for this ride request
//                                    startRideRequestTimer(it) // Call the timer function here
                                }
                            }
                        }
                    }

                    // Remove expired requests from the list
                    rideRequestAdapter.notifyDataSetChanged()

                    // Update UI visibility
                    binding.noRequestsTextView.visibility = if (rideRequests.isEmpty()) View.VISIBLE else View.GONE
                    binding.rideRequestsRecyclerView.visibility = if (rideRequests.isEmpty()) View.GONE else View.VISIBLE

                    // Toggle map visibility based on confirmed rides
                    toggleMapVisibility(!isAnyRideConfirmed)

                    // Log visibility states
                    Log.d("DriverDashboard", "No requests visible: ${rideRequests.isEmpty()}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverDashboard", "Failed to load ride requests: ${error.message}")
                Toast.makeText(this@DriverDashboard, "Failed to load ride requests.", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permission is granted, proceed with location-related tasks
            fetchAvailableRides() // For driver
        }
    }
    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, proceed with location-related tasks
                fetchAvailableRides() // For driver
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Location permission is required to access your location.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun getCurrentDriverLocation(onLocationReceived: (LatLng) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val driverLocation = LatLng(location.latitude, location.longitude)
                onLocationReceived(driverLocation)
            } else {
                // Handle the case where the location is null (e.g., return a default location or notify the user)
                Log.e("DriverDashboard", "Driver location is null.")
                onLocationReceived(LatLng(0.0, 0.0)) // Return a default value or handle it appropriately
            }
        }.addOnFailureListener { exception ->
            Log.e("DriverDashboard", "Failed to get driver location: ${exception.message}")
            // Handle the failure case
        }
    }
    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0] // Distance in meters
    }

//    private fun startRideRequestTimer(rideRequest: RideRequest) {
//        val timer = object : CountDownTimer(rideRequest.timeRemaining, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                rideRequest.timeRemaining = millisUntilFinished // Update the time remaining
//                Log.d("DriverDashboard", "Time remaining for ${rideRequest.id}: ${millisUntilFinished / 1000} seconds")
//                rideRequestAdapter.notifyDataSetChanged() // Notify the adapter to refresh the UI
//            }
//
//            override fun onFinish() {
//                rideRequest.status = "expired" // Update the status to expired
//                rideRequest.timeRemaining = 0 // Reset time remaining
//                rideRequestTimers.remove(rideRequest.id ?: "") // Remove the timer from the map
//                rideRequestAdapter.notifyDataSetChanged() // Notify the adapter to refresh the UI
//            }
//        }
//        timer.start()
//        rideRequestTimers[rideRequest.id ?: ""] = timer // Use a non-null key
//    }

//    private fun cancelRideRequestTimer(rideRequestId: String) {
//        rideRequestTimers[rideRequestId]?.cancel()
//        rideRequestTimers.remove(rideRequestId)
//    }

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
        val rideRequestId = rideRequest.id ?: return
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequestId)

        val currentDriverId = auth.currentUser?.uid ?: return

        // Cancel the timer for the accepted ride request
//        rideRequestAdapter.cancelRideRequestTimer(rideRequestId)

        // Check the current accepted commuters count
        val driverRef = firebaseDatabaseReference.child("drivers").child(currentDriverId)
        driverRef.child("acceptedCommutersCount").get().addOnSuccessListener { snapshot ->
            val currentCount = snapshot.getValue(Int::class.java) ?: 0
            val newRideCommuters = rideRequest.commuterCount ?: 0 // Get the number of commuters in the ride request

            // Check if the total number of commuters would exceed the maximum allowed (6)
            if (currentCount + newRideCommuters > MAX_COMMUTERS) {
                showMaxCommuterLimitDialog() // Show a dialog to inform the driver that they reached the limit
                return@addOnSuccessListener
            }

            // Proceed with accepting the ride
            rideRequestRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    val currentRideRequest = mutableData.getValue(RideRequest::class.java)

                    if (currentRideRequest == null || currentRideRequest.status != "pending") {
                        return Transaction.abort()
                    }

                    currentRideRequest.status = "accepted"
                    currentRideRequest.driverId = currentDriverId
                    mutableData.value = currentRideRequest

                    return Transaction.success(mutableData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                    if (committed) {
                        Toast.makeText(this@DriverDashboard, "Ride accepted!", Toast.LENGTH_SHORT).show()

                        // Update the accepted commuters count for the driver
                        driverRef.child("acceptedCommutersCount").setValue(currentCount + newRideCommuters)

                        // Remove the accepted ride request from the list
                        rideRequestAdapter.removeRideRequest(rideRequestId)

                        // Update driver status to indicate they are on a ride
                        driverRef.child("isOnRide").setValue(true).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d("DriverDashboard", "Driver is now on a ride.")
                            } else {
                                Log.e("DriverDashboard", "Failed to update isOnRide status: ${updateTask.exception?.message}")
                            }
                        }

                        // Update driver location
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            location?.let {
                                driverLocation = LatLng(it.latitude, it.longitude) // Initialize driverLocation
                                updateDriverLocationOnMap(driverLocation!!)
                                commuterLocation = LatLng(rideRequest.latitude, rideRequest.longitude)
                                commuterDestination = LatLng(rideRequest.dropOffLatitude, rideRequest.dropOffLongitude)
                                setCommuterMarker(commuterLocation!!)
                                monitorDriverArrival(driverLocation!!, commuterLocation!!) // Pass driverLocation here
                            } ?: run {
                                Log.e("DriverDashboard", "Driver location is null.")
                                // Handle the case where the location is null
                            }
                        }
                    } else {
                        showRideAlreadyAcceptedDialog()
                    }
                }
            })
        }.addOnFailureListener { error ->
            Log.e("DriverDashboard", "Failed to get accepted commuters count: ${error.message}")
        }
    }


    private fun showMaxCommuterLimitDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Limit Reached")
        dialogBuilder.setMessage("You cannot accept more than 6 commuters.")
        dialogBuilder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.create().show()
    }



    // Dialog to notify driver that ride has already been accepted by another driver
    private fun showRideAlreadyAcceptedDialog() {
        // Inflate the custom layout for the dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_ride_accepted, null)

        // Create the AlertDialog and set the custom view
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Prevent dismissing by tapping outside
            .create()

        // Find the button in the custom layout and set its click listener
        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            // Dismiss the dialog when OK is clicked
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
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
        // Check if driverLocation and targetLocation are initialized
        if (driverLocation == null || targetLocation == null) {
            Log.e("DriverDashboard", "Driver location or target location is not initialized.")
            return
        }

        val distanceToTarget = FloatArray(1)
        Location.distanceBetween(
            driverLocation.latitude, driverLocation.longitude,
            targetLocation.latitude, targetLocation.longitude, distanceToTarget
        )

        // Check if the driver is within 50 meters of the target location
        if (distanceToTarget[0] < 50) {
            if (targetLocation == commuterLocation) {
                Toast.makeText(this, "Arrived at commuter location", Toast.LENGTH_SHORT).show()
                notifyCommuter()
                openMapForNavigation(commuterDestination!!)
            } else if (targetLocation == commuterDestination) {
                Toast.makeText(this, "Arrived at the destination", Toast.LENGTH_SHORT).show()
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

    private fun notifyCommuter() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ride_notifications"
        val channelName = "Ride Notifications"

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notification) // Replace with your notification icon
            .setContentTitle("Driver Arrived")
            .setContentText("Your driver has arrived at your location.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
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
        // Cancel the timer for the declined ride request
//        rideRequest.id?.let { cancelRideRequestTimer(it) }

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

        // Make sure rideRequest is valid and initialized properly
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
                Log.d("DriverHistory", "Completed ride saved to history.")
            } else {
                Log.e("DriverHistory", "Failed to save completed ride to history.", task.exception)
            }
        }
    }

}
