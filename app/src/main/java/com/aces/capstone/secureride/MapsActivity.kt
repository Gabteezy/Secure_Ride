package com.aces.capstone.secureride

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps) // Make sure to create this layout

        // Initialize the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Get the driver's and user's coordinates from the intent
        val driverLocation = LatLng(
            intent.getDoubleExtra("driver_latitude", 0.0),
            intent.getDoubleExtra("driver_longitude", 0.0)
        )
        val userDestination = LatLng(
            intent.getDoubleExtra("user_latitude", 0.0),
            intent.getDoubleExtra("user_longitude", 0.0)
        )

        // Add markers for driver and user destination
        mMap.addMarker(MarkerOptions().position(driverLocation).title("Driver Location"))
        mMap.addMarker(MarkerOptions().position(userDestination).title("User Destination"))

        // Move the camera to the driver's location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 15f))

        // Draw a route (this example uses a simple line; real implementation would use Directions API)
        drawRoute(driverLocation, userDestination)
    }

    private fun drawRoute(driverLocation: LatLng, userDestination: LatLng) {
        val polylineOptions = PolylineOptions()
            .add(driverLocation)
            .add(userDestination)
            .width(5f)
            .color(android.graphics.Color.BLUE)

        mMap.addPolyline(polylineOptions)
    }
}
