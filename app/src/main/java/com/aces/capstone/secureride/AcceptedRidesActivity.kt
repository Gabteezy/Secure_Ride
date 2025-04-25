package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.adapter.AcceptedRidesAdapter
import com.aces.capstone.secureride.databinding.ActivityAcceptedRidesBinding
import com.aces.capstone.secureride.model.RideRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AcceptedRidesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAcceptedRidesBinding
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var acceptedRidesAdapter: AcceptedRidesAdapter
    private val acceptedRides = mutableListOf<RideRequest>()
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAcceptedRidesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        acceptedRidesAdapter = AcceptedRidesAdapter(this, acceptedRides) { rideRequest ->
            completeRide(rideRequest) // Pass the rideRequest to completeRide method
        }

        bottomNavigationView = binding.bottomNavView

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navDriverHome -> {
                    startActivity(Intent(this, DriverDashboard::class.java))
                    true
                }
                R.id.navAcceptedRides -> {
                    true
                }
                R.id.navDriverHistory -> {
                    // Start the DriverHistory activity
                    startActivity(Intent(this, DriverHistory::class.java))
                    true
                }
                R.id.navDriverProfile -> {
                    // Start the DriverProfile activity
                    startActivity(Intent(this, DriverProfile::class.java))
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.navAcceptedRides
        binding.acceptedRidesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.acceptedRidesRecyclerView.adapter = acceptedRidesAdapter

        fetchAcceptedRides()
    }

    private fun fetchAcceptedRides() {
        val userId = auth.currentUser ?.uid ?: return
        val acceptedRidesRef = firebaseDatabaseReference.child("ride_requests").orderByChild("driverId").equalTo(userId)

        acceptedRidesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                acceptedRides.clear()
                for (rideSnapshot in snapshot.children) {
                    val rideRequest = rideSnapshot.getValue(RideRequest::class.java)
                    rideRequest?.let {
                        if (it.status == "accepted") {
                            acceptedRides.add(it)
                        }
                    }
                }
                acceptedRidesAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AcceptedRidesActivity", "Failed to fetch accepted rides: ${error.message}")
                Toast.makeText(this@AcceptedRidesActivity, "Failed to load accepted rides.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun completeRide(rideRequest: RideRequest) {
        val currentDriverId = auth.currentUser ?.uid ?: return
        val driverRef = firebaseDatabaseReference.child("drivers").child(currentDriverId)

        // When ride is completed, decrease commuter count
        val commuterCount = rideRequest.commuterCount ?: 0
        saveCompletedRideToHistory(rideRequest)


        // Get the current accepted commuters count
        driverRef.child("acceptedCommutersCount").get().addOnSuccessListener { snapshot ->
            val currentCount = snapshot.getValue(Int::class.java) ?: 0

            // Decrease the commuter count
            driverRef.child("acceptedCommutersCount").setValue(currentCount - commuterCount).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this@AcceptedRidesActivity, "Ride completed!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AcceptedRidesActivity, "Failed to update commuter count after ride completion.", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { error ->
            Log.e("CompleteRide", "Failed to get accepted commuters count: ${error.message}")
            Toast.makeText(this@AcceptedRidesActivity, "Error fetching commuter count.", Toast.LENGTH_SHORT).show()
        }

        // Update the ride request status to 'completed'
        val rideRequestRef = firebaseDatabaseReference.child("ride_requests").child(rideRequest.id ?: return)
        rideRequestRef.child("status").setValue("completed").addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("CompleteRide", "Ride request status updated to completed.")
            } else {
                Log.e("CompleteRide", "Failed to update ride request status: ${task.exception?.message}")
            }
        }

        // Optionally, set the driver's isOnRide status to false
        driverRef.child("isOnRide").setValue(false).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("CompleteRide", "Driver is no longer on a ride.")
            } else {
                Log.e("CompleteRide", "Failed to update isOnRide status: ${task.exception?.message}")
            }
        }
    }


    private fun saveCompletedRideToHistory(rideRequest: RideRequest) {
        val historyRef = firebaseDatabaseReference.child("driver_history").child(auth.currentUser ?.uid ?: "")

        // Make sure rideRequest is valid and initialized properly
        if (rideRequest.id == null || rideRequest.firstName == null || rideRequest.lastName == null) {
            Log.e("DriverHistory", "Invalid ride request data!")
            return
        }

        val rideHistory: HashMap<String, Any?> = hashMapOf(
            "rideId" to rideRequest.id,
            "commuterName" to "${rideRequest.firstName} ${rideRequest.lastName}",
            "pickupLocation" to rideRequest.pickupLocation,
            "dropoffLocation" to rideRequest.dropoffLocation,
            "status" to "COMPLETED",
            "totalFare" to rideRequest.totalFare,
            "timestamp" to System.currentTimeMillis()
        )

        historyRef.push().setValue(rideHistory).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("DriverHistory", "Completed ride saved to history.")
            } else {
                Log.e("DriverHistory", "Failed to save completed ride to history.", task.exception)
            }
        }
    }
}