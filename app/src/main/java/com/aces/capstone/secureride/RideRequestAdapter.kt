package com.aces.capstone.secureride

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.model.RideRequest
import com.google.firebase.database.FirebaseDatabase

class RideRequestAdapter : RecyclerView.Adapter<RideRequestAdapter.RideRequestViewHolder>() {
    private val rideRequests = mutableListOf<RideRequest>()

    fun addRideRequest(rideRequest: RideRequest) {
        rideRequests.add(rideRequest)
        notifyItemInserted(rideRequests.size - 1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideRequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ride_request_item, parent, false)
        return RideRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideRequestViewHolder, position: Int) {
        val rideRequest = rideRequests[position]
        holder.bind(rideRequest)
    }

    override fun getItemCount() = rideRequests.size

    class RideRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val firstNameTextView: TextView = itemView.findViewById(R.id.firstNameTextView)
        private val lastNameTextView: TextView = itemView.findViewById(R.id.lastNameTextView)
        private val destinationTextView: TextView = itemView.findViewById(R.id.destinationTextView)
        private val acceptButton: Button = itemView.findViewById(R.id.acceptButton)

        fun bind(rideRequest: RideRequest) {
            firstNameTextView.text = rideRequest.firstName
            lastNameTextView.text = rideRequest.lastName
            destinationTextView.text = rideRequest.destination

            // Show "Accept" button only if the status is "pending"
            if (rideRequest.status == "pending") {
                acceptButton.visibility = View.VISIBLE
            } else {
                acceptButton.visibility = View.GONE
            }

            // Handle the "Accept" button click
            acceptButton.setOnClickListener {
                acceptRideRequest(rideRequest)
            }
        }

        private fun acceptRideRequest(rideRequest: RideRequest) {
            // Update the ride request status in Firebase to "accepted"
            val rideRequestId = rideRequest.id
            if (rideRequestId != null) {
                val firebaseDatabaseReference = FirebaseDatabase.getInstance().reference
                firebaseDatabaseReference.child("ride_requests").child(rideRequestId)
                    .child("status").setValue("accepted")
            }
        }
    }
}
