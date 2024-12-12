package com.aces.capstone.secureride

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aces.capstone.secureride.databinding.ActivityDriverProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class DriverProfile : AppCompatActivity() {

    private lateinit var driver: UserData
    private lateinit var binding: ActivityDriverProfileBinding
    private var firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            retrieveDriverDetails() // Reload driver details
            swipeRefreshLayout.isRefreshing = false // Stop the refreshing animation
        }

        retrieveDriverDetails()

        // Set up listeners
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this@DriverProfile, DriverDashboard::class.java))
        }
        binding.Logout.setOnClickListener {
            val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.clear() // Clear all stored user session data
            editor.apply() // Apply the changes

            // Optionally clear user data (if needed)

            startActivity(Intent(this@DriverProfile, LoginUser::class.java))
        }
        binding.profileImage.setOnClickListener {
            openGallery()
        }

        binding.btnSave.setOnClickListener {
            saveDriverDetails()
        }

        binding.ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            if (fromUser) {
                submitRatingToFirebase(rating)
            }
        }
    }

    private fun submitRatingToFirebase(rating: Float) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val driverId = currentUser.uid
        val databaseRef = firebaseDatabase.reference.child("drivers").child(driverId)

        databaseRef.child("ratings").push().setValue(rating)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Rating submitted", Toast.LENGTH_SHORT).show()
                    calculateAverageRating()
                } else {
                    Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun calculateAverageRating() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val driverId = currentUser.uid
        val ratingsRef = firebaseDatabase.reference.child("drivers").child(driverId).child("ratings")

        ratingsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val ratingsSnapshot = task.result
                var totalRating = 0f
                var count = 0

                for (ratingSnapshot in ratingsSnapshot.children) {
                    val rating = ratingSnapshot.getValue(Float::class.java) ?: continue
                    totalRating += rating
                    count++
                }

                val averageRating = if (count > 0) totalRating / count else 0f
                binding.totalRatings.text = "Total Ratings: $averageRating"
            } else {
                Log.e("DRIVER_PROFILE", "Failed to calculate average rating", task.exception)
            }
        }
    }

    private fun retrieveDriverDetails() {
        Log.d("DASHBOARD", "CHECKING USER RECORD FROM FIREBASE")

        val databaseRef = firebaseDatabase.reference.child("user")
            .child(firebaseAuth.currentUser !!.uid)

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
                    binding.plateNumber.setText(driver.platenumber)
                    binding.license.setText(driver.licenceImage)

                    // Load the existing profile image if it's a Base64 string
                    val profileImage = driver.profileImage
                    if (!profileImage.isNullOrEmpty()) {
                        val bitmap = decodeBase64ToBitmap(profileImage)
                        binding.profileImage.setImageBitmap(bitmap)
                    }
                }
            } else {
                Log.d("USER_DETAILS_NOT_FOUND", "USER NOT FOUND")
            }
        }
    }

    private fun saveDriverDetails() {
        val firstname = binding.firstName.text.toString()
        val lastname = binding.lastName.text.toString()
        val email = binding.email.text.toString()
        val phone = binding.phone.text.toString()
        val address = binding.address.text.toString()
        val plateNumber = binding.plateNumber.text.toString()
        val licenceImage = binding.license.text.toString()

        // Update user data in Firebase
        val driverUpdates = mapOf(
            "firstname" to firstname,
            "lastname" to lastname,
            "email" to email,
            "phone" to phone,
            "address" to address,
            "platenumber" to plateNumber,
            "licenceImage" to licenceImage
        )

        val databaseRef = firebaseDatabase.reference.child("user")
            .child(firebaseAuth.currentUser !!.uid)

        databaseRef.updateChildren(driverUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "User details updated successfully", Toast.LENGTH_SHORT).show()
                // Upload the image as Base64 if a new image is selected
                if (selectedImageUri != null) {
                    uploadImageAsBase64()
                }
            } else {
                Toast.makeText(this, "Failed to update user details", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data
            binding.profileImage.setImageURI(selectedImageUri) // Display the selected image
        }
    }

    private fun uploadImageAsBase64() {
        selectedImageUri?.let { uri ->
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val base64Image = encodeImageToBase64(bitmap)

            val userUpdates = mapOf(
                "profileImage" to base64Image  // Store the Base64 string in the user's data
            )

            val databaseRef = firebaseDatabase.reference.child("user")
                .child(firebaseAuth.currentUser !!.uid)

            databaseRef.updateChildren(userUpdates).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Profile image updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update profile image", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}
