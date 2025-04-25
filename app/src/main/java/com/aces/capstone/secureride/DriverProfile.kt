package com.aces.capstone.secureride

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aces.capstone.secureride.databinding.ActivityDriverProfileBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DriverProfile : AppCompatActivity() {

    private lateinit var driver: UserData
    private lateinit var binding: ActivityDriverProfileBinding
    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var bottomNavigationView: BottomNavigationView
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isUserAuthenticated()) {
            redirectToLogin()
            return
        }

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            retrieveDriverDetails() // Reload driver details
            swipeRefreshLayout.isRefreshing = false // Stop the refreshing animation
        }

        retrieveDriverDetails()


        // Set up listeners
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.Logout.setOnClickListener {
            logout()
        }

        binding.btnSave.setOnClickListener {
            saveDriverDetails()
        }

    }

    private fun logout() {
        val userId = FirebaseAuth.getInstance().currentUser ?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("user").child(userId)

        // Set the driver's status to logged out
        userRef.child("isLoggedIn").setValue(false).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Optionally, you can also set isOnline to false
                userRef.child("isOnline").setValue(false).addOnCompleteListener { onlineTask ->
                    if (onlineTask.isSuccessful) {
                        // Sign out from Firebase Auth
                        FirebaseAuth.getInstance().signOut()
                        // Start the LoginActivity
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Log.e("Logout", "Failed to update online status: ${onlineTask.exception?.message}")
                    }
                }
            } else {
                Log.e("Logout", "Failed to update logged in status: ${task.exception?.message}")
            }
        }
    }

    private fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser  != null
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity ::class.java))
        finish() // Close the current activity
    }



    private fun retrieveDriverDetails() {
        Log.d("DASHBOARD", "CHECKING USER RECORD FROM FIREBASE")

        val databaseRef = firebaseDatabase.reference.child("user").child(firebaseAuth.currentUser !!.uid)

        databaseRef.get().addOnCompleteListener { dataSnapshot ->
            if (dataSnapshot.isSuccessful) {
                driver = dataSnapshot.result.getValue(UserData::class.java)!!
                if (driver != null) {
                    Log.d("DASHBOARD", "USER FOUND \n ${driver.toString()}")
                    binding.firstName.setText(driver.firstname)
                    binding.lastName.setText(driver.lastname)
                    binding.email.setText(driver.email)
                    binding.phone.setText(driver.phone)
                    binding.address.setText(driver.address)
                    driver.profileImage?.let {
                        binding.profileImage.setImageBitmap(decodeBase64ToBitmap(it))
                    }

                    // Fetch ratings for the driver
                    fetchDriverRatings(driver.uid) // Assuming driver.id is the unique identifier for the driver
                }
            } else {
                Log.d("USER_DETAILS_NOT_FOUND", "USER NOT FOUND")
            }
        }
    }

    private fun fetchDriverRatings(driverId: String?) {
        if (driverId == null) return

        val ratingsRef = firebaseDatabase.reference.child("drivers").child(driverId)

        ratingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if the ratings exist and are of the correct type
                val ratings = snapshot.child("averageRating").getValue(Double::class.java) ?: 0.0
                val totalRatings = snapshot.child("averageRating").getValue(Int::class.java) ?: 0

                // Set the rating bar value
                binding.ratingBar.rating = ratings.toFloat() // Convert to Float for RatingBar

                // Display total ratings if needed
                binding.totalRatings.text = "Total Ratings: $totalRatings"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverProfile", "Failed to fetch driver ratings: ${error.message}")
                Toast.makeText(this@DriverProfile, "Failed to fetch driver ratings.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveDriverDetails() {
        val firstname = binding.firstName.text.toString()
        val lastname = binding.lastName.text.toString()
        val email = binding.email.text.toString()
        val phone = binding.phone.text.toString()
        val address = binding.address.text.toString()

        // Update user data in Firebase
        val driverUpdates = mapOf(
            "firstname" to firstname,
            "lastname" to lastname,
            "email" to email,
            "phone" to phone,
            "address" to address
        )

        val databaseRef = firebaseDatabase.reference.child("user")
            .child(firebaseAuth.currentUser!!.uid)

        databaseRef.updateChildren(driverUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "User details updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to update user details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun decodeBase64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrEmpty()) {
            Log.e("DecodeError", "Base64 string is null or empty")
            return null
        }
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("DecodeError", "Error decoding Base64 to Bitmap", e)
            null
        }
    }
}
