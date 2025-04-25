package com.aces.capstone.secureride

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityLoginBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    // Declare the binding variable
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isUserAuthenticated()) {
            redirectToDashboard() // Redirect to the appropriate dashboard
            return
        }

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

    private fun isUserAuthenticated(): Boolean {
        return FirebaseAuth.getInstance().currentUser  != null
    }

    private fun redirectToDashboard() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val userRef = FirebaseDatabase.getInstance().reference.child("user").child(userId)

            userRef.child("userType").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userType = snapshot.getValue(String::class.java)
                    Log.d("LoginActivity", "User Type: $userType")
                    when (userType) {
                        "Driver" -> {
                            // Check if the driver is suspended
                            checkIfDriverSuspended(userId)
                        }
                        "Commuter" -> {
                            startActivity(Intent(this@LoginActivity, UserDashboard::class.java))
                            finish()
                        }
                        "Admin" -> {
                            startActivity(Intent(this@LoginActivity, AdminDashboard::class.java))
                            finish()
                        }
                        else -> {
                            Toast.makeText(this@LoginActivity, "Unknown user type", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LoginActivity", "Failed to retrieve user type: ${error.message}")
                }
            })
        }
    }

    private fun checkIfDriverSuspended(userId: String) {
        val userRef = FirebaseDatabase.getInstance().reference.child("user").child(userId)
        userRef.child("isSuspended").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isSuspended = snapshot.getValue(Boolean::class.java) ?: false
                Log.d("LoginActivity", "Is Suspended: $isSuspended")
                if (isSuspended) {
                    val suspensionEndDate = snapshot.child("suspensionEndDate").getValue(Long::class.java)
                    Log.d("LoginActivity", "Suspension End Date: $suspensionEndDate")
                    if (suspensionEndDate != null) {
                        val daysRemaining = (suspensionEndDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)
                        Log.d("LoginActivity", "Days Remaining: $daysRemaining")
                        if (daysRemaining > 0) {
                            showSuspensionDialog(daysRemaining)
                        } else {
                            // If suspension time is over, continue to dashboard
                            startActivity(Intent(this@LoginActivity, DriverDashboard::class.java))
                            finish()
                        }
                    } else {
                        // Handle case where suspensionEndDate is null (show a fallback dialog or message)
                        showSuspensionDialog(-1) // -1 for unknown end date
                    }
                } else {
                    // If not suspended, continue to dashboard
                    startActivity(Intent(this@LoginActivity, DriverDashboard::class.java))
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LoginActivity", "Error checking suspension status: ${error.message}")
            }
        })
    }

    private fun showSuspensionDialog(daysRemaining: Long) {
        val intent = Intent(this, SuspensionActivity::class.java).apply {
            putExtra("DAYS_REMAINING", daysRemaining)
        }
        startActivity(intent)
        finish() // Optional: Close current activity
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
        // Inflate the custom layout for the dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_location_disabled, null)

        // Find the ImageView in the dialog layout
        val gifImageView = dialogView.findViewById<ImageView>(R.id.ivInternetGif)

        // Load the GIF into the ImageView using Glide
        Glide.with(this)
            .asGif() // Specify that you want to load a GIF
            .load(R.drawable.location) // Replace with your GIF resource or URL
            .into(gifImageView)

        // Build and show the dialog
        AlertDialog.Builder(this)
            .setTitle("Location Disabled")
            .setView(dialogView) // Set the custom view
            .setMessage("Please enable location services to continue.")
            .setPositiveButton("Enable Location") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Show dialog to enable Internet connection
    private fun showInternetDialog() {
        // Inflate the custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_internet_connection, null)

        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setView(dialogView)
            .setPositiveButton("Enable Internet") { _, _ ->
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Load the GIF into the ImageView using Glide
        val gifImageView: ImageView = dialogView.findViewById(R.id.gifImageView)
        Glide.with(this)
            .asGif()
            .load(R.drawable.nointernetdialog) // Replace with your GIF resource
            .into(gifImageView)

        // Show the dialog
        dialog.show()
    }
}
