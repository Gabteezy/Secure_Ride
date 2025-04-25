package com.aces.capstone.secureride.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.model.RideRequest

class AcceptedRidesAdapter(
    private val context: Context,
    private val acceptedRides: List<RideRequest>,
    private val onCompleteRideClicked: (RideRequest) -> Unit // Callback for completing the ride
) : RecyclerView.Adapter<AcceptedRidesAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commuterNameTextView: TextView = itemView.findViewById(R.id.contactNameTextView)
        val pickupLocationTextView: TextView = itemView.findViewById(R.id.pickupLocationTextView)
        val dropoffLocationTextView: TextView = itemView.findViewById(R.id.dropoffLocationTextView)
        val rideInfoTextView: TextView = itemView.findViewById(R.id.rideInfoTextView)
        val totalFareTextView: TextView = itemView.findViewById(R.id.totalFareTextView)
        val completeRideButton: Button = itemView.findViewById(R.id.completeRideButton)

        fun bind(rideRequest: RideRequest) {
            commuterNameTextView.text = "Name: ${rideRequest.firstName} ${rideRequest.lastName}"
            pickupLocationTextView.text = "Pickup: ${rideRequest.pickupLocation}"
            dropoffLocationTextView.text = "Dropoff: ${rideRequest.dropoffLocation}"
            rideInfoTextView.text = rideRequest.rideInfo ?: "No ride info available"
            totalFareTextView.text = "Fare: PHP ${rideRequest.totalFare}"

            completeRideButton.setOnClickListener {
                onCompleteRideClicked(rideRequest) // Trigger the callback to complete the ride
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.accepted_ride_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rideRequest = acceptedRides[position]
        holder.bind(rideRequest)
    }

    override fun getItemCount(): Int = acceptedRides.size
}