package com.aces.capstone.secureride

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityLoginUserBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class LoginUser  : AppCompatActivity() {

    private lateinit var binding: ActivityLoginUserBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var firebaseDatabaseReference: DatabaseReference = FirebaseDatabase.getInstance()
        .getReferenceFromUrl("https://ride-e16d9-default-rtdb.firebaseio.com/")
    private lateinit var credential: AuthCredential
    private var userType = "UNKNOWN"

    companion object {
        private const val TAG = "LoginUser "
        private const val RC_SIGN_IN = 9001
        private const val PREFS_NAME = "SecureRidePrefs"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REMEMBER_ME = "remember_me"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load saved credentials
        loadSavedCredentials()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.app_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check for Internet and Location Services
        if (!isInternetAvailable()) {
            showNoInternetDialog()
        } else if (!isLocationEnabled()) {
            showNoLocationDialog()
        } else {
            setupButtonClickListeners()
        }
    }

    private fun loadSavedCredentials() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val email = sharedPreferences.getString(KEY_EMAIL, "")
        val password = sharedPreferences.getString(KEY_PASSWORD, "")
        val rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false)

        if (rememberMe) {
            binding.username.setText(email)
            binding.passWord.setText(password)
            binding.rememberMeCheckBox.isChecked = true
        }
    }

    private fun setupButtonClickListeners() {
        binding.registerAs.setOnClickListener {
            startActivity(Intent(this, RegisterAs::class.java))
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.username.text.toString().trim()
            val password = binding.passWord.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                handleEmptyFields()
            } else if (!ValidEmail(username)) {
                binding.username.error = "Please enter a valid email address."
            } else {
                signInWithEmailAndPassword(username, password)
            }
        }

        binding.btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun signInWithEmailAndPassword(username: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE // Show progress bar
        firebaseAuth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE // Hide progress bar
                if (task.isSuccessful) {
                    Log.d(TAG, "Successfully logged in")
                    if (binding.rememberMeCheckBox.isChecked) {
                        saveCredentials(username, password)
                    }
                    checkUserAccount()
                } else {
                    Log.e(TAG, "Login failed", task.exception)
                    Toast.makeText(this, "Invalid username or password! Please try again !", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveCredentials(email: String, password: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
            putBoolean(KEY_REMEMBER_ME, true)
            apply()
        }
    }

    private fun handleEmptyFields() {
        if (binding.username.text!!.isEmpty()) {
            binding.username.error = "Please enter a valid email."
        }
        if (binding.passWord.text!!.isEmpty()) {
            binding.passWord.error = "Please enter a password."
        }
        Toast.makeText(this, "Please check the error(s)!", Toast.LENGTH_SHORT).show()
    }

    private fun ValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Google sign-in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener {
                Log.d(TAG, "Google sign-in successful")
                checkUserAccount()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Google sign-in failed", e)
            }
    }

    private fun checkUserAccount() {
        binding.progressBar.visibility = View.VISIBLE
        Log.d(TAG, "Checking user account...")
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val userRef = firebaseDatabaseReference.child("user").child(uid)

            // Fetch the user data
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.progressBar.visibility = View.GONE
                    if (snapshot.exists()) {
                        val isSuspended = snapshot.child("isSuspended").getValue(Boolean::class.java) ?: false
                        val suspensionEndDate = snapshot.child("suspensionEndDate").getValue(Long::class.java) ?: 0L

                        if (isSuspended) {
                            // Calculate remaining suspension days
                            val currentTime = System.currentTimeMillis()
                            val remainingTime = suspensionEndDate - currentTime
                            if (remainingTime > 0) {
                                val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingTime)
                                showSuspensionDialog(remainingDays)
                            } else {
                                // Suspension period over, clear suspension
                                userRef.child("isSuspended").setValue(false)
                                userRef.child("suspensionEndDate").removeValue()
                                navigateToDashboard(snapshot)
                            }
                        } else {
                            // No suspension, navigate to dashboard
                            navigateToDashboard(snapshot)
                        }
                    } else {
                        Toast.makeText(this@LoginUser, "User data not found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.progressBar.visibility = View.GONE
                    Log.e(TAG, "Failed to fetch user data: ${error.message}")
                    Toast.makeText(this@LoginUser, "Failed to fetch user data.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun navigateToDashboard(snapshot: DataSnapshot) {
        // Attempt to get the userType from the snapshot
        val userType = snapshot.child("userType").getValue(String::class.java)

        if (userType != null && userType.isNotEmpty()) {
            // Navigate based on the retrieved userType
            when (userType) {
                "Driver" -> {
                    startActivity(Intent(this, DriverDashboard::class.java))
                    finish()
                }
                "Commuter" -> {
                    startActivity(Intent(this, UserDashboard::class.java))
                    finish()
                }
                "Admin" -> {
                    startActivity(Intent(this, AdminDashboard::class.java))
                    finish()
                }
                else -> {
                    // Log and show a message if userType doesn't match expected values
                    Log.e("navigateToDashboard", "Unknown user type: $userType")
                    Toast.makeText(this, "Unknown user type!", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Handle case where userType is null or empty
            Log.e("navigateToDashboard", "Failed to retrieve userType: $userType")
            Toast.makeText(this, "Failed to retrieve user type!", Toast.LENGTH_SHORT).show()
        }
    }




    private fun showSuspensionDialog(daysRemaining: Long) {
        val intent = Intent(this, SuspensionActivity::class.java).apply {
            putExtra("DAYS_REMAINING", daysRemaining)
        }
        startActivity(intent)
        finish() // Optional: Close current activity
    }


    private fun logged(userType: String) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = View.GONE
            val intent = when (userType) {
                "Driver" -> Intent(this, DriverDashboard::class.java)
                "Commuter" -> Intent(this, UserDashboard::class.java)
                "Admin" -> Intent(this, AdminDashboard::class.java)
                else -> Intent(this, AdminDashboard::class.java)
            }
            startActivity(intent)
            finish()
        }, 3000)
    }

    // Check if Internet is available
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    // Check if Location services are enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Show dialog when no internet connection
    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage("Please connect to the internet to proceed.")
            .setPositiveButton("Enable Internet") { _, _ ->
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Show dialog when no location service
    private fun showNoLocationDialog() {
        AlertDialog.Builder(this)
            .setTitle("No Location Service")
            .setMessage("Please enable location services to proceed.")
            .setPositiveButton("Enable Location") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}