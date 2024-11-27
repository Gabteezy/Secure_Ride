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
    private var commuterMarker: Marker? = null
    private var destinationLatitude: Double? = null
    private var destinationLongitude: Double? = null

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

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun fetchAvailableRides() {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").orderByChild("status").equalTo("pending")
        rideRequestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rideRequests.clear()
                Log.d("DriverDashboard", "Ride request count: ${snapshot.childrenCount}")
                for (requestSnapshot in snapshot.children) {
                    val rideRequest = requestSnapshot.getValue(RideRequest::class.java)
                    rideRequest?.let {
                        rideRequests.add(it)
                    }
                }
                rideRequestAdapter.notifyDataSetChanged()
                toggleRideRequestVisibility(rideRequests.isEmpty())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverDashboard", "Failed to load ride requests: ${error.message}")
                Toast.makeText(this@DriverDashboard, "Failed to load ride requests.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleRideRequestVisibility(isEmpty
    : Boolean) {
        binding.noRequestsTextView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rideRequestsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun acceptRide(rideRequest: RideRequest) {
        rideRequestId = rideRequest.id ?: run {
            Toast.makeText(this, "Ride request ID is null", Toast.LENGTH_SHORT).show()
            return
        }

        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequestId!!)
        rideRequestRef.child("status").setValue("accepted").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Ride accepted!", Toast.LENGTH_SHORT).show()

                // Commuter's Location
                val commuterLocation = LatLng(rideRequest.latitude, rideRequest.longitude)
                // Commuter's Drop-off Location
                val commuterDropOffLocation = LatLng(rideRequest.dropOffLatitude, rideRequest.dropOffLongitude)

                Log.d("DriverDashboard", "Commuter's Location: $commuterLocation")
                Log.d("DriverDashboard", "Commuter's Drop-off Location: $commuterDropOffLocation")

                // Show Commuter's Marker
                setCommuterMarker(commuterLocation)

                // Show Drop-off Marker
                setDropOffMarker(commuterDropOffLocation)

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val driverLocation = LatLng(location.latitude, location.longitude)
                            requestDirections(driverLocation, commuterLocation) // Draw polyline from driver to commuter
                            requestDirections(commuterLocation, commuterDropOffLocation) // Draw polyline from commuter to drop-off
                        } else {
                            Toast.makeText(this, "Unable to get driver's location", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
                }

                listenForCommuterLocationUpdates(rideRequestId!!)
            } else {
                Toast.makeText(this, "Failed to accept ride.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setDropOffMarker(dropOffLocation: LatLng) {
        Log.d("DriverDashboard", "Setting drop-off marker at: $dropOffLocation")
        val dropOffMarkerOptions = MarkerOptions()
            .position(dropOffLocation)
            .title("Drop-off Location")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)); // Optional: Change color

        // Add the drop-off marker to the map
        googleMap.addMarker(dropOffMarkerOptions)

        // Optionally, move the camera to the drop-off location
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dropOffLocation, 12f))
    }

//    private fun getLatLngFromAddress(address: String): LatLng? {
//        return try {
//            val results = geocoder.getFromLocationName(address, 1)
//            results?.firstOrNull()?.let {
//                LatLng(it.latitude, it.longitude)
//            } ?: run {
//                Log.e("DriverDashboard", "No address found for $address")
//                null
//            }
//        } catch (e: IOException) {
//            Log.e("DriverDashboard", "Geocoding failed: ${e.message}")
//            null
//        }
//    }

    private fun setCommuterMarker(commuterLocation: LatLng) {
        Log.d("DriverDashboard", "Setting commuter marker at: $commuterLocation")
        val markerOptions = MarkerOptions().position(commuterLocation).title("Commuter Location")

        // Remove the existing marker if it exists
        commuterMarker?.remove()
        commuterMarker = googleMap.addMarker(markerOptions)

        // Move the camera to the commuter's location
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(commuterLocation, 12f))
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
                if (ActivityCompat.checkSelfPermission(this@DriverDashboard, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val driverLocation = LatLng(location.latitude, location.longitude)
                            val driverLocationRef = firebaseDatabaseReference.child("driver_locations").child(auth.currentUser ?.uid ?: "")
                            driverLocationRef.setValue(driverLocation)
                        }
                    }
                }
                handler.postDelayed(this, 5000) // Update every 5 seconds
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun declineRide(rideRequest: RideRequest) {
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequest.id!!)
        rideRequestRef.child("status").setValue("declined").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Ride declined.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to decline ride.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}