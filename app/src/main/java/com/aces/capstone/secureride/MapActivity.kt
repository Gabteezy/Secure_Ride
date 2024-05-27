package com.aces.capstone.secureride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.widget.SearchView
import com.aces.capstone.secureride.databinding.ActivityMapBinding
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mGoogleMap: GoogleMap
    private lateinit var binding: ActivityMapBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var currentLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val destinationLatLng = LatLng(7.45, 125.82) // Example destination coordinates
        showRouteToDestination(destinationLatLng)

        val bottomNavigationView: BottomNavigationView = binding.dashboardNav

        setupBottomNavigation(bottomNavigationView)

        Log.d("MapActivity", "onCreate called")

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("MapActivity", "Search query submitted: $query")
                if (query != null) {
                    handleSearchQuery(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }


    private fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCustomerHome -> {
                    startActivity(Intent(this@MapActivity, UserDashboard::class.java))
                    true
                }
                R.id.navCustomerFindRide -> {
                    // Navigate to SearchActivity
                    startActivity(Intent(this@MapActivity, Search::class.java))
                    true
                }
                R.id.navCustomerMyRides -> {
                    startActivity(Intent(this@MapActivity, BookingDetails::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d("MapActivity", "Map is ready")
        mGoogleMap = googleMap
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGoogleMap.isMyLocationEnabled = true
            val tagumCityBounds = LatLngBounds(
                LatLng(7.4135, 125.8090), // Southwest corner of Tagum City
                LatLng(7.4702, 125.8295)  // Northeast corner of Tagum City
            )
            mGoogleMap.setLatLngBoundsForCameraTarget(tagumCityBounds) // Limit map to Tagum City bounds
            getCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun getCurrentLocation() {
        Log.d("MapActivity", "Getting current location")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    currentLocation = LatLng(it.latitude, it.longitude)
                    Log.d("MapActivity", "Current location: $currentLocation")
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 12.0f))
                    mGoogleMap.addMarker(MarkerOptions().position(currentLocation!!).title("Current Location"))
                } ?: run {
                    Log.d("MapActivity", "Location is null")
                }
            }.addOnFailureListener {
                Log.e("MapActivity", "Error getting location: ${it.message}")
            }
        }
    }

    private fun handleSearchQuery(query: String) {
        val geocoder = Geocoder(this)
        try {
            val addresses = geocoder.getFromLocationName(query, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val destination = LatLng(addresses[0].latitude, addresses[0].longitude)
                if (isLocationWithinBounds(destination)) {
                    showRouteToDestination(destination)
                } else {
                    Snackbar.make(findViewById(R.id.mapFragment), "Destination is outside Tagum City", Snackbar.LENGTH_LONG).show()
                }
            } else {
                Snackbar.make(findViewById(R.id.mapFragment), "Location not found", Snackbar.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            Log.e("MapActivity", "Error finding location: ${e.message}")
            e.printStackTrace()
            Snackbar.make(findViewById(R.id.mapFragment), "Error finding location", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun isLocationWithinBounds(location: LatLng): Boolean {
        val tagumCityBounds = LatLngBounds(
            LatLng(7.4135, 125.8090), // Southwest corner of Tagum City
            LatLng(7.4702, 125.8295)  // Northeast corner of Tagum City
        )
        return tagumCityBounds.contains(location)
    }



    private fun showRouteToDestination(destination: LatLng) {
        if (currentLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(currentLocation!!.latitude, currentLocation!!.longitude, destination.latitude, destination.longitude, results)
            val distance = results[0] / 1000  // Convert to kilometers
            Snackbar.make(findViewById(R.id.mapFragment), "Distance: %.2f km".format(distance), Snackbar.LENGTH_LONG).show()

            mGoogleMap.addMarker(MarkerOptions().position(destination).title("Destination"))
            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(currentLocation!!)
            boundsBuilder.include(destination)
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100))
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mGoogleMap.isMyLocationEnabled = true
                    getCurrentLocation()
                }
            } else {
                Snackbar.make(findViewById(R.id.mapFragment), "Location permission denied", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
