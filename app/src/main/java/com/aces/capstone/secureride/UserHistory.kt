    package com.aces.capstone.secureride
    
    import android.content.Intent
    import android.os.Bundle
    import android.util.Log
    import android.view.View
    import android.widget.AdapterView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.recyclerview.widget.LinearLayoutManager
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.database.*
    import com.aces.capstone.secureride.model.RideHistory
    import com.aces.capstone.secureride.adapter.RideHistoryAdapter
    import com.aces.capstone.secureride.databinding.ActivityUserHistoryBinding // Import the generated binding class
    
    class UserHistory : AppCompatActivity() {
    
        private lateinit var auth: FirebaseAuth
        private lateinit var firebaseDatabaseReference: DatabaseReference
        private lateinit var rideHistoryAdapter: RideHistoryAdapter
        private val rideHistoryList = mutableListOf<RideHistory>()
        private var rideHistoryListener: ValueEventListener? = null
        private lateinit var binding: ActivityUserHistoryBinding // Declare the binding variable
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityUserHistoryBinding.inflate(layoutInflater)
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
    
            // Bottom Navigation Setup (unchanged)
            binding.bottomNavView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navCommuterHistory -> {
                        true
                    }
                    R.id.navCommuterHome -> {
                        val intent = Intent(this@UserHistory, UserDashboard::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.navCommuterProfile -> {
                        val intent = Intent(this@UserHistory, UserProfile::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
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

            val historyRef = firebaseDatabaseReference.child("user_history").child(userId)

            // Remove the existing listener to prevent multiple listeners
            rideHistoryListener?.let {
                historyRef.removeEventListener(it)
            }

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

                    // Sort the rideHistoryList by timestamp (or rideDate) in descending order (most recent first)
                    rideHistoryList.sortByDescending { it.timestamp }

                    // Notify the adapter of the updated list
                    rideHistoryAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserHistory", "Failed to fetch ride history: ${error.message}")
                    Toast.makeText(this@UserHistory, "Failed to fetch ride history.", Toast.LENGTH_SHORT).show()
                }
            }

            // Add the listener
            historyRef.addValueEventListener(rideHistoryListener!!)
        }




        override fun onDestroy() {
            super.onDestroy()
            // Remove listener to prevent memory leaks
            rideHistoryListener?.let { firebaseDatabaseReference.child("user_history").child(auth.currentUser ?.uid ?: "").removeEventListener(it) }
        }
    }