package com.aces.capstone.secureride.ui

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.UserData
import com.aces.capstone.secureride.databinding.ActivityListOfPendingDriversBinding
import com.aces.capstone.secureride.model.PendingDriversAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ListOfPendingDrivers : AppCompatActivity() {

    private lateinit var binding: ActivityListOfPendingDriversBinding
    private lateinit var pendingDriversAdapter: PendingDriversAdapter
    private val driverList = mutableListOf<UserData>()
    private lateinit var firebaseDatabaseReference: DatabaseReference
    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListOfPendingDriversBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database reference
        firebaseDatabaseReference = FirebaseDatabase.getInstance().getReference("pending_drivers")

        // Initialize the RecyclerView with the PendingDriversAdapter
        pendingDriversAdapter = PendingDriversAdapter(driverList, { driver ->
            // Handle approve action
            approveDriver(driver.uid)
        }, { driver ->
            // Handle try again action
            tryAgainDriver(driver.uid)
        })

        // Set up RecyclerView
        binding.recyclerViewPendingDrivers.apply {
            layoutManager = LinearLayoutManager(this@ListOfPendingDrivers)
            adapter = pendingDriversAdapter
        }

        // Fetch drivers from Firebase
        fetchDrivers()

        // Back button action
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchDrivers() {
        // Fetch drivers who are not verified
        val driversRef = firebaseDatabaseReference

        driversRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                driverList.clear()
                for (driverSnapshot in snapshot.children) {
                    val driver = driverSnapshot.getValue(UserData::class.java)
                    if (driver != null) {
                        driver.uid = driverSnapshot.key ?: ""
                        driverList.add(driver)
                    }
                }
                pendingDriversAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListOfPendingDrivers, "Failed to load drivers: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun approveDriver(driverId: String?) {
        val nonNullDriverId = driverId ?: return
        Log.d("ListOfPendingDrivers", "Approving driver with ID: $nonNullDriverId")

        val pendingDriverRef = firebaseDatabaseReference.child(nonNullDriverId)
        val approvedDriverRef = FirebaseDatabase.getInstance().getReference("user").child(nonNullDriverId)

        pendingDriverRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val driverData = snapshot.getValue(UserData::class.java)
                    if (driverData != null) {
                        driverData.isVerified = true

                        // Ensure email is preserved
                        val email = driverData.email ?: return@onDataChange
                        driverData.authEmail = email // Store in a separate field if needed

                        // Move driver data to 'user' node
                        approvedDriverRef.setValue(driverData).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                pendingDriverRef.removeValue().addOnCompleteListener { removeTask ->
                                    if (removeTask.isSuccessful) {
                                        Log.d("ListOfPendingDrivers", "Driver approved and moved to 'users' node.")
                                        Toast.makeText(this@ListOfPendingDrivers, "Driver approved successfully.", Toast.LENGTH_SHORT).show()
                                        fetchDrivers()
                                    } else {
                                        Log.e("ListOfPendingDrivers", "Failed to remove driver from pending list: ${removeTask.exception?.message}")
                                        Toast.makeText(this@ListOfPendingDrivers, "Failed to remove driver from pending list.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Log.e("ListOfPendingDrivers", "Failed to approve driver: ${task.exception?.message}")
                                Toast.makeText(this@ListOfPendingDrivers, "Failed to approve driver.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.e("ListOfPendingDrivers", "Driver data does not exist in pending list for UID: $nonNullDriverId")
                    Toast.makeText(this@ListOfPendingDrivers, "Driver data does not exist.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ListOfPendingDrivers", "Error fetching pending driver data: ${error.message}")
            }
        })
    }


    private fun tryAgainDriver(driverId: String?) {
        val nonNullDriverId = driverId ?: return
        val driverRef = firebaseDatabaseReference.child(nonNullDriverId)

        // Show a dialog to allow the admin to enter a note
        val input = EditText(this)
        input.hint = "Enter reason for rejection or instructions"

        AlertDialog.Builder(this)
            .setTitle("Add Admin Note")
            .setView(input)
            .setPositiveButton("Submit") { dialog, _ ->
                val adminNote = input.text.toString()
                if (adminNote.isNotEmpty()) {
                    // Save the admin note to the driver's record
                    driverRef.child("note").setValue(adminNote).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Remove the driver from pending drivers
                            driverRef.removeValue().addOnCompleteListener { removeTask ->
                                if (removeTask.isSuccessful) {
                                    // Delete the Firebase Authentication account
                                    firebaseAuth.currentUser?.delete()?.addOnCompleteListener { deleteTask ->
                                        if (deleteTask.isSuccessful) {
                                            Toast.makeText(this, "Driver registration canceled successfully.", Toast.LENGTH_SHORT).show()
                                            fetchDrivers()
                                        } else {
                                            Toast.makeText(this, "Failed to delete Firebase account: ${deleteTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this, "Failed to cancel registration: ${removeTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Log.e("Admin", "Error saving admin note: ${task.exception?.message}")
                        }
                    }
                } else {
                    Toast.makeText(this, "Please enter a note.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}