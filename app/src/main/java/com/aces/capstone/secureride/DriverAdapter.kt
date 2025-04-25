package com.aces.capstone.secureride.adapter

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData
import com.aces.capstone.secureride.ui.DriverProfileActivity
import java.util.concurrent.TimeUnit

class DriverAdapter(
    private val driverList: List<UserData>,
    private val onDeleteClick: (UserData) -> Unit
) : RecyclerView.Adapter<DriverAdapter.DriverViewHolder>() {

    class DriverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val driverIdTextView: TextView = itemView.findViewById(R.id.driverIdTextView)
        val driverNameTextView: TextView = itemView.findViewById(R.id.driverNameTextView)
        val suspendIcon: ImageView = itemView.findViewById(R.id.suspendIcon)
        val suspendedLabel: TextView = itemView.findViewById(R.id.suspendedLabel) // Reference the suspended label
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_driver, parent, false)
        return DriverViewHolder(view)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
        val driver = driverList[position]

        // Display driver's UID
        holder.driverIdTextView.text = driver.uid ?: "N/A"

        // Display driver's full name
        val fullName = "${driver.firstname ?: "No First Name"} ${driver.lastname ?: "No Last Name"}"
        holder.driverNameTextView.text = fullName

        // Check if the driver is suspended
        if (driver.isSuspended) {
            holder.itemView.setBackgroundColor(Color.RED) // Change background color to red for suspended drivers
            holder.suspendedLabel.visibility = View.VISIBLE // Show a label indicating suspension
            val suspensionEndDate = driver.suspensionEndDate
            if (suspensionEndDate != null) {
                val currentTime = System.currentTimeMillis()
                if (currentTime < suspensionEndDate) {
                    val remainingDays = TimeUnit.MILLISECONDS.toDays(suspensionEndDate - currentTime)
                    holder.driverNameTextView.text = "$fullName (Suspended: $remainingDays days remaining)"
                }
            }
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE) // Default background color
            holder.suspendedLabel.visibility = View.GONE // Hide the label
        }

        holder.suspendIcon.setOnClickListener {
            onDeleteClick(driver)
        }
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DriverProfileActivity::class.java)
            intent.putExtra("user_id", driver.uid) // Pass only the user ID
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = driverList.size
}