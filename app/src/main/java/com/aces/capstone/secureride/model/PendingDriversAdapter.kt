package com.aces.capstone.secureride.model

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData
import com.aces.capstone.secureride.ui.PendingDriverProfileActivity
import com.google.firebase.database.FirebaseDatabase

class PendingDriversAdapter(
    private val driverList: List<UserData>,
    private val onCheckClick: (UserData) -> Unit,
    private val onDeleteClick: (UserData) -> Unit
) : RecyclerView.Adapter<PendingDriversAdapter.DriverViewHolder>() {

    inner class DriverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val driverName: TextView = itemView.findViewById(R.id.driverName)
        val checkButton: ImageView = itemView.findViewById(R.id.checkButton)
        val tryAgainButton: ImageView = itemView.findViewById(R.id.tryAgainButton)

        fun bind(driver: UserData) {
            driverName.text = "${driver.firstname} ${driver.lastname}"

            // Set click listeners for check and delete buttons
            checkButton.setOnClickListener { onCheckClick(driver) }
            tryAgainButton.setOnClickListener {
                onDeleteClick(driver)
                showAdminNoteToast(driver.uid!!, itemView.context) // Pass the correct context here
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pending_drivers, parent, false)
        return DriverViewHolder(view)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) {
        val driver = driverList[position]
        holder.bind(driver)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PendingDriverProfileActivity::class.java)
            intent.putExtra("user_id", driver.uid) // Pass only the user ID
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = driverList.size

    private fun showAdminNoteToast(driverId: String, context: android.content.Context) {
        val adminNoteRef = FirebaseDatabase.getInstance().getReference("pending_drivers").child(driverId).child("note")

        adminNoteRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val adminNote = task.result?.getValue(String::class.java)
                val message = adminNote ?: "No admin note available."
            } else {
                Toast.makeText(context, "Failed to fetch admin note: ${task.exception?.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
