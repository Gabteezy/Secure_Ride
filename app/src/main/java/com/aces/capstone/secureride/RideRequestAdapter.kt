package com.aces.capstone.secureride.adapter

import android.app.AlertDialog
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.model.RideRequest
import com.google.firebase.database.FirebaseDatabase

class RideRequestAdapter(
    private val context: Context,
    private val rideRequests: MutableList<RideRequest>, // Make it mutable
    private val onAcceptClicked: (RideRequest) -> Unit,
    private val onDeclineClicked: (RideRequest) -> Unit,
    private val onReferClicked: (RideRequest) -> Unit
) : RecyclerView.Adapter<RideRequestAdapter.ViewHolder>() {

    // Declare a map to hold the timers
    private val rideRequestTimers = HashMap<String, CountDownTimer>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timerTextView: TextView = itemView.findViewById(R.id.timerTextView)
        val nameTextView: TextView = itemView.findViewById(R.id.contactNameTextView)
        val pickupLocationTextView: TextView = itemView.findViewById(R.id.pickupLocationTextView)
        val dropoffLocationTextView: TextView = itemView.findViewById(R.id.dropoffLocationTextView)
        val totalFareTextView: TextView = itemView.findViewById(R.id.totalFareTextView)
        val rideInfoTextView: TextView = itemView.findViewById(R.id.rideInfoTextView)
        val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        val declineButton: Button = itemView.findViewById(R.id.declineButton)
        val referButton: Button = itemView.findViewById(R.id.referButton)

        fun bind(rideRequest: RideRequest) {
            nameTextView.text = "Name: ${rideRequest.firstName ?: "N/A"} ${rideRequest.lastName ?: "N/A"}"
            pickupLocationTextView.text = "${rideRequest.pickupLocation ?: "Unknown"}"
            dropoffLocationTextView.text = "${rideRequest.dropoffLocation ?: "Unknown"}"
            rideInfoTextView.text = rideRequest.rideInfo ?: "No ride info available"
            totalFareTextView.text = "PHP ${rideRequest.totalFare ?: 0.0}"

            if (rideRequest.status == "pending") {
                startTimer(rideRequest)
            } else if (rideRequest.status == "accepted") {
                timerTextView.text = "Ride Accepted"
                acceptButton.isEnabled = true
                declineButton.isEnabled = true
                referButton.isEnabled = true
                cancelRideRequestTimer(rideRequest.id ?: "") // Cancel the timer when status is "accepted"
            } else if (rideRequest.status == "expired") {
                timerTextView.text = "Time expired!"
                acceptButton.isEnabled = true
                declineButton.isEnabled = true
                referButton.isEnabled = true
            }

            acceptButton.setOnClickListener {
                onAcceptClicked(rideRequest)
                cancelRideRequestTimer(rideRequest.id ?: "") // Cancel the timer when accepting
                rideRequest.status = "accepted"
                notifyItemChanged(adapterPosition) // Notify the adapter that the item has changed
            }

            declineButton.setOnClickListener {
                onDeclineClicked(rideRequest)
                cancelRideRequestTimer(rideRequest.id ?: "") // Cancel the timer when declining
                rideRequest.status = "declined"
                notifyItemChanged(adapterPosition) // Notify the adapter that the item has changed
            }

            referButton.setOnClickListener {
                onReferClicked(rideRequest)
            }
        }

        private fun startTimer(rideRequest: RideRequest) {
            Log.d("RideRequestAdapter", "Starting timer for ride request: ${rideRequest.id}")
            val timer = object : CountDownTimer(rideRequest.timeRemaining, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    rideRequest.timeRemaining = millisUntilFinished
                    timerTextView.text = "Ends in ${millisUntilFinished / 1000} sec"
                }

                override fun onFinish() {
                    timerTextView.text = "Time expired!"
                    acceptButton.isEnabled = true
                    declineButton.isEnabled = true
                    referButton.isEnabled = true // Disable refer button when time expires
                    rideRequest.status = "expired"
                    updateRideRequestStatus(rideRequest)
                    notifyItemChanged(adapterPosition) // Notify the adapter that the item has changed
                }
            }
            timer.start()
            rideRequestTimers[rideRequest.id ?: ""] = timer // Store the timer
        }

        private fun cancelRideRequestTimer(rideRequestId: String) {
            Log.d("RideRequestAdapter", "Cancelling timer for ride request: $rideRequestId")
            rideRequestTimers[rideRequestId]?.cancel() // Cancel the timer
            rideRequestTimers.remove(rideRequestId) // Remove the timer from the map
        }

        private fun updateRideRequestStatus(rideRequest: RideRequest) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("ride_requests")
            databaseReference.child(rideRequest.id ?: "").child("status").setValue("expired")
        }

        private fun showReferDialog(rideRequest: RideRequest) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Refer Driver")
            builder.setMessage("There are no available drivers for this ride request for now.")

            builder.setPositiveButton("OK") { dialog, _ ->
                onReferClicked(rideRequest) // Call the refer callback
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.ride_request_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rideRequest = rideRequests[position]
        holder.bind(rideRequest)
    }

    override fun getItemCount(): Int = rideRequests.size

    // Add a method to remove a ride request from the list
    fun removeRideRequest(rideRequestId: String) {
        val index = rideRequests.indexOfFirst { it.id == rideRequestId }
        if (index != -1) {
            rideRequests.removeAt(index)
            notifyItemRemoved(index) // Notify the adapter that the item has been removed
        }
    }
}