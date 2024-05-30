package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aces.capstone.secureride.model.Booking
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DriverNotification : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var notificationTextView: TextView
    private lateinit var btnAccept: Button
    private lateinit var btnDecline: Button

    private var currentBookingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_notification)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        notificationTextView = findViewById(R.id.notification)
        btnAccept = findViewById(R.id.btnAccept)
        btnDecline = findViewById(R.id.btnDecline)

        listenForBookings()

        btnAccept.setOnClickListener {
            currentBookingId?.let { bookingId ->
                acceptBooking(bookingId)
            }
        }

        btnDecline.setOnClickListener {
            Toast.makeText(this, "Booking Declined", Toast.LENGTH_SHORT).show()
            currentBookingId = null
            notificationTextView.text = "No booking requests."
        }
    }

    private fun listenForBookings() {
        val bookingsRef = database.reference.child("bookings")

        bookingsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleBooking(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleBooking(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriverNotification, "Failed to listen for bookings", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleBooking(snapshot: DataSnapshot) {
        val booking = snapshot.getValue(Booking::class.java)
        if (booking != null && booking.driverId == null) {
            currentBookingId = snapshot.key
            notificationTextView.text = "New booking request:\nPickup: ${booking.details?.get("pickup")}\nDestination: ${booking.details?.get("destination")}\nTime: ${booking.details?.get("time")}\nDate: ${booking.details?.get("date")}"
            Toast.makeText(this, "New booking request available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun acceptBooking(bookingId: String) {
        val driverId = auth.currentUser?.uid

        if (driverId != null) {
            database.reference.child("bookings").child(bookingId).child("driverId").setValue(driverId).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Booking Accepted", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@DriverNotification, MapsActivity::class.java))
                } else {
                    Toast.makeText(this, "Failed to accept booking", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No authenticated user found", Toast.LENGTH_SHORT).show()
        }
    }
}
