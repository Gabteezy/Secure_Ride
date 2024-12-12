package com.aces.capstone.secureride.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.DriverProfile
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.adapter.DriverAdapter
import com.aces.capstone.secureride.databinding.ActivityListOfDriversBinding
import com.aces.capstone.secureride.UserData
import com.google.firebase.database.*

class ListOfDriversActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListOfDriversBinding
    private lateinit var driverAdapter: DriverAdapter
    private val driverList = mutableListOf<UserData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListOfDriversBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the RecyclerView with the DriverAdapter
        driverAdapter = DriverAdapter(driverList, { driver ->
            // Handle edit action
            editDriver(driver)
        }, { driver ->
            // Handle delete action
            deleteDriver(driver)
        })

        // Set up RecyclerView
        binding.recyclerViewDrivers.apply {
            layoutManager = LinearLayoutManager(this@ListOfDriversActivity)
            adapter = driverAdapter
        }

        // Fetch drivers from Firebase
        fetchDrivers()

        // Back button action
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchDrivers() {
        val driversRef = FirebaseDatabase.getInstance().getReference("user").orderByChild("type").equalTo("Driver")

        driversRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                driverList.clear() // Clear the current list
                for (driverSnapshot in snapshot.children) {
                    val driver = driverSnapshot.getValue(UserData::class.java) // Get as UserData
                    if (driver != null) {
                        driverList.add(driver) // Add the driver to the list
                    }
                }
                driverAdapter.notifyDataSetChanged() // Notify adapter that the data has changed
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListOfDriversActivity, "Failed to load drivers: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun editDriver(driver: UserData) {
        // This listens for clicks on the edit icon in the RecyclerView item view
        val intent = Intent(this@ListOfDriversActivity, DriverProfile::class.java)
        intent.putExtra("driver_id", driver.uid)  // Pass the driver ID to the profile screen

        // Start the activity when the edit icon is clicked
        startActivity(intent)

        // Display a Toast message for feedback
        Toast.makeText(this, "Edit driver: ${driver.firstname} ${driver.lastname}", Toast.LENGTH_SHORT).show()
    }

    private fun deleteDriver(driver: UserData) {
        // Implement delete functionality
        Toast.makeText(this, "Delete driver: ${driver.firstname} ${driver.lastname}", Toast.LENGTH_SHORT).show()
    }
}