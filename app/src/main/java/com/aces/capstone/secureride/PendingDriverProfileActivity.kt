package com.aces.capstone.secureride.ui

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData
import com.google.firebase.database.*

class PendingDriverProfileActivity : AppCompatActivity() {

    private lateinit var btnAccount: TextView
    private lateinit var btnEmail: TextView
    private lateinit var btnAddress: TextView
    private lateinit var btnPhone: TextView
    private lateinit var btnBack: TextView
    private lateinit var driverImageView: ImageView
    private lateinit var tricycleImageView: ImageView
    private lateinit var plateNumberImageView: ImageView
    private lateinit var driversLicenseImageView: ImageView
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_driver_profile)

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        btnAccount = findViewById(R.id.btnAccount)
        btnEmail = findViewById(R.id.btnEmail)
        btnAddress = findViewById(R.id.btnAddress)
        btnPhone = findViewById(R.id.btnPhone)
        driverImageView = findViewById(R.id.driverImageView)
        tricycleImageView = findViewById(R.id.tricycle_add)
        plateNumberImageView = findViewById(R.id.plate_number_add)
        driversLicenseImageView = findViewById(R.id.drivers_license_add)

        // Get the user ID from the intent
        val userId = intent.getStringExtra("user_id")

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("pending_drivers")

        // Fetch user data using userId
        userId?.let {
            fetchUserData(it)
        }

        // Set click listeners for images
        setupImageClickListeners()

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupImageClickListeners() {
        driverImageView.setOnClickListener {
            showFullScreenImage(driverImageView.drawable?.toBitmap())
        }
        tricycleImageView.setOnClickListener {
            showFullScreenImage(tricycleImageView.drawable?.toBitmap())
        }
        plateNumberImageView.setOnClickListener {
            showFullScreenImage(plateNumberImageView.drawable?.toBitmap())
        }
        driversLicenseImageView.setOnClickListener {
            showFullScreenImage(driversLicenseImageView.drawable?.toBitmap())
        }
    }

    private fun showFullScreenImage(bitmap: Bitmap?) {
        if (bitmap == null) {
            Toast.makeText(this, "No image available", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a dialog for fullscreen image
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.activity_full_screen_image)

        // Find the ImageView and close button in the dialog layout
        val fullScreenImageView = dialog.findViewById<ImageView>(R.id.fullScreenImageView)
        val closeButton = dialog.findViewById<ImageView>(R.id.closeButton)

        // Set the bitmap to the ImageView
        fullScreenImageView.setImageBitmap(bitmap)

        // Close the dialog when the close button is clicked
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        // Allow clicking the image to dismiss the dialog (optional)
        fullScreenImageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun fetchUserData(userId: String) {
        databaseReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driver = snapshot.getValue(UserData::class.java)
                if (driver != null) {
                    // Update UI with driver details
                    btnAccount.text = "${driver.firstname} ${driver.lastname}"
                    btnEmail.text = driver.email
                    btnAddress.text = driver.address
                    btnPhone.text = driver.phone

                    // Load the driver's profile image from Base64
                    driver.profileImage?.let { base64Image ->
                        val bitmap: Bitmap? = decodeBase64ToBitmap(base64Image)
                        if (bitmap != null) {
                            driverImageView.setImageBitmap(bitmap)
                        } else {
                            Toast.makeText(this@PendingDriverProfileActivity, "Error loading profile image.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Load the tricycle image
                    driver.tricycleImage?.let { base64Image ->
                        val bitmap: Bitmap? = decodeBase64ToBitmap(base64Image)
                        if (bitmap != null) {
                            tricycleImageView.setImageBitmap(bitmap)
                        } else {
                            Toast.makeText(this@PendingDriverProfileActivity, "Error loading tricycle image.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Load the plate number image
                    driver.plateNumberImage?.let { base64Image ->
                        val bitmap: Bitmap? = decodeBase64ToBitmap(base64Image)
                        if (bitmap != null) {
                            plateNumberImageView.setImageBitmap(bitmap)
                        } else {
                            Toast.makeText(this@PendingDriverProfileActivity, "Error loading plate number image.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Load the driver's license image
                    driver.licenceImage?.let { base64Image ->
                        val bitmap: Bitmap? = decodeBase64ToBitmap(base64Image)
                        if (bitmap != null) {
                            driversLicenseImageView.setImageBitmap(bitmap)
                        } else {
                            Toast.makeText(this@PendingDriverProfileActivity, "Error loading driver's license image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this@PendingDriverProfileActivity, "Driver data not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PendingDriverProfileActivity, "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}