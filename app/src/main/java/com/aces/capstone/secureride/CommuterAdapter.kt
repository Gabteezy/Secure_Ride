package com.aces.capstone.secureride.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData
import com.aces.capstone.secureride.ui.CommuterProfileActivity

class CommuterAdapter(
    private val commuterList: List<UserData>,
    private val onEditClick: (UserData) -> Unit,
    private val onDeleteClick: (UserData) -> Unit
) : RecyclerView.Adapter<CommuterAdapter.CommuterViewHolder>() {

    class CommuterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commuterIdTextView: TextView = itemView.findViewById(R.id.commuterIdTextView)
        val commuterNameTextView: TextView = itemView.findViewById(R.id.commuterNameTextView)
        val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommuterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_commuter, parent, false)
        return CommuterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommuterViewHolder, position: Int) {
        val commuter = commuterList[position]

        // Display commuter's UID
        holder.commuterIdTextView.text = commuter.uid ?: "N/A"

        // Display commuter's full name
        val fullName = "${commuter.firstname ?: "No First Name"} ${commuter.lastname ?: "No Last Name"}"
        holder.commuterNameTextView.text = fullName



        holder.deleteIcon.setOnClickListener {
            onDeleteClick(commuter)
        }

        // Set up click listener for the commuter item to view profile
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, CommuterProfileActivity::class.java)
            intent.putExtra("commuter_data", commuter) // Pass commuter data
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = commuterList.size
}