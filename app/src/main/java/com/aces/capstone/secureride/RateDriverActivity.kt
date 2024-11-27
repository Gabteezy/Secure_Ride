package com.aces.capstone.secureride

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityRateDriverBinding
import com.google.firebase.database.FirebaseDatabase

class RateDriverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRateDriverBinding
    private lateinit var rideRequestId: String
    private lateinit var driverName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRateDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rideRequestId = intent.getStringExtra("rideRequestId") ?: ""
        driverName = intent.getStringExtra("driverName") ?: ""

        binding.driverNameTextView.text = driverName

        binding.submitRatingButton.setOnClickListener {
            val rating = binding.ratingBar.rating
            saveRatingToDatabase(rideRequestId, rating)
        }
    }

    private fun saveRatingToDatabase(rideRequestId: String, rating: Float) {
        val ratingRef = FirebaseDatabase.getInstance().getReference("ratings").child(rideRequestId)
        ratingRef.setValue(rating).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Rating submitted successfully!", Toast.LENGTH_SHORT).show()
                finish() // Close the activity after submitting the rating
            } else {
                Toast.makeText(this, "Failed to submit rating. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}