package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.bottomnavigation.BottomNavigationView

class DriverDashboard : AppCompatActivity() {

    private lateinit var mapFragment: SupportMapFragment
    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)

        mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment

        mapFragment.getMapAsync { map ->
            googleMap = map
            // Set the bounds to limit the map to Tagum City
            val tagumCityBounds = LatLngBounds(
                LatLng(7.422, 125.757), // Southwest corner of Tagum City
                LatLng(7.495, 125.855) // Northeast corner of Tagum City
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(tagumCityBounds, 0))
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.dashboardNav)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCustomerHome -> {
                    true
                }
                R.id.navDriverNotification -> {
                    startActivity(Intent(this@DriverDashboard, DriverNotification::class.java))
                    true
                }
                R.id.navDriverProfile -> {
                    startActivity(Intent(this@DriverDashboard, Profile::class.java))
                    true
                }
                // Add more cases for other menu items if needed
                else -> false
            }
        }
    }
}
