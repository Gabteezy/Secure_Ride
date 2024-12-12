package com.aces.capstone.secureride.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.model.RideHistory

class RideHistoryAdapter(private val historyList: List<RideHistory>) : RecyclerView.Adapter<RideHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ride_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rideHistory = historyList[position]
        holder.bind(rideHistory)
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val commuterNameTextView: TextView = itemView.findViewById(R.id.commuterNameTextView)
        private val startLocationTextView: TextView = itemView.findViewById(R.id.startLocationTextView)
        private val endLocationTextView: TextView = itemView.findViewById(R.id.endLocationTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        val rideInfoTextView: TextView = itemView.findViewById(R.id.rideInfoTextView)
        val totalFareTextView: TextView = itemView.findViewById(R.id.totalFareTextView)

        fun bind(rideHistory: RideHistory) {
            commuterNameTextView.text = rideHistory.commuterName
            startLocationTextView.text = rideHistory.pickuplocation
            endLocationTextView.text = rideHistory.dropofflocation
            statusTextView.text = rideHistory.status
            rideInfoTextView.text = rideHistory.rideInfo ?: ""
            totalFareTextView.text = "₱${rideHistory.totalFare ?: 0.0}"

        }
    }
}