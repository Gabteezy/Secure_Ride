package com.aces.capstone.secureride

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.CalendarView
import android.widget.Toast
import com.aces.capstone.secureride.databinding.ActivityBookingDetailsBinding
import com.aces.capstone.secureride.databinding.ActivitySearchBinding

class BookingDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBookingDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailsBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_booking_details)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val timeSlotButton = findViewById<Button>(R.id.timeSlot)
        val timeSlotButton1 = findViewById<Button>(R.id.timeSlot1)
        val timeSlotButton2 = findViewById<Button>(R.id.timeSlot2)
        val timeSlotButton3 = findViewById<Button>(R.id.timeSlot3)

// Set a date change listener for the CalendarView
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
        }

        timeSlotButton.setOnClickListener {
            Toast.makeText(this, "1:00pm selected", Toast.LENGTH_SHORT).show()
        }

        timeSlotButton1.setOnClickListener {
            Toast.makeText(this, "2:00pm selected", Toast.LENGTH_SHORT).show()
        }

        timeSlotButton2.setOnClickListener {
            Toast.makeText(this, "3:00pm selected", Toast.LENGTH_SHORT).show()
        }

        timeSlotButton3.setOnClickListener {
            Toast.makeText(this, "4:00pm selected", Toast.LENGTH_SHORT).show()
        }

        binding.confirm.setOnClickListener {
            startActivity(Intent(this@BookingDetails, Map::class.java))
        }


    }
}