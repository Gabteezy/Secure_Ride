package com.aces.capstone.secureride

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.adapter.RideRequestAdapter
import com.aces.capstone.secureride.databinding.ActivityDriverDashboardBinding
import com.aces.capstone.secureride.model.RideRequest
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.io.IOException

class DriverDashboard : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityDriverDashboardBinding
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var rideRequestAdapter: RideRequestAdapter
    private val rideRequests = mutableListOf<RideRequest>()
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private var commuterLocationListener: ValueEventListener? = null
    private var rideRequestId: String? = null
    private lateinit var mapFragment: SupportMapFragment
    private var commuterMarker: Marker? = null
    private var driverMarker: Marker? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this)

        rideRequestAdapter = RideRequestAdapter(rideRequests, { rideRequest ->
            acceptRide(rideRequest)
        }, { rideRequest ->
            declineRide(rideRequest)
        })

        binding.rideRequestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@DriverDashboard)
            adapter = rideRequestAdapter
        }

        fetchAvailableRides()
        startDriverLocationUpdates()

        // Initially hide the map fragment

        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
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
        rideRequestRef.child("status").setValue("accepted").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Hide unnecessary views and show the map
                binding.rideRequestsRecyclerView.visibility = View.GONE
                binding.noRequestsTextView.visibility = View.GONE
                mapFragment.view?.visibility = View.VISIBLE

                // Set the commuter and drop-off markers
                val commuterLocation = LatLng(rideRequest.latitude, rideRequest.longitude)
                val dropOffLocation = LatLng(rideRequest.dropOffLatitude, rideRequest.dropOffLongitude)
                setCommuterMarker(commuterLocation)
                setDropOffMarker(dropOffLocation)

                // Get the driver's current location and draw the route
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val driverLocation = LatLng(it.latitude, it.longitude)

                            // Call updateDriverLocationOnMap here
                            updateDriverLocationOnMap(driverLocation)

                            // Fit all markers (driver, commuter, drop-off) in the map view
                            fitMarkersInMap(driverLocation, commuterLocation, dropOffLocation)

                            // 1. Route from driver to commuter
                            requestDirections(driverLocation, commuterLocation)

                            // 2. Route from commuter to drop-off location
                            requestDirections(commuterLocation, dropOffLocation)
                        }
                    }
                    listenForCommuterLocationUpdates(rideRequestId!!)
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest .permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                }
            } else {
                Toast.makeText(this, "Failed to accept ride.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fitMarkersInMap(driverLocation: LatLng, commuterLocation: LatLng, dropOffLocation: LatLng) {
        val builder = LatLngBounds.Builder()
        builder.include(driverLocation)
        builder.include(commuterLocation)
        builder.include(dropOffLocation)

        val bounds = builder.build()
        val padding = 100 // Padding around the edges of the map in pixels
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        googleMap.animateCamera(cameraUpdate)
    }

    private fun updateDriverLocationOnMap(driverLocation: LatLng) {
        // Remove the old marker
        driverMarker?.remove()

        // Add a new marker at the driver's current location
        val markerOptions = MarkerOptions()
            .position(driverLocation)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            .title("Driver Location")

        driverMarker = googleMap.addMarker(markerOptions)
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(driverLocation))
    }

    private fun setDropOffMarker(dropOffLocation: LatLng) {
        Log.d("DriverDashboard", "Setting drop-off marker at: $dropOffLocation")
        val dropOffMarkerOptions = MarkerOptions()
            .position(dropOffLocation)
            .title("Drop-off Location")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)) // Optional: Change color

        // Add the drop-off marker to the map
        googleMap.addMarker(dropOffMarkerOptions)

        // Optionally, move the camera to the drop-off location
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dropOffLocation, 12f))
    }

    private fun setCommuterMarker(commuterLocation: LatLng) {
        if (commuterMarker != null) {
            commuterMarker!!.remove() // Remove the existing marker
        }
        commuterMarker = googleMap.addMarker(
            MarkerOptions()
                .position(commuterLocation)
                .title("Commuter Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }

    private fun setDriverMarker(driverLocation: LatLng) {
        if (driverMarker != null) {
            driverMarker!!.remove() // Remove the existing marker
        }
        driverMarker = googleMap.addMarker(
            MarkerOptions()
                .position(driverLocation)
                .title("Driver Location")
        )
    }

    private fun listenForCommuterLocationUpdates(rideRequestId: String) {
        commuterLocationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val commuterLocation = snapshot.child("latitude").getValue(Double::class.java)?.let { lat ->
                    snapshot.child("longitude").getValue(Double::class.java)?.let { lon ->
                        LatLng(lat, lon)
                    }
                }
                commuterLocation?.let {
                    Log.d("DriverDashboard", "Updated commuter location: $it")
                    setCommuterMarker(it) // Update commuter's marker
                } ?: Log.d("DriverDashboard", "Commuter location not found in snapshot.")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverDashboard", "Failed to load commuter location: ${error.message}")
            }
        }

        val commuterLocationRef = firebaseDatabaseReference.child("commuter_locations").child(rideRequestId)
        commuterLocationRef.addValueEventListener(commuterLocationListener!!)
    }

    private fun startDriverLocationUpdates() {
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                if (ActivityCompat.checkSelfPermission(
                        this@DriverDashboard,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                val driverLocation = LatLng(location.latitude, location.longitude)
                                val driverLocationRef = firebaseDatabaseReference.child("driver_locations").child(auth.currentUser?.uid ?: "")
                                driverLocationRef.setValue(driverLocation)

                                // Call updateDriverLocationOnMap here
                                updateDriverLocationOnMap(driverLocation)
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.e("DriverDashboard", "Location permission issue: ${e.message}")
                        Toast.makeText(this@DriverDashboard, "Failed to update location due to missing permissions.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Request permissions if not already granted
                    ActivityCompat.requestPermissions(
                        this@DriverDashboard,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
                handler.postDelayed(this, 1000) // Update every 3 seconds
            }
        }
        handler.post(runnable)
    }

    private fun requestDirections(origin: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.google_map_api_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=$apiKey"
        val requestQueue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(Request.Method.GET, url, { response ->
            val jsonResponse = JSONObject(response)
            val routes = jsonResponse.getJSONArray("routes")
            if (routes.length() > 0) {
                val points = routes.getJSONObject(0).getJSONObject("overview_polyline").getString("points")
                val polylineList = PolyUtil.decode(points)
                googleMap.addPolyline(PolylineOptions().addAll(polylineList).color(Color.RED).width(10f))
            }
        }, { error ->
            Log.e("DriverDashboard", "Failed to get directions: ${error.message}")
        })

        requestQueue.add(stringRequest)
    }

    private fun declineRide(rideRequest: RideRequest) {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequest.id!!)
        rideRequestRef.child("status").setValue("declined").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Ride declined.", Toast.LENGTH_SHORT).show()

                // Show the back button and the RecyclerView when the ride is declined
                binding.rideRequestsRecyclerView.visibility = View.VISIBLE
                binding.noRequestsTextView.visibility = View.VISIBLE

                // Hide the map
                mapFragment.view?.visibility = View.GONE
            } else {
                Toast.makeText(this, "Failed to decline ride.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}