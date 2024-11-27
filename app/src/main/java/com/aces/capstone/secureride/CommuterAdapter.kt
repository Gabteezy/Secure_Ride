package com.aces.capstone.secureride.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aces.capstone.secureride.R
import com.aces.capstone.secureride.UserData

class CommuterAdapter(
    private val commuterList: List<UserData>,
    private val onEditClick: (UserData) -> Unit,
    private val onDeleteClick: (UserData) -> Unit
) : RecyclerView.Adapter<CommuterAdapter.CommuterViewHolder>() {

    class CommuterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commuterIdTextView: TextView = itemView.findViewById(R.id.commuterIdTextView)
        val commuterNameTextView: TextView = itemView.findViewById(R.id.commuterNameTextView)
        val editIcon: ImageView = itemView.findViewById(R.id.editIcon)
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

        // Set up click listeners
        holder.editIcon.setOnClickListener {
            onEditClick(commuter)
        }

        holder.deleteIcon.setOnClickListener {
            onDeleteClick(commuter)
        }
    }

    override fun getItemCount(): Int = commuterList.size
}
