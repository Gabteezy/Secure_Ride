package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.adapter.RideHistoryAdapter
import com.aces.capstone.secureride.databinding.ActivityDriverHistoryBinding
import com.aces.capstone.secureride.model.RideHistory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DriverHistory : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var rideHistoryAdapter: RideHistoryAdapter
    private val rideHistoryList = mutableListOf<RideHistory>()
    private lateinit var bottomNavigationView: BottomNavigationView
    private var rideHistoryListener: ValueEventListener? = null
    private lateinit var binding: ActivityDriverHistoryBinding // Declare the binding variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance()
        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference

        // Set up RecyclerView
        setupRecyclerView()

        // Fetch ride history
        fetchRideHistory("All") // Initially show all rides

        // Set up the Spinner to handle filter selection
        binding.statusFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = parent?.getItemAtPosition(position).toString()
                fetchRideHistory(selectedStatus) // Fetch rides based on the selected filter
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

        bottomNavigationView = binding.bottomNavView
        // Bottom Navigation Setup (unchanged)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navDriverHome -> {
                    // Start the DriverDashboard activity
                    startActivity(Intent(this, DriverDashboard::class.java))
                    true
                }
                R.id.navAcceptedRides -> {
                    startActivity(Intent(this, AcceptedRidesActivity::class.java))
                    true
                }
                R.id.navDriverHistory -> {
                    // Stay in this activity
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

        // Set the selected item as History
        bottomNavigationView.selectedItemId = R.id.navDriverHistory
    }

    private fun setupRecyclerView() {
        rideHistoryAdapter = RideHistoryAdapter(rideHistoryList) // Initialize your adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this) // Use the binding to access the RecyclerView
        binding.recyclerView.adapter = rideHistoryAdapter
    }

    private fun fetchRideHistory(statusFilter: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val historyRef = firebaseDatabaseReference.child("driver_history").child(userId)
        rideHistoryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rideHistoryList.clear()
                for (historySnapshot in snapshot.children) {
                    val rideHistory = historySnapshot.getValue(RideHistory::class.java)
                    if (rideHistory != null) {
                        if (statusFilter == "All" || rideHistory.status == statusFilter) {
                            rideHistoryList.add(rideHistory)
                        }
                    } else {
                        Log.e("UserHistory", "Failed to deserialize RideHistory for snapshot: $historySnapshot")
                    }
                }

                // Sort the rideHistoryList by timestamp in descending order (most recent first)
                rideHistoryList.sortByDescending { it.timestamp }  // Use the timestamp field for sorting

                // Notify the adapter of the updated list
                rideHistoryAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserHistory", "Failed to fetch ride history: ${error.message}")
                Toast.makeText(this@DriverHistory, "Failed to fetch ride history.", Toast.LENGTH_SHORT).show()
            }
        }
        historyRef.addValueEventListener(rideHistoryListener!!)
    }



    override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        rideHistoryListener?.let { firebaseDatabaseReference.child("driver_history").child(auth.currentUser ?.uid ?: "").removeEventListener(it) }
    }
}