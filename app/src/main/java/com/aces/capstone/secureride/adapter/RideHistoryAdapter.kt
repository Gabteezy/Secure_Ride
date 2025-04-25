package com.aces.capstone.secureride.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.model.RideHistory
import java.text.SimpleDateFormat
import java.util.*

class RideHistoryAdapter(private val rideHistoryList: List<RideHistory>) : RecyclerView.Adapter<RideHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rideIdTextView: TextView = itemView.findViewById(R.id.rideIdTextView)
        val commuterNameTextView: TextView = itemView.findViewById(R.id.commuterNameTextView)
        val pickupLocationTextView: TextView = itemView.findViewById(R.id.pickupLocationTextView)
        val dropoffLocationTextView: TextView = itemView.findViewById(R.id.dropoffLocationTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val totalFareTextView: TextView = itemView.findViewById(R.id.totalFareTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ride_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rideHistory = rideHistoryList[position]

        // Bind static fields
        holder.rideIdTextView.text = "Ride ID: ${rideHistory.rideId ?: "N/A"}"
        holder.commuterNameTextView.text = "Name: ${rideHistory.commuterName ?: "N/A"}"
        holder.pickupLocationTextView.text = rideHistory.pickupLocation ?: "N/A"
        holder.dropoffLocationTextView.text = rideHistory.dropoffLocation ?: "N/A"
        holder.statusTextView.text = rideHistory.status ?: "N/A"
        holder.totalFareTextView.text = "₱${rideHistory.totalFare?.toString() ?: "0"}"

        // Handle time and date formatting
        if (rideHistory.timestamp != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Date format
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()) // Time format

            val date = Date(rideHistory.timestamp) // Convert timestamp to Date
            holder.dateTextView.text = "Date: ${dateFormat.format(date)}"
            holder.timeTextView.text = "Time: ${timeFormat.format(date)}"
        } else {
            holder.dateTextView.text = "Date: N/A"
            holder.timeTextView.text = "Time: N/A"
        }

        // Log for debugging purposes
        Log.d("RideHistoryAdapter", "Time: ${rideHistory.timestamp}, Date: ${rideHistory.timestamp}")

        // Handle status color
        if (rideHistory.status.equals("Declined", ignoreCase = true)) {
            holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.red))  // Assuming you have a red color defined in your colors.xml
        } else {
            holder.statusTextView.setTextColor(holder.itemView.context.getColor(R.color.black))  // Default color
        }
    }

    override fun getItemCount(): Int {
        return rideHistoryList.size
    }
}


