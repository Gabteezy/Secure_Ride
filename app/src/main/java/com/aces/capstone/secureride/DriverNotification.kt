package com.aces.capstone.secureride

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.databinding.ActivityDriverNotificationBinding
import com.aces.capstone.secureride.model.BookingRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore

class DriverNotification : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "DriverNotification"
    private lateinit var binding: ActivityDriverNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Current user UID: ${currentUser.uid}")
            listenForBookingRequests(currentUser.uid)
        } else {
            Log.w(TAG, "No authenticated user found")
        }
    }

    private fun listenForBookingRequests(driverId: String) {
        db.collection("bookingRequests")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    Log.d(TAG, "Booking requests snapshot received")
                    for (dc in snapshots.documentChanges) {
                        when (dc.type) {
                            DocumentChange.Type.ADDED -> {
                                val bookingRequest = dc.document.toObject(BookingRequest::class.java)
                                Log.d(TAG, "New booking request: $bookingRequest")
                                displayNotification(bookingRequest)
                            }
                            DocumentChange.Type.MODIFIED -> {
                                Log.d(TAG, "Modified booking request")
                            }
                            DocumentChange.Type.REMOVED -> {
                                Log.d(TAG, "Removed booking request")
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "No new booking requests")
                }
            }
    }

    private fun displayNotification(bookingRequest: BookingRequest) {
        binding.notification.text = " Commuter: ${bookingRequest.commuterId}\n" +
                    " FirstName: ${bookingRequest.firstname}\n" +
                    " Pickup: ${bookingRequest.pickupLocation}\n" +
                    " Dropoff: ${bookingRequest.dropoffLocation}\n" +
                    " Time: ${bookingRequest.timeSlot}\n" +
                    " Date: ${bookingRequest.date}"
    }
}