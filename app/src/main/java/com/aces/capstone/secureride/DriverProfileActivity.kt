package com.aces.capstone.secureride.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData
import com.google.firebase.database.*

class DriverProfileActivity : AppCompatActivity() {

    private lateinit var btnAccount: TextView
    private lateinit var btnEmail: TextView
    private lateinit var btnAddress: TextView
    private lateinit var btnPhone: TextView
    private lateinit var btnBack: TextView
    private lateinit var driverImageView: ImageView // ImageView for the driver's image
    private lateinit var tricycleImageView: ImageView // ImageView for the tricycle image
    private lateinit var plateNumberImageView: ImageView // ImageView for the plate number image
    private lateinit var driversLicenseImageView: ImageView // ImageView for the driver's license image
    private lateinit var databaseReference: DatabaseReference // Firebase Database reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_driver)

        // Initialize views
        btnBack = findViewById(R.id.btnBack)
        btnAccount = findViewById(R.id.btnAccount)
        btnEmail = findViewById(R .id.btnEmail)
        btnAddress = findViewById(R.id.btnAddress)
        btnPhone = findViewById(R.id.btnPhone)
        driverImageView = findViewById(R.id.driverImageView) // Initialize the ImageView
        tricycleImageView = findViewById(R.id.tricycle_add) // Initialize the tricycle ImageView
        plateNumberImageView = findViewById(R.id.plate_number_add) // Initialize the plate number ImageView
        driversLicenseImageView = findViewById(R.id.drivers_license_add) // Initialize the driver's license ImageView

        // Get the user ID from the intent
        val userId = intent.getStringExtra("user_id")

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("user") // Adjust the path as needed

        // Fetch user data using userId
        userId?.let {
            fetchUserData(it)
        }

        btnBack.setOnClickListener {
            finish() // This will close the current activity and return to the previous one
        }
    }

    private fun fetchUserData(userId: String) {
        databaseReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val driver = snapshot.getValue(UserData::class.java)
                if (driver != null) {
                    // Update UI with driver details
                    btnAccount.text = "${driver.firstname} ${driver.lastname}" // Display full name
                    btnEmail.text = driver.email // Display email
                    btnAddress.text = driver.address // Display address
                    btnPhone.text = driver.phone

                    // Load the driver's profile image from Base64
                    driver.profileImage?.let { base64Image ->
                        val bitmap: Bitmap? = decodeBase64ToBitmap(base64Image)
                        if (bitmap != null) {
                            driverImageView.setImageBitmap(bitmap) // Set the Bitmap to ImageView
                        } else {
                            Toast.makeText(this@DriverProfileActivity, "Error loading profile image.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Load the tricycle image
                    driver.tricycleImage?.let { base64Image ->
                        val bitmap: Bitmap? = decodeBase64ToBitmap(base64Image)
                        if (bitmap != null) {
                            tricycleImageView.setImageBitmap(bitmap) // Set the Bitmap to tricycle ImageView
                        } else {
                            Toast.makeText(this@DriverProfileActivity, "Error loading tricycle image.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Load the plate number image
                    driver.plateNumberImage?.let { base64Image ->
                        val bitmap: Bitmap? = decodeBase64ToBitmap(base64Image)
                        if (bitmap != null) {
                            plateNumberImageView.setImageBitmap(bitmap) // Set the Bitmap to plate number ImageView
                        } else {
                            Toast.makeText(this@DriverProfileActivity, "Error loading plate number image.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Load the driver's license image
                    driver.licenceImage?.let { base64Image ->
                        val bitmap: Bitmap? = decodeBase64ToBitmap(base64Image)
                        if (bitmap != null) {
                            driversLicenseImageView.setImageBitmap(bitmap) // Set the Bitmap to driver's license ImageView
                        } else {
                            Toast.makeText(this@DriverProfileActivity, "Error loading driver's license image.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Handle case where driver data is null
                    Toast.makeText(this@DriverProfileActivity, "Driver data not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
                Toast.makeText(this@DriverProfileActivity, "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
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