package com.aces.capstone.secureride.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData
import com.aces.capstone.secureride.adapter.DriverAdapter
import com.aces.capstone.secureride.databinding.ActivityListOfDriversBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class ListOfDriversActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListOfDriversBinding
    private lateinit var driverAdapter: DriverAdapter
    private val driverList = mutableListOf<UserData>() // List of active drivers
    private lateinit var auth: FirebaseAuth
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListOfDriversBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchDrivers() // Reload driver details
            swipeRefreshLayout.isRefreshing = false // Stop the refreshing animation
        }

        auth = FirebaseAuth.getInstance()

        // Initialize the RecyclerView with the DriverAdapter
        driverAdapter = DriverAdapter(driverList, { driver ->
            suspendDriver(driver) // Handle suspend action
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

        // Navigate to Suspended Drivers Activity
        binding.buttonViewSuspendedDrivers.setOnClickListener {
            val intent = Intent(this, SuspendedDriversActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchDrivers() {
        val driversRef = FirebaseDatabase.getInstance().getReference("user")
            .orderByChild("userType")
            .equalTo("Driver")

        driversRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                driverList.clear() // Clear the current list
                for (driverSnapshot in snapshot.children) {
                    val driver = driverSnapshot.getValue(UserData::class.java) // Get as UserData
                    if (driver != null && (driver.isSuspended == null || !driver.isSuspended)) {
                        driverList.add(driver) // Add only non-suspended drivers
                    }
                }
                driverAdapter.notifyDataSetChanged() // Notify adapter that the data has changed
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListOfDriversActivity, "Failed to load drivers: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun suspendDriver(driver: UserData) {
        // Fetch the latest online status directly from the database
        val driverRef = FirebaseDatabase.getInstance().getReference("user").child(driver.uid ?: "")

        driverRef.child("isOnline").get().addOnSuccessListener { dataSnapshot ->
            val isOnline = dataSnapshot.getValue(Boolean::class.java) ?: false

            if (isOnline) {
                Toast.makeText(this, "Driver ${driver.firstname} is currently online and cannot be suspended.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener // Return early and don't proceed with suspension if the driver is online
            }

            // Proceed with suspension only if the driver is not online
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, 2) // Set suspension period to 2 days from now
            val suspensionEndDate = calendar.timeInMillis

            driverRef.child("isSuspended").setValue(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    driverRef.child("suspensionEndDate").setValue(suspensionEndDate).addOnCompleteListener { subTask ->
                        if (subTask.isSuccessful) {
                            Toast.makeText(this, "Driver ${driver.firstname} has been suspended.", Toast.LENGTH_SHORT).show()

                            // Immediately remove the suspended driver from the list and update the UI
                            driverList.remove(driver)
                            driverAdapter.notifyDataSetChanged() // Notify adapter about the changes

                        } else {
                            Toast.makeText(this, "Failed to set suspension end date: ${subTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Failed to suspend driver: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to fetch online status: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

}
