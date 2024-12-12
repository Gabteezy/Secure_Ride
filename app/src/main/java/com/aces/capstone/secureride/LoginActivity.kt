package com.aces.capstone.secureride

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.location.LocationManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    // Declare the binding variable
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the binding variable
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if location and internet are enabled
        if (!isLocationEnabled()) {
            showLocationDialog()
        }

        if (!isInternetAvailable()) {
            showInternetDialog()
        }

        // Access the button via binding
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this@LoginActivity, LoginUser::class.java))
        }

        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterAs::class.java))
        }
    }

    // Check if Location services are enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Check if internet is available
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    // Show dialog to enable Location
    private fun showLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Disabled")
            .setMessage("Please enable location services to continue.")
            .setPositiveButton("Enable Location") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Show dialog to enable Internet connection
    private fun showInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage("Please connect to the internet to continue.")
            .setPositiveButton("Enable Internet") { _, _ ->
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
