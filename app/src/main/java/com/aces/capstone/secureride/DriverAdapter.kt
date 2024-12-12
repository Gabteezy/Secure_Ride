package com.aces.capstone.secureride.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData

class DriverAdapter(
    private val driverList: List<UserData>,
    private val onEditClick: (UserData) -> Unit,
    private val onDeleteClick: (UserData) -> Unit
) : RecyclerView.Adapter<DriverAdapter.DriverViewHolder>() {

    class DriverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val driverIdTextView: TextView = itemView.findViewById(R.id.driverIdTextView) // Reference the correct ID
        val driverNameTextView: TextView = itemView.findViewById(R.id.driverNameTextView) // Reference the correct ID
        val editIcon: ImageView = itemView.findViewById(R.id.editIcon)
        val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_driver, parent, false)
        return DriverViewHolder(view)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
        val driver = driverList[position]

        // Display commuter's UID
        holder.driverIdTextView.text = driver.uid ?: "N/A"

        // Display commuter's full name
        val fullName = "${driver.firstname ?: "No First Name"} ${driver.lastname ?: "No Last Name"}"
        holder.driverNameTextView.text = fullName

        // Set up click listeners
        holder.editIcon.setOnClickListener {
            onEditClick(driver)
        }

        holder.deleteIcon.setOnClickListener {
            onDeleteClick(driver)
        }
    }

    override fun getItemCount(): Int = driverList.size
}