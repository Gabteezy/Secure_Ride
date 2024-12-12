package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.adapter.RideHistoryAdapter
import com.aces.capstone.secureride.databinding.ActivityUserHistoryBinding
import com.aces.capstone.secureride.model.RideHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserHistory : AppCompatActivity() {
    private lateinit var binding: ActivityUserHistoryBinding
    private lateinit var rideHistoryAdapter: RideHistoryAdapter
    private val rideHistoryList = mutableListOf<RideHistory>()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth
        setupRecyclerView()
        fetchUserHistory()

        binding.bottomNavView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navCommuterHistory -> {
                    true
                }
                R.id.navCommuterHome -> {
                    // Navigate to DriverDashboard
                    val intent = Intent(this@UserHistory, UserDashboard::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navCommuterProfile -> {
                    // Navigate to DriverDashboard
                    val intent = Intent(this@UserHistory, UserProfile::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

    }

    private fun setupRecyclerView() {
        rideHistoryAdapter = RideHistoryAdapter(rideHistoryList)
        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewHistory.adapter = rideHistoryAdapter
    }

    private fun fetchUserHistory() {
        val currentUser  = auth.currentUser  ?: return
        val historyRef = FirebaseDatabase.getInstance().reference.child("user_history").child(currentUser .uid)

        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rideHistoryList.clear()
                for (childSnapshot in snapshot.children) {
                    val rideHistory = childSnapshot.getValue(RideHistory::class.java)
                    rideHistory?.let { rideHistoryList.add(it) }
                }
                rideHistoryAdapter.notifyDataSetChanged() // Notify adapter of data change
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("User History", "Failed to fetch user history: ${error.message}")
            }
        })
    }
}