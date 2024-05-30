package com.aces.capstone.secureride

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.aces.capstone.secureride.databinding.ActivityMapsBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Setup bottom navigation view
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.dashboardNav)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCustomerHome -> {
                    // Navigate to Customer Home
                    startActivity(Intent(this, UserDashboard::class.java))
                    true
                }
                R.id.navDriverNotification -> {
                    // Navigate to Driver Notification
                    startActivity(Intent(this, DriverNotification::class.java))
                    true
                }
                R.id.navDriverProfile -> {
                    // Navigate to Driver Profile
                    startActivity(Intent(this, Profile::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Coordinates of Tagum City
        val tagumCity = LatLng(7.4478, 125.8082)

        // Add a marker in Tagum City and move the camera
        mMap.addMarker(MarkerOptions().position(tagumCity).title("Marker in Tagum City"))

        // Move the camera to Tagum City and set zoom level
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tagumCity, 12f))
    }
}
