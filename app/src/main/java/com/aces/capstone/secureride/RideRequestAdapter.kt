package com.aces.capstone.secureride.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.model.RideRequest

class RideRequestAdapter(
    private val rideRequests: List<RideRequest>,
    private val onAcceptClicked: (RideRequest) -> Unit,
    private val onDeclineClicked: (RideRequest) -> Unit // Added decline callback
) : RecyclerView.Adapter<RideRequestAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Initialize your TextViews
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        val acceptButton: TextView = itemView.findViewById(R.id.acceptButton)
        val declineButton: TextView = itemView.findViewById(R.id.declineButton)

        fun bind(rideRequest: RideRequest) {
            // Bind the data to your views
            nameTextView.text = "${rideRequest.firstName} ${rideRequest.lastName}"
            locationTextView.text = "Location: ${rideRequest.latitude}, ${rideRequest.longitude}"

            // Example condition: Change text color if name is "John Doe"
            if (nameTextView.text == "John Doe") {
                nameTextView.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
            }

            acceptButton.setOnClickListener {
                onAcceptClicked(rideRequest)
            }

            declineButton.setOnClickListener {
                onDeclineClicked(rideRequest)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ride_request_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rideRequest = rideRequests[position]
        holder.bind(rideRequest) // This sets the name and location text
    }

    override fun getItemCount(): Int = rideRequests.size
}