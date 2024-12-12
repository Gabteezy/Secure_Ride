package com.aces.capstone.secureride.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.model.RideRequest

class RideAdapter(
    private val rideList: List<RideRequest>,
    private val onEditClick: (RideRequest) -> Unit,
    private val onDeleteClick: (RideRequest) -> Unit
) : RecyclerView.Adapter<RideAdapter.RideViewHolder>() {

    class RideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rideIDTextView: TextView = itemView.findViewById(R.id.rideIDTextView)
        val editIcon: ImageView = itemView.findViewById(R.id.editIcon)
        val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ride, parent, false)
        return RideViewHolder(view)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {
        val ride = rideList[position]

        // Display Ride ID
        holder.rideIDTextView.text = ride.id ?: "No ID"

        // Set up click listeners
        holder.editIcon.setOnClickListener {
            onEditClick(ride)
        }

        holder.deleteIcon.setOnClickListener {
            onDeleteClick(ride)
        }
    }

    override fun getItemCount(): Int = rideList.size
}