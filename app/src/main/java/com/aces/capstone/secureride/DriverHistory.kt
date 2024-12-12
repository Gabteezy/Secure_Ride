package com.aces.capstone.secureride

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.adapter.RideHistoryAdapter
import com.aces.capstone.secureride.databinding.ActivityDriverDashboardBinding
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

    private lateinit var binding: ActivityDriverHistoryBinding

    private lateinit var firebaseDatabaseReference: DatabaseReference
    private lateinit var rideHistoryAdapter: RideHistoryAdapter
    private val rideHistoryList = mutableListOf<RideHistory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize binding here
        binding = ActivityDriverHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)  // Use the root view from the binding

        firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
        rideHistoryAdapter = RideHistoryAdapter(rideHistoryList)

        // RecyclerView setup
        val recyclerView = binding.recyclerViewHistory
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = rideHistoryAdapter

        // BottomNavigationView setup
        binding.bottomNavView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navDriverHistory -> {
                    true
                }
                R.id.navDriverHome -> {
                    // Navigate to DriverDashboard
                    val intent = Intent(this@DriverHistory, DriverDashboard::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        fetchRideHistory()
    }

    private fun fetchRideHistory() {
        val historyRef = firebaseDatabaseReference.child("driver_history").child(FirebaseAuth.getInstance().currentUser?.uid ?: "")
        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                rideHistoryList.clear()
                for (historySnapshot in snapshot.children) {
                    val rideHistory = historySnapshot.getValue(RideHistory::class.java)
                    rideHistory?.let {
                        rideHistoryList.add(it)
                    }
                }
                rideHistoryAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverHistory", "Failed to load history: ${error.message}")
                Toast.makeText(this@DriverHistory, "Failed to load history.", Toast.LENGTH_SHORT).show()
            }
        })
    }

}