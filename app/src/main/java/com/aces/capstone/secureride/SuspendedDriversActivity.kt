package com.aces.capstone.secureride.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData
import com.aces.capstone.secureride.adapter.DriverAdapter
import com.aces.capstone.secureride.databinding.ActivitySuspendedDriversBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SuspendedDriversActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuspendedDriversBinding
    private lateinit var driverAdapter: DriverAdapter
    private val suspendedDriverList = mutableListOf<UserData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuspendedDriversBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchSuspendedDrivers() // Reload driver details
            swipeRefreshLayout.isRefreshing = false // Stop the refreshing animation
        }

        // Initialize the RecyclerView with the DriverAdapter
        driverAdapter = DriverAdapter(suspendedDriverList, { driver ->
            // Handle unsuspend action
            unsuspendDriver(driver)
        })

        // Set up RecyclerView
        binding.recyclerViewSuspendedDrivers.apply {
            layoutManager = LinearLayoutManager(this@SuspendedDriversActivity)
            adapter = driverAdapter
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        // Fetch suspended drivers from Firebase
        fetchSuspendedDrivers()
    }

    private fun fetchSuspendedDrivers() {
        val suspendedDriversRef = FirebaseDatabase.getInstance().getReference("user").orderByChild("isSuspended").equalTo(true)

        suspendedDriversRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                suspendedDriverList.clear() // Clear the current list
                for (driverSnapshot in snapshot.children) {
                    val driver = driverSnapshot.getValue(UserData::class.java) // Get as UserData
                    if (driver != null) {
                        suspendedDriverList.add(driver) // Add the suspended driver to the list
                    }
                }
                driverAdapter.notifyDataSetChanged() // Notify adapter that the data has changed
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SuspendedDriversActivity, "Failed to load suspended drivers: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun unsuspendDriver(driver: UserData) {
        // Logic to unsuspend the driver
        val driverRef = FirebaseDatabase.getInstance().getReference("user").child(driver.uid ?: "")
        driverRef.child("isSuspended").setValue(false).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Driver ${driver.firstname} has been unsuspended.", Toast.LENGTH_SHORT).show()
                fetchSuspendedDrivers() // Refresh the list
            } else {
                Toast.makeText(this, "Failed to unsuspend driver: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}