package com.aces.capstone.secureride

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.model.Driver
import com.bumptech.glide.Glide
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.lang.Exception

class DriverProfile : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private lateinit var profileImageView: ImageView
    private lateinit var nameEditText: EditText
    private lateinit var plateNumberEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var ratingEditText: EditText
    private lateinit var emergencyEditText: EditText
    private lateinit var logoutButton: Button
    private lateinit var editButton: Button
    private lateinit var saveButton: Button

    private var driverId: String? = null
    private lateinit var database: DatabaseReference
    private lateinit var storage: StorageReference
    private lateinit var driverRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_profile)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference
        storage = FirebaseStorage.getInstance().reference

        profileImageView = findViewById(R.id.profile_picture)
        nameEditText = findViewById(R.id.name)
        plateNumberEditText = findViewById(R.id.plate_number)
        phoneEditText = findViewById(R.id.phone)
        emailEditText = findViewById(R.id.email)
        addressEditText = findViewById(R.id.current_address)
        ratingEditText = findViewById(R.id.rating)
        emergencyEditText = findViewById(R.id.emergency_number)
        logoutButton = findViewById(R.id.logout_button)
        editButton = findViewById(R.id.edit_button)
        saveButton = findViewById(R.id.save_button)

        driverId = intent.getStringExtra("driver_id")
        driverRef = database.child("drivers").child(driverId.toString())

        // Fetch driver profile details
        fetchDriverDetails()

        // Set up image picker
        profileImageView.setOnClickListener {
            openGallery()
        }

        // Handle logout action
        logoutButton.setOnClickListener {
            // Your logout logic here
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
        }

        // Handle Edit button click
        editButton.setOnClickListener {
            toggleEditing(true)
        }

        // Handle Save button click
        saveButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun fetchDriverDetails() {
        driverRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val driver = snapshot.getValue(Driver::class.java)
                driver?.let {
                    nameEditText.setText(it.name)
                    plateNumberEditText.setText(it.plateNumber)
                    phoneEditText.setText(it.phone)
                    emailEditText.setText(it.email)
                    addressEditText.setText(it.currentAddress)
                    ratingEditText.setText(it.rating)
                    emergencyEditText.setText(it.emergencyNumber)

                    // Load profile image
                    if (it.profilePicture.isNotEmpty()) {
                        Glide.with(this)
                            .load(it.profilePicture)
                            .into(profileImageView)
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch driver details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleEditing(enable: Boolean) {
        nameEditText.isEnabled = enable
        plateNumberEditText.isEnabled = enable
        phoneEditText.isEnabled = enable
        emailEditText.isEnabled = enable
        addressEditText.isEnabled = enable
        ratingEditText.isEnabled = enable
        emergencyEditText.isEnabled = enable
    }

    private fun saveProfile() {
        val updatedDriver = Driver(
            driverId = driverId.toString(),
            name = nameEditText.text.toString(),
            plateNumber = plateNumberEditText.text.toString(),
            phone = phoneEditText.text.toString(),
            email = emailEditText.text.toString(),
            currentAddress = addressEditText.text.toString(),
            rating = ratingEditText.text.toString(),
            emergencyNumber = emergencyEditText.text.toString(),
            profilePicture = "" // Handle image saving logic separately
        )

        driverRef.setValue(updatedDriver).addOnSuccessListener {
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            toggleEditing(false)  // Disable editing after save
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
        }
    }

    // Add the gallery image picker logic as previously shown
}
