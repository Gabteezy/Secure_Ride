package com.aces.capstone.secureride.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.aces.capstone.secureride.databinding.ActivityListOfCommutersBinding
import com.aces.capstone.secureride.UserData
import com.aces.capstone.secureride.adapter.CommuterAdapter
import com.google.firebase.database.*

class ListOfCommuterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListOfCommutersBinding
    private lateinit var commuterAdapter: CommuterAdapter
    private val commuterList = mutableListOf<UserData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListOfCommutersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the RecyclerView with the CommuterAdapter
        commuterAdapter = CommuterAdapter(commuterList, { commuter ->
            // Handle edit action
            editCommuter(commuter)
        }, { commuter ->
            // Handle delete action
            deleteCommuter(commuter)
        })

        // Set up RecyclerView
        binding.recyclerViewDrivers.apply {
            layoutManager = LinearLayoutManager(this@ListOfCommuterActivity)
            adapter = commuterAdapter
        }

        // Fetch commuters from Firebase
        fetchCommuters()

        // Back button action
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchCommuters() {
        val commutersRef = FirebaseDatabase.getInstance().getReference("user").orderByChild("type").equalTo("Commuter")

        commutersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commuterList.clear()
                for (commuterSnapshot in snapshot.children) {
                    val commuter = commuterSnapshot.getValue(UserData::class.java)
                    if (commuter != null) {
                        commuterList.add(commuter)
                    } else {
                        Log.e("ListOfCommuterActivity", "Null commuter data found in snapshot.")
                    }
                }
                Log.d("ListOfCommuterActivity", "Total commuters: ${commuterList.size}")
                commuterAdapter.notifyDataSetChanged()
            }



            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListOfCommuterActivity, "Failed to load commuters: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun editCommuter(commuter: UserData) {
        // Implement edit functionality
        Toast.makeText(this, "Edit commuter: ${commuter.firstname}", Toast.LENGTH_SHORT).show()
    }

    private fun deleteCommuter(commuter: UserData) {
        // Implement delete functionality
        Toast.makeText(this, "Delete commuter: ${commuter.firstname}", Toast.LENGTH_SHORT).show()
    }
}