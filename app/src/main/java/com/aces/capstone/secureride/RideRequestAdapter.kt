package com.aces.capstone.secureride.adapter

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
        val timerTextView: TextView = itemView.findViewById(R.id.timerTextView)
        val nameTextView: TextView = itemView.findViewById(R.id.contactNameTextView)
        val pickupLocationTextView: TextView = itemView.findViewById(R.id.pickupLocationTextView)
        val dropoffLocationTextView: TextView = itemView.findViewById(R.id.dropoffLocationTextView)
        val totalFareTextView: TextView = itemView.findViewById(R.id.totalFareTextView)
        val rideInfoTextView: TextView = itemView.findViewById(R.id.rideInfoTextView)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val declineButton: Button = itemView.findViewById(R.id.declineButton)


        fun bind(rideRequest: RideRequest) {
            nameTextView.text = "Name: ${rideRequest.firstName ?: "N/A"} ${rideRequest.lastName ?: "N/A"}"
            pickupLocationTextView.text = "Pickup Location: ${rideRequest.pickupLocation ?: "Unknown"}"
            dropoffLocationTextView.text = "Dropoff Location: ${rideRequest.dropoffLocation ?: "Unknown"}"
            rideInfoTextView.text = rideRequest.rideInfo ?: "No ride info available"
            totalFareTextView.text = "PHP ${rideRequest.totalFare ?: 0.0}" // Use default fare if null

            object : CountDownTimer(300000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsLeft = millisUntilFinished / 1000
                    timerTextView.text = "Ends in $secondsLeft sec"
                }

                override fun onFinish() {
                    timerTextView.text = "Time expired!"
                    acceptButton.isEnabled = false
                    declineButton.isEnabled = false
                }
            }.start()

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
        holder.bind(rideRequest) // Bind the ride request data to the views
    }

    override fun getItemCount(): Int = rideRequests.size
}
