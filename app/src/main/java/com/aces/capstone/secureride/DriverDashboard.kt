package com.aces.capstone.secureride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

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
    private var commuterLocation: LatLng? = null
    private var commuterDestination: LatLng? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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



        // Set up map fragment
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
                for (requestSnapshot in snapshot.children) {
                    val rideRequest = requestSnapshot.getValue(RideRequest::class.java)
                    rideRequest?.let {
                        rideRequests.add(it)
                    }
                }
                rideRequestAdapter.notifyDataSetChanged()

                binding.noRequestsTextView.visibility = if (rideRequests.isEmpty()) View.VISIBLE else View.GONE
                binding.rideRequestsRecyclerView.visibility = if (rideRequests.isEmpty()) View.GONE else View.VISIBLE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriverDashboard, "Failed to load ride requests.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun acceptRide(rideRequest: RideRequest) {
        rideRequestId = rideRequest.id ?: return
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequestId!!)
        rideRequestRef.child("status").setValue("accepted").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                binding.rideRequestsRecyclerView.visibility = View.GONE
                binding.noRequestsTextView.visibility = View.GONE
                mapFragment.view?.visibility = View.VISIBLE

                // Commuter location (first stop)
                commuterLocation = LatLng(rideRequest.latitude, rideRequest.longitude)
                setCommuterMarker(commuterLocation!!)

                // Commuter's destination location (final stop)
                commuterDestination = LatLng(rideRequest.dropOffLatitude, rideRequest.dropOffLongitude)

                // Get the driver's current location and navigate to commuter's location
                if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val driverLocation = LatLng(it.latitude, it.longitude)
                            updateDriverLocationOnMap(driverLocation)
                            openMapForNavigation(commuterLocation!!)  // Start navigation to commuter
                            monitorDriverArrival(driverLocation, commuterLocation!!)
                        }
                    }
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                }
            } else {
                Toast.makeText(this, "Failed to accept ride.", Toast.LENGTH_SHORT).show()
            }
        }
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
                Toast.makeText(this, "Ride declined.", Toast.LENGTH_SHORT).show()
                binding.rideRequestsRecyclerView.visibility = View.VISIBLE
                binding.noRequestsTextView.visibility = View.VISIBLE
                mapFragment.view?.visibility = View.GONE
            } else {
                Toast.makeText(this, "Failed to decline ride.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
